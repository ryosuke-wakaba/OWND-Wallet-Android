package com.ownd_project.tw2023_wallet_android.oid

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.Claim
import com.auth0.jwt.interfaces.DecodedJWT
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.URLDecoder

fun decodeUriAsJson(uri: String): Map<String, Any> {
    if (uri.isBlank()) {
        throw IllegalArgumentException(SIOPErrors.BAD_PARAMS.message)
    }

    val queryString =
        URI(uri).query ?: throw IllegalArgumentException(SIOPErrors.BAD_PARAMS.message)

    val params = queryString.split("&").map { it.split("=") }
    val json = mutableMapOf<String, Any>()
    val mapper = jacksonObjectMapper()

    for (param in params) {
        if (param.size != 2) continue
        val key = URLDecoder.decode(param[0], "UTF-8")
        val value = URLDecoder.decode(param[1], "UTF-8")

        when {
            value.toBooleanStrictOrNull() != null -> json[key] = value.toBoolean()
            value.toIntOrNull() != null -> json[key] = value.toInt()
            value.startsWith("{") && value.endsWith("}") -> json[key] = mapper.readValue(value)
            else -> json[key] = value
        }
    }

    return json
}

fun parse(uri: String): Pair<String, AuthorizationRequestPayload> {
    if (uri.isBlank()) {
        throw IllegalArgumentException(SIOPErrors.BAD_PARAMS.message)
    }

    val scheme = Regex("^([a-zA-Z][a-zA-Z0-9-_]*)://").find(uri)?.groupValues?.get(1)
        ?: throw IllegalArgumentException(SIOPErrors.BAD_PARAMS.message)

//        ?: throw IllegalArgumentException(SIOPErrors.BAD_PARAMS.message)

    val authorizationRequestPayloadMap = decodeUriAsJson(uri)
    val authorizationRequestPayload =
        createAuthorizationRequestPayloadFromMap(authorizationRequestPayloadMap)

    return Pair(scheme, authorizationRequestPayload)
}

class RequestObjectHandler(private val authorizationRequestPayload: AuthorizationRequestPayload) {
    lateinit var requestObjectJwt: String
    private lateinit var decodedJwt: DecodedJWT

    val isSigned: Boolean
        get() = !((authorizationRequestPayload.request
            ?: authorizationRequestPayload.requestUri).isNullOrBlank())

    private suspend fun getRequestObjectJwt(): String {
        if (::requestObjectJwt.isInitialized) {
            println("request jwt: $requestObjectJwt")
            return requestObjectJwt
        }
        requestObjectJwt = withContext(Dispatchers.IO) {
            fetchByReferenceOrUseByValue(
                referenceURI = authorizationRequestPayload.requestUri,
                valueObject = authorizationRequestPayload.request,
                responseType = String::class.java,
                textResponse = true
            )
        }
        println("request jwt: $requestObjectJwt")
        return requestObjectJwt
    }

    suspend fun getClaimValue(claimName: String): String? {
        if (!::decodedJwt.isInitialized) {
            decodedJwt = JWT.decode(getRequestObjectJwt())
        }
        val claim = decodedJwt.getClaim(claimName)?.let {
            if (!it.isMissing && !it.isNull) it.asString() else null
        }
        return claim
    }

    suspend fun getClaim(claimName: String): Claim? {
        if (!::decodedJwt.isInitialized) {
            decodedJwt = JWT.decode(getRequestObjectJwt())
        }
        val claim = decodedJwt.getClaim(claimName)
        return claim
    }
}

