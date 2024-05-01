package com.ownd_project.tw2023_wallet_android.oid

import arrow.core.Either
import com.auth0.jwt.exceptions.JWTVerificationException
import com.ownd_project.tw2023_wallet_android.signature.ECPublicJwk
import com.ownd_project.tw2023_wallet_android.signature.ES256K.createJws
import com.ownd_project.tw2023_wallet_android.signature.JWT
import com.ownd_project.tw2023_wallet_android.signature.SignatureUtil.toJwkThumbprint
import com.ownd_project.tw2023_wallet_android.utils.EnumDeserializer
import com.ownd_project.tw2023_wallet_android.utils.SDJwtUtil
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ownd_project.tw2023_wallet_android.signature.toBase64Url
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.bouncycastle.jce.spec.ECNamedCurveSpec
import java.math.BigInteger
import java.net.URI
import java.security.KeyPair
import java.security.PublicKey
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.util.Base64
import java.util.UUID

data class ProviderOption(
    val expiresIn: Int = 600,
    val signingAlgo: String = "ES256K",
    val signingCurve: String = "P-256",
)

class OpenIdProvider(val uri: String, val option: ProviderOption = ProviderOption()) {
    private lateinit var keyPair: KeyPair
    private lateinit var keyBinding: KeyBinding
    private lateinit var siopRequest: ProcessSIOPRequestResult

    companion object {
        fun selectDisclosure(
            sdJwt: String,
            presentationDefinition: PresentationDefinition
        ): Pair<InputDescriptor, List<SDJwtUtil.Disclosure>>? {
            val parts = sdJwt.split('~')
            val newList = if (parts.size > 2) parts.drop(1).dropLast(1) else emptyList<String>()
            val disclosures = SDJwtUtil.decodeDisclosure(newList)
            // JsonPathによるフィルタのために一度JSONにシリアライズする
            val sourcePayload =
                disclosures.associate { it.key to it.value }
            /*
                example of source payload
                {
                  "claim1": "foo",
                  "claim2": "bar"
                }
             */

            presentationDefinition.inputDescriptors.forEach { inputDescriptor ->
                val matchingDisclosures = inputDescriptor.constraints.fields?.flatMap { field ->
                    /*
                    array of string values filtered by `inputDescriptor.constraints.fields.path`

                    example of input_descriptors
                        "input_descriptors": [
                          {
                            "constraints": {
                              "fields": [
                                {
                                  "path": ["$.claim1"], ここが配列になっている理由はformat毎に異なるpathを指定するため
                                }
                              ]
                            }
                          }
                        ]
                     */

                    // fieldのpathには`$.key`というJsonPath形式の文字列が入っているので, sourcePayloadから該当するkey, valueを取り出す
                    // todo ネストしたペイロードに対応するためにはJsonPathのライブラリを使用する
                    // todo `filter`も合致条件に加える
                    field.path.mapNotNull { jsonPath ->
                        val key = jsonPath.removePrefix("$.")
                         sourcePayload[key]
                        if (sourcePayload.containsKey(key)) key else null
                    }
                }?.let { fieldKeys ->
                    disclosures.filter { it.key in fieldKeys }
                }

                if (!matchingDisclosures.isNullOrEmpty()) {
                    return Pair(inputDescriptor, matchingDisclosures)
                }
            }
            return null
        }
    }

    fun setKeyPair(keyPair: KeyPair) {
        this.keyPair = keyPair
    }

    fun setKeyBinding(keyBinding: KeyBinding) {
        this.keyBinding = keyBinding
    }

    fun getSiopRequest(): ProcessSIOPRequestResult {
        return this.siopRequest
    }

    suspend fun processAuthorizationRequest(): Either<String, ProcessSIOPRequestResult> {
        if (uri.isBlank()) {
            throw IllegalArgumentException(SIOPErrors.BAD_PARAMS.message)
        }

        val (scheme, authorizationRequestPayload, requestObjectJwt, registrationMetadata, presentationDefinition, requestIsSigned) = parseAndResolve(
            uri
        )

        val objectMapper: ObjectMapper = jacksonObjectMapper().apply {
            propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
            configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) // ランダムなプロパティは無視する
            val module = SimpleModule().apply {
                addDeserializer(Audience::class.java, AudienceDeserializer())
                addDeserializer(ResponseMode::class.java, EnumDeserializer(ResponseMode::class))
            }
            registerModule(module)
        }

        return if (requestIsSigned) {
            val jwksUrl =
                registrationMetadata.jwksUri ?: throw IllegalStateException("JWKS URLが見つかりません。")

            val result = try {
                // JWTを検証
                val jwtValidationResult = JWT.verifyJwtWithJwks(requestObjectJwt, jwksUrl)
                val payloadJson = String(Base64.getUrlDecoder().decode(jwtValidationResult.payload))
                val payload = objectMapper.readValue(payloadJson, RequestObjectPayloadImpl::class.java)

                val clientScheme = payload.clientIdScheme?: authorizationRequestPayload.clientIdScheme
                if (clientScheme == "redirect_uri") {
                    val clientId = payload.clientId?: authorizationRequestPayload.clientId
                    val responseUri = payload.responseUri?: authorizationRequestPayload.responseUri
                    if (clientId.isNullOrBlank() || responseUri.isNullOrBlank() || clientId != responseUri) {
                        return Either.Left("Invalid client_id or response_uri")
                    }
                }

                Either.Right(
                    ProcessSIOPRequestResult(
                        scheme,
                        payload,
                        authorizationRequestPayload,
                        requestObjectJwt,
                        registrationMetadata,
                        presentationDefinition
                    )
                )
            } catch (e: JWTVerificationException) {
                Either.Left(e.message ?: "JWT検証エラー")
            }
            if (result.isRight()) {
                this.siopRequest = (result as Either.Right).value
            }
            return result
        } else {
            val decodedJwt = com.auth0.jwt.JWT.decode(requestObjectJwt)
            val payloadJson = String(Base64.getUrlDecoder().decode(decodedJwt.payload))
            val payload = objectMapper.readValue(payloadJson, RequestObjectPayloadImpl::class.java)

            val clientScheme = payload.clientIdScheme?: authorizationRequestPayload.clientIdScheme
            if (clientScheme == "redirect_uri") {
                val clientId = payload.clientId?: authorizationRequestPayload.clientId
                val responseUri = payload.responseUri?: authorizationRequestPayload.responseUri
                if (clientId.isNullOrBlank() || responseUri.isNullOrBlank() || clientId != responseUri) {
                    return Either.Left("Invalid client_id or response_uri")
                }
            }
            val siopRequest = ProcessSIOPRequestResult(
                    scheme,
                    payload,
                    authorizationRequestPayload,
                    requestObjectJwt,
                    registrationMetadata,
                    presentationDefinition
                )
            this.siopRequest = siopRequest
            Either.Right(siopRequest)
        }
    }

    suspend fun respondIdTokenResponse(): Either<String?, PostResult> {
        try {
            val authRequest = mergeOAuth2AndOpenIdInRequestPayload(
                this.siopRequest.authorizationRequestPayload,
                this.siopRequest.requestObject
            )
            val state = authRequest.state
            val nonce = authRequest.nonce
            val SEC_IN_MS = 1000

            val subJwk = generatePublicKeyJwk(keyPair, option)
            // todo: support rsa key
            val jwk = object : ECPublicJwk {
                override val kty = subJwk["kty"]!!
                override val crv = subJwk["crv"]!!
                override val x = subJwk["x"]!!
                override val y = subJwk["y"]!!
            }
            val prefix = "urn:ietf:params:oauth:jwk-thumbprint:sha-256"
            val sub = "$prefix:${toJwkThumbprint(jwk)}"
            // https://openid.github.io/SIOPv2/openid-connect-self-issued-v2-wg-draft.html#section-11.1
            // The RP MUST validate that the aud (audience) Claim contains the value of the Client ID that the RP sent in the Authorization Request as an audience.
            // When the request has been signed, the value might be an HTTPS URL, or a Decentralized Identifier.
            val idTokenPayload = IDTokenPayloadImpl(
                iss = sub,
                aud = Audience.Single(authRequest.clientId!!),
                iat = (System.currentTimeMillis() / SEC_IN_MS).toLong(),
                exp = (System.currentTimeMillis() / SEC_IN_MS + option.expiresIn).toLong(),
                sub = sub,
                nonce = nonce as? String,
                subJwk = subJwk,
            )
            val module = SimpleModule().apply {
                addSerializer(Audience::class.java, AudienceSerializer())
            }
            val mapper = jacksonObjectMapper().apply {
                propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
                registerModule(module)
            }
            val payloadJson = mapper.writeValueAsString(idTokenPayload)
            println("create id token")
            // todo support various algorithms using provider option's `signingAlgo`
            val idToken = createJws(keyPair, payloadJson, false)
            println("id token: $idToken")
            // todo support redirect response when response_mode is not `direct_post`
            val redirectUrl = requireNotNull(authRequest.redirectUri)

            println("send id token to $redirectUrl")
            val result = sendRequest(redirectUrl, mapOf("id_token" to idToken))
            println("Received result: $result")
            return Either.Right(result)
        } catch (e: Exception) {
            return Either.Left(e.message ?: "IDToken Response Error")
        }
    }

    suspend fun respondVPResponse(
        credentials: List<SubmissionCredential>,
    ): Either<String, Pair<PostResult, List<SharedContent>>> {
        try {
            val authRequest = mergeOAuth2AndOpenIdInRequestPayload(
                this.siopRequest.authorizationRequestPayload,
                this.siopRequest.requestObject
            )
            val presentationDefinition = this.siopRequest.presentationDefinition
                ?: throw IllegalArgumentException(SIOPErrors.BAD_PARAMS.message)
            // presentationDefinition.inputDescriptors を使って選択項目でフィルター
            val vpTokens = credentials.mapNotNull { it ->
                when (it.format) {
                    "vc+sd-jwt" -> {
                        Pair(
                            it.id,
                            createPresentationSubmissionSdJwtVc(
                                it,
                                authRequest,
                                presentationDefinition
                            )
                        )
                    }

                    "jwt_vc_json" -> {
                        Pair(
                            it.id,
                            createPresentationSubmissionJwtVc(
                                it,
                                authRequest,
                                presentationDefinition
                            )
                        )
                    }

                    else -> {
                        throw IllegalArgumentException(SIOPErrors.BAD_PARAMS.message)
                    }
                }
            }
            // presentation_submissionを生成
            val vpTokenValue = if (vpTokens.size == 1) {
                vpTokens[0].second.first
            } else if (vpTokens.isNotEmpty()) {
                val tokens = vpTokens.map { it.second.first }
                jacksonObjectMapper().writeValueAsString(tokens)
            } else {
                "" // 0件の場合はブランク
            }
            val presentationSubmission = PresentationSubmission(
                id = UUID.randomUUID().toString(),
                definitionId = presentationDefinition.id,
                descriptorMap = vpTokens.map { it.second.second }
            )
            println(presentationSubmission)
            val objectMapper: ObjectMapper = jacksonObjectMapper().apply {
                propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
            }
            val jsonString = objectMapper.writeValueAsString(presentationSubmission)

            // todo fragmentの場合はSame Deviceにリダイレクト
            val redirectUrl = requireNotNull(authRequest.responseUri)

            val body = mutableMapOf(
                "vp_token" to vpTokenValue,
                "presentation_submission" to jsonString
            )
            val state = authRequest.state
            if (!state.isNullOrBlank()) {
                body["state"] = state
            }

            println("send vp token to $redirectUrl")
            val result = sendRequest(redirectUrl, body)
            print("status code: ${result.statusCode}")
            print("location: ${result.location}")
            print("cookies: ${result.cookies}")
            val sharedContents = vpTokens.map { SharedContent(it.first, it.second.third) }
            return Either.Right(Pair(result, sharedContents))
        } catch (e: Exception) {
            return Either.Left(e.message ?: "IDToken Response Error")
        }
    }

    private fun createPresentationSubmissionSdJwtVc(
        credential: SubmissionCredential,
        authRequest: RequestObjectPayload,
        presentationDefinition: PresentationDefinition
    ): Triple<String, DescriptorMap, List<DisclosedClaim>> {
        val sdJwt = credential.credential
        val (_, selectedDisclosures) = selectDisclosure(sdJwt, presentationDefinition)!!
        val keyBindingJwt = keyBinding.generateJwt(
            sdJwt,
            selectedDisclosures,
            authRequest.clientId!!,
            authRequest.nonce!!,
        )
        // 絞ったdisclosureでチルダ連結してsd-jwtを構成
        val parts = sdJwt.split('~')
        val issuerSignedJwt = parts[0]
        val vpToken =
            issuerSignedJwt + "~" + selectedDisclosures.joinToString("~") { it.disclosure } + "~" + keyBindingJwt

        val pathNested = Path(format = credential.format, path = "$")
        val dm = DescriptorMap(
            id = credential.inputDescriptor.id,
            format = credential.format,
            path = "$",
            pathNested = pathNested
        )
        val disclosedClaims =
            selectedDisclosures.map { DisclosedClaim(credential.id, credential.types, it.key!!) }
        return Triple(vpToken, dm, disclosedClaims)
    }

    private fun createPresentationSubmissionJwtVc(
        credential: SubmissionCredential,
        authRequest: RequestObjectPayload,
        presentationDefinition: PresentationDefinition
    ): Triple<String, DescriptorMap, List<DisclosedClaim>> {
        if (authRequest.responseMode != ResponseMode.DIRECT_POST) {
            throw IllegalArgumentException("Unsupported response mode: ${authRequest.responseMode}")
        }
        try {
            val (_, payload, _) = JWT.decodeJwt(jwt = credential.credential)
            val disclosedClaims = payload.mapNotNull { (key, _) ->
                DisclosedClaim(id = credential.id, types = credential.types, name = key)
            }
            val pathNested = Path(format = credential.format, path = "$")
            val dm = DescriptorMap(
                id = credential.inputDescriptor.id,
                format = credential.format,
                path = "$",
                pathNested = pathNested
            )
            // todo check credential is matched condition specified by input_descriptor
            return Triple(
                first = credential.credential,
                second = dm,
                third = disclosedClaims
            )
        } catch (error: Exception) {
            throw error
        }
    }
}