class ClientMetadataHandler(
    private val requestObjectHandler: RequestObjectHandler,
    private val authorizationRequestPayload: AuthorizationRequestPayload
) {
    suspend fun getMetadata(): RPRegistrationMetadataPayload {
        println("get client metadata")
        return if (!requestObjectHandler.isSigned) {
            val clientMetadata = authorizationRequestPayload.clientMetadata
            println("client metadata: $clientMetadata")
            val clientMetadataUri = authorizationRequestPayload.clientMetadataUri
            println("client metadata uri: $clientMetadataUri")

            val registrationMetadata: RPRegistrationMetadataPayload = withContext(Dispatchers.IO) {
                if (clientMetadataUri == null && clientMetadata == null) {
                    RPRegistrationMetadataPayload()
                } else {
                    fetchByReferenceOrUseByValue(
                        referenceURI = clientMetadataUri,
                        valueObject = clientMetadata,
                        responseType = RPRegistrationMetadataPayload::class.java
                    )
                }
            }
            registrationMetadata
        } else {
//            val clientMetadataValue = requestObjectHandler.getClaimValue("client_metadata")
            val clientMetadataValue = requestObjectHandler.getClaim("client_metadata")
            val mapper: ObjectMapper = jacksonObjectMapper().apply {
                propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
                configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true)
            }
            val clientMetadata =
                if (clientMetadataValue != null && !clientMetadataValue.isNull && !clientMetadataValue.isMissing) mapper.readValue<RPRegistrationMetadataPayload>(
                    clientMetadataValue.toString()
                ) else null
            println("client metadata: $clientMetadata")

            val clientMetadataUri = requestObjectHandler.getClaimValue("client_metadata_uri")
            println("client metadata uri: $clientMetadataUri")

            val registrationMetadata: RPRegistrationMetadataPayload = withContext(Dispatchers.IO) {
                if (clientMetadataUri == null && clientMetadata == null) {
                    RPRegistrationMetadataPayload()
                } else {
                    fetchByReferenceOrUseByValue(
                        referenceURI = clientMetadataUri,
                        valueObject = clientMetadata,
                        responseType = RPRegistrationMetadataPayload::class.java
                    )
                }
            }
            registrationMetadata
        }
    }
}

class PresentationDefinitionHandler(
    private val requestObjectHandler: RequestObjectHandler,
    private val authorizationRequestPayload: AuthorizationRequestPayload
) {
    suspend fun get(): PresentationDefinition? {
        println("get presentation definition")
        val presentationDefinitionValue = if (requestObjectHandler.isSigned) {
            val claim = requestObjectHandler.getClaim("presentation_definition")
            val presentationDefinitionValue =
                if (claim != null && !claim.isMissing && !claim.isNull) deserializePresentationDefinition(
                    claim.toString()
                ) else null
            presentationDefinitionValue
        } else {
            authorizationRequestPayload.presentationDefinition
        }
        val presentationDefinitionUri = if (requestObjectHandler.isSigned) {
            val claim = requestObjectHandler.getClaim("presentation_definition_uri")
            if (claim != null && !claim.isMissing && !claim.isNull) claim.asString() else null
        } else {
            authorizationRequestPayload.presentationDefinitionUri
        }
        val presentationDefinition: PresentationDefinition? =
            if (presentationDefinitionValue != null || presentationDefinitionUri != null) {
                withContext(Dispatchers.IO) {
                    fetchByReferenceOrUseByValue(
                        referenceURI = presentationDefinitionUri,
                        valueObject = presentationDefinitionValue,
                        responseType = PresentationDefinition::class.java
                    )
                }
            } else {
                null
            }
        return presentationDefinition
    }
}

suspend fun parseAndResolve(uri: String): ParseAndResolveResult {
    println("parseAndResolve: $uri")
    if (uri.isBlank()) {
        throw IllegalArgumentException(SIOPErrors.BAD_PARAMS.message)
    }

    println("parse")
    val (scheme, authorizationRequestPayload) = parse(uri)

    val requestObjectHandler = RequestObjectHandler(authorizationRequestPayload)

    // client metadata
    val clientMetadataHandler =
        ClientMetadataHandler(requestObjectHandler, authorizationRequestPayload)
    val registrationMetadata = clientMetadataHandler.getMetadata()

    // presentation definition
    val presentationDefinitionHandler =
        PresentationDefinitionHandler(requestObjectHandler, authorizationRequestPayload)
    val presentationDefinition = presentationDefinitionHandler.get()

    return ParseAndResolveResult(
        scheme,
        authorizationRequestPayload,
        if (requestObjectHandler.isSigned) requestObjectHandler.requestObjectJwt else "",
        registrationMetadata,
        presentationDefinition,
        requestObjectHandler.isSigned
    )
}

data class ParseAndResolveResult(
    val scheme: String,
    val authorizationRequestPayload: AuthorizationRequestPayload,
    val requestObjectJwt: String,
    val registrationMetadata: RPRegistrationMetadataPayload,
    val presentationDefinition: PresentationDefinition?,
    val requestIsSigned: Boolean
)