fun generatePublicKeyJwk(keyPair: KeyPair, option: ProviderOption): Map<String, String> {
    val publicKey: PublicKey = keyPair.public

    return when (publicKey) {
        is RSAPublicKey -> generateRsaPublicKeyJwk(publicKey)
        is ECPublicKey -> generateEcPublicKeyJwk(publicKey, option)
        else -> throw IllegalArgumentException("Unsupported Key Type: ${publicKey::class.java.name}")
    }
}

fun correctBytes(value: BigInteger): ByteArray {
    /*
    BigInteger の toByteArray() メソッドは、数値をバイト配列に変換しますが、
    この数値が正の場合、最上位バイトが符号ビットとして解釈されることを避けるために、追加のゼロバイトが先頭に挿入されることがあります。
    これは、数値が正で、最上位バイトが 0x80 以上の場合（つまり、最上位ビットが 1 の場合）に起こります。
    その結果、期待していた 32 バイトではなく 33 バイトの配列が得られることがあります。

    期待する 32 バイトの配列を得るには、返されたバイト配列から余分なゼロバイトを取り除くか、
    または正確なバイト長を指定して配列を生成する必要があります。
     */
    val bytes = value.toByteArray()
    return if (bytes.size == 33 && bytes[0] == 0.toByte()) bytes.copyOfRange(
        1,
        bytes.size
    ) else bytes
}

fun generateEcPublicKeyJwk(ecPublicKey: ECPublicKey, option: ProviderOption): Map<String, String> {
    val ecPoint: ECPoint = ecPublicKey.w
    val x = correctBytes(ecPoint.affineX).toBase64Url()
    val y = correctBytes(ecPoint.affineY).toBase64Url()

    // return """{"kty":"EC","crv":"P-256","x":"$x","y":"$y"}""" // crvは適宜変更してください
    return mapOf(
        "kty" to "EC",
        "crv" to option.signingCurve,
        "x" to x,
        "y" to y
    )
}

fun generateRsaPublicKeyJwk(rsaPublicKey: RSAPublicKey): Map<String, String> {
    val n = Base64.getUrlEncoder().encodeToString(rsaPublicKey.modulus.toByteArray())
    val e = Base64.getUrlEncoder().encodeToString(rsaPublicKey.publicExponent.toByteArray())

    // return """{"kty":"RSA","n":"$n","e":"$e"}"""
    return mapOf(
        "kty" to "RSA",
        "n" to n,
        "e" to e
    )
}

fun getCurveName(ecPublicKey: ECPublicKey): String {
    val params = ecPublicKey.params

    return when (params) {
        is ECNamedCurveSpec -> {
            // Bouncy Castle の ECNamedCurveSpec の場合
            params.name
        }

        is ECParameterSpec -> {
            val curve = params.curve
            // 標準の Java ECParameterSpec の場合
            // ここでは、標準の Java API では曲線名を直接取得できないため、
            // 曲線のオーダーのビット長などに基づいて推定する方法を採用する
            when (params.order.bitLength()) {
                256 -> "P-256"
                384 -> "P-384"
                521 -> "P-521"
                else -> "不明なカーブ"
            }
        }

        else -> "サポートされていないパラメータタイプ"
    }
}

fun mergeOAuth2AndOpenIdInRequestPayload(
    payload: AuthorizationRequestPayload,
    requestObject: RequestObjectPayload? = null
): RequestObjectPayload {
    val module = SimpleModule().apply {
        addSerializer(Audience::class.java, AudienceSerializer())
    }
    val mapper = jacksonObjectMapper().apply {
        propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
        registerModule(module)
    }
    // AuthorizationRequestPayloadをMapに変換
    val payloadMap: Map<String, Any?> = mapper.convertValue(payload)

    // RequestObjectをMapに変換（nullの場合は空のMapを使用）
    val requestObjectMap: Map<String, Any?> = if (requestObject != null) {
        mapper.convertValue(requestObject)
    } else {
        emptyMap()
    }

    val mergedMap = payloadMap.toMutableMap().apply {
        requestObjectMap.forEach { (key, value) ->
            if (value != null) {
                this[key] = value
            }
        }
    }
    return createRequestObjectPayloadFromMap(mergedMap)
}

fun sendRequest(redirectUrl: String, formData: Map<String, String>): PostResult {
    val client = OkHttpClient.Builder()
        .followRedirects(false)
        .build()

    val formBodyBuilder = FormBody.Builder()
    formData.forEach { (key, value) ->
        formBodyBuilder.add(key, value)
    }
    val formBody = formBodyBuilder.build()

    val request = Request.Builder()
        .url(redirectUrl)
        .post(formBody)
        .build()

    client.newCall(request).execute().use { response ->
        val statusCode = response.code()
        var location = response.header("Location")
        val contentType = response.header("Content-Type")
        val cookies = response.headers("Set-Cookie")
        print("cookies@sendRequest: $cookies")

        if (statusCode == 302 && location != null) {
            // URI解析を使用して絶対URLかどうかを判断
            val uri = URI.create(location)
            if (!uri.isAbsolute) {
                // 元のURLからホスト情報を抽出して補完
                val originalUri = URI.create(redirectUrl)
                val portPart = if (originalUri.port != -1) ":${originalUri.port}" else ""
                location = "${originalUri.scheme}://${originalUri.host}$portPart$location"
            }
        } else if (statusCode == 200 && contentType?.contains("application/json") == true) {
            response.body()?.string()?.let { body ->
//                val jsonObject = JSONObject(body)
//                if (jsonObject.has("redirect_uri")) {
//                    location = jsonObject.getString("redirect_uri")
//                }
                val mapper = jacksonObjectMapper()
                val jsonNode = mapper.readTree(body)
                if (jsonNode.has("redirect_uri")) {
                    location = jsonNode.get("redirect_uri").asText()
                }
            }
        }

        return PostResult(
            statusCode = statusCode,
            location = location,
            cookies = if (cookies.isNotEmpty()) cookies.toTypedArray() else emptyArray()
        )
    }
}

fun satisfyConstrains(credential: Map<String, Any>, presentationDefinition: PresentationDefinition): Boolean {
    // TODO: 暫定で固定パス(vc.credentialSubject)のクレデンシャルをサポートする
    val vc = credential["vc"] as? Map<String, Any> ?: run {
        println("unsupported format")
        println(credential)
        return false
    }
    val credentialSubject = vc["credentialSubject"] as? Map<String, Any> ?: run {
        println("unsupported format")
        println(credential)
        return false
    }
    val inputDescriptors = presentationDefinition.inputDescriptors

    var matchingFieldsCount = 0

    inputDescriptors.forEach { inputDescriptor ->
        inputDescriptor.constraints.fields?.let { fields ->
            fields.forEach { field ->
                val isFieldMatched = field.path.any { jsonPath ->
                    val pathComponents = jsonPath.split(".")
                    pathComponents.last().let { lastComponent ->
                        if (lastComponent != "$") {
                            val key = lastComponent.replace("vc.", "")
                            // credentialのキーとして含まれているか判定
                            credentialSubject.keys.contains(key)
                        } else false
                    }
                }

                if (isFieldMatched) {
                    matchingFieldsCount++
                    return@forEach // pathのいずれかがマッチしたら、そのfieldは条件を満たしていると見なす
                }
            }
        }
    }

    println("match count: $matchingFieldsCount")
    // 元のfieldsの件数と該当したfieldの件数が一致するか判定
    return matchingFieldsCount == inputDescriptors.mapNotNull { it.constraints.fields }.size
}

data class ProcessSIOPRequestResult(
    val scheme: String,
    val requestObject: RequestObjectPayload,
    val authorizationRequestPayload: AuthorizationRequestPayload,
    val requestObjectJwt: String,
    val registrationMetadata: RPRegistrationMetadataPayload,
    val presentationDefinition: PresentationDefinition?,
)

data class SubmissionCredential(
    val id: String, // credential identifier
    val format: String, // specified by VCI
    val types: List<String>,
    val credential: String,
    val inputDescriptor: InputDescriptor,
)

data class DisclosedClaim(
    val id: String, // credential identifier
    val types: List<String>,
    val name: String
    // val path: String (when nested claim is supported, it may be needed like this)
)

data class SharedContent(
    val id: String,
    val sharedClaims: List<DisclosedClaim>
)

data class PostResult(
    val statusCode: Int,
    val location: String?,
    val cookies: Array<String>
)