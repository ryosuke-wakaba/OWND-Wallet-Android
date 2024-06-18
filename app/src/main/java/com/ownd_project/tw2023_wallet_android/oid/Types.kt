package com.ownd_project.tw2023_wallet_android.oid

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.ownd_project.tw2023_wallet_android.utils.EnumDeserializer

data class TokenErrorResponse(val error: String, val errorDescription: String)

interface StringValueEnum {
    val value: String

    companion object {
        inline fun <reified T : Enum<T>> createFromString(value: String, values: Array<T>): T {
            return enumValues<T>().firstOrNull { (it as StringValueEnum).value == value }
                ?: throw IllegalArgumentException("Unknown enum type $value")
        }
    }

    @JsonValue
    fun toJson(): String = value
}

enum class ResponseMode(override val value: String) : StringValueEnum {
    FRAGMENT("fragment"),
    FRAGMENT_JWT("fragment.jwt"),
    FORM_POST("form_post"),
    FORM_POST_JWT("form_post.jwt"),
    JWT("jwt"),
    POST("post"),
    QUERY("query"),
    QUERY_JWT("query.jwt"),
    DIRECT_POST("direct_post"),
    DIRECT_POST_JWT("direct_post.jwt");

    companion object {
        @JsonCreator
        @JvmStatic
        fun fromValue(value: String): ResponseMode =
            StringValueEnum.createFromString(value, values())
    }
}

enum class Scope(override val value: String) : StringValueEnum {
    OPENID("openid"),
    OPENID_DIDAUTHN("openid did_authn"),
    PROFILE("profile"),
    EMAIL("email"),
    ADDRESS("address"),
    PHONE("phone"),
    OFFLINE_ACCESS("offline_access");

    companion object {
        @JsonCreator
        @JvmStatic
        fun fromValue(value: String): Scope =
            StringValueEnum.createFromString(value, Scope.values())
    }
}

enum class SubjectType(override val value: String) : StringValueEnum {
    PUBLIC("public"),
    PAIRWISE("pairwise");

    companion object {
        @JsonCreator
        @JvmStatic
        fun fromValue(value: String): SubjectType =
            StringValueEnum.createFromString(value, SubjectType.values())
    }
}

enum class SigningAlgo(override val value: String) : StringValueEnum {
    EDDSA("EdDSA"),
    RS256("RS256"),
    PS256("PS256"),
    ES256("ES256"),
    ES256K("ES256K");

    companion object {
        @JsonCreator
        @JvmStatic
        fun fromValue(value: String): SigningAlgo =
            StringValueEnum.createFromString(value, values())
    }
}

enum class GrantType(override val value: String) : StringValueEnum {
    AUTHORIZATION_CODE("authorization_code"),
    IMPLICIT("implicit");

    companion object {
        @JsonCreator
        @JvmStatic
        fun fromValue(value: String): GrantType =
            StringValueEnum.createFromString(value, values())
    }
}

enum class AuthenticationContextReferences(override val value: String) : StringValueEnum {
    PHR("phr"),
    PHRH("phrh");

    companion object {
        @JsonCreator
        @JvmStatic
        fun fromValue(value: String): AuthenticationContextReferences =
            StringValueEnum.createFromString(value, values())
    }
}

enum class TokenEndpointAuthMethod(override val value: String) : StringValueEnum {
    CLIENT_SECRET_POST("client_secret_post"),
    CLIENT_SECRET_BASIC("client_secret_basic"),
    CLIENT_SECRET_JWT("client_secret_jwt"),
    PRIVATE_KEY_JWT("private_key_jwt");

    companion object {
        @JsonCreator
        @JvmStatic
        fun fromValue(value: String): TokenEndpointAuthMethod =
            StringValueEnum.createFromString(value, values())
    }
}

enum class ClaimType(override val value: String) : StringValueEnum {
    NORMAL("normal"),
    AGGREGATED("aggregated"),
    DISTRIBUTED("distributed");

    companion object {
        @JsonCreator
        @JvmStatic
        fun fromValue(value: String): ClaimType =
            StringValueEnum.createFromString(value, values())
    }
}

enum class Format(val value: String) {
    JWT("jwt"),
    JWT_VC("jwt_vc"),
    JWT_VC_JSON("jwt_vc_json"),
    JWT_VP("jwt_vp"),
    LDP("ldp"),
    LDP_VC("ldp_vc"),
    LDP_VP("ldp_vp");

    companion object {
        @JsonCreator
        @JvmStatic
        fun fromValue(value: String): Format {
            return values().firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown format value: $value")
        }
    }
}

sealed class Audience {
    data class Single(val value: String) : Audience()
    data class Multiple(val values: List<String>) : Audience()
}

class AudienceDeserializer : JsonDeserializer<Audience>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Audience {
        val node: JsonNode = p.codec.readTree(p)
        return if (node.isArray) {
            // JSONノードが配列の場合、複数の値を持つAudienceを生成
            Audience.Multiple(node.map { it.asText() })
        } else {
            // JSONノードが配列でない場合、単一の値を持つAudienceを生成
            Audience.Single(node.asText())
        }
    }
}

class AudienceSerializer : JsonSerializer<Audience>() {
    override fun serialize(value: Audience, gen: JsonGenerator, serializers: SerializerProvider) {
        when (value) {
            is Audience.Single -> gen.writeString(value.value)
            is Audience.Multiple -> gen.writeArray(
                value.values.toTypedArray(),
                0,
                value.values.size
            )
        }
    }
}

interface PrivateJwk {
    val kty: String
    val d: String
}

data class ECPrivateJwk(
    override val kty: String,
    override val d: String,
    val crv: String,
    val x: String,
    val y: String,
) : PrivateJwk

data class RsaPrivateJwk(
    override val kty: String,
    override val d: String,
    val n: String,
    val e: String
) : PrivateJwk

interface JWTPayload {
    val iss: String?
    val sub: String?
    val aud: Audience?
    val iat: Long?
//    val nbf: Long?
//    val type: String?
    val exp: Long?
//    val jti: String?
}

interface IDTokenPayload : JWTPayload {
    val nonce: String?
//    val authTime: Long?
//    val acr: String?
//    val azp: String?
    val subJwk: Map<String, String>?
}

class IDTokenPayloadImpl(
    override val iss: String? = null,
    override val sub: String? = null,
    override val aud: Audience? = null,
    override val iat: Long? = null,
//    override val nbf: Long? = null,
//    override val type: String? = null,
    override val exp: Long? = null,
//    override val jti: String? = null,
    override val nonce: String? = null,
//    override val authTime: Long? = null,
//    override val acr: String? = null,
//    override val azp: String? = null,
    override val subJwk: Map<String, String>? = null
) : IDTokenPayload

interface AuthorizationRequestCommonPayload {
    val scope: String?
    val responseType: String?
    val clientId: String?
    val redirectUri: String?
    val idTokenHint: String?
    val nonce: String?
    val state: String?
    val responseMode: ResponseMode?
    val maxAge: Int?
    val clientMetadata: RPRegistrationMetadataPayload?
    val clientMetadataUri: String?
    val responseUri: String?
    val presentationDefinition: PresentationDefinition?
    val presentationDefinitionUri: String?
    val clientIdScheme: String?
}

interface RequestObjectPayload : AuthorizationRequestCommonPayload, JWTPayload

interface AuthorizationRequestPayload : AuthorizationRequestCommonPayload {
    val request: String?
    val requestUri: String?
}

@JsonIgnoreProperties("presentation_definition")
class RequestObjectPayloadImpl(
    override val iss: String? = null,
    override val sub: String? = null,
    override val aud: Audience? = null,
    override val iat: Long? = null,
//    override val nbf: Long? = null,
//    override val type: String? = null,
    override val exp: Long? = null,
//    override val jti: String? = null,
    override val scope: String? = null,
    override val responseType: String? = null,
    override val clientId: String? = null,
    override val redirectUri: String? = null,
    override val idTokenHint: String? = null,
    override val nonce: String? = null,
    override val state: String? = null,
    override val responseMode: ResponseMode? = null,
    override val maxAge: Int? = null,
    override val clientMetadata: RPRegistrationMetadataPayload? = null,
    override val clientMetadataUri: String? = null,
    override val responseUri: String? = null,
    override val presentationDefinition: PresentationDefinition? = null,
    override val presentationDefinitionUri: String? = null,
    override val clientIdScheme: String? = null,
) : RequestObjectPayload

class AuthorizationRequestPayloadImpl(
    override val scope: String? = null,
    override val responseType: String? = null,
    override val clientId: String? = null,
    override val redirectUri: String? = null,
    override val idTokenHint: String? = null,
    override val nonce: String? = null,
    override val state: String? = null,
    override val responseMode: ResponseMode? = null,
    override val maxAge: Int? = null,
    override val clientMetadata: RPRegistrationMetadataPayload? = null,
    override val clientMetadataUri: String? = null,
    override val request: String? = null,
    override val requestUri: String? = null,
    override val responseUri: String? = null,
    override val presentationDefinition: PresentationDefinition? = null,
    override val presentationDefinitionUri: String? = null,
    override val clientIdScheme: String? = null,
) : AuthorizationRequestPayload


data class AuthorizationServerMetadata(
    val authorizationEndpoint: String? = null,
    val issuer: String? = null,
    val responseTypesSupported: List<String>? = null,
    val scopesSupported: List<Scope>? = null,
    val subjectTypesSupported: List<SubjectType>? = null,
    val idTokenSigningAlgValuesSupported: List<SigningAlgo>? = null,
    val requestObjectSigningAlgValuesSupported: List<SigningAlgo>? = null,
    val subjectSyntaxTypesSupported: List<String>? = null,
    val tokenEndpoint: String? = null,
    val userinfoEndpoint: String? = null,
    val jwksUri: String? = null,
    val registrationEndpoint: String? = null,
    val responseModesSupported: List<ResponseMode>? = null,
    val grantTypesSupported: List<GrantType>? = null,
    val acrValuesSupported: List<AuthenticationContextReferences>? = null,
    val idTokenEncryptionAlgValuesSupported: List<SigningAlgo>? = null,
    val idTokenEncryptionEncValuesSupported: List<String>? = null,
    val userinfoSigningAlgValuesSupported: List<SigningAlgo>? = null,
    val userinfoEncryptionAlgValuesSupported: List<SigningAlgo>? = null,
    val userinfoEncryptionEncValuesSupported: List<String>? = null,
    val requestObjectEncryptionAlgValuesSupported: List<SigningAlgo>? = null,
    val requestObjectEncryptionEncValuesSupported: List<String>? = null,
    val tokenEndpointAuthMethodsSupported: List<TokenEndpointAuthMethod>? = null,
    val tokenEndpointAuthSigningAlgValuesSupported: List<SigningAlgo>? = null,
    val displayValuesSupported: List<String>? = null,
    val claimTypesSupported: List<ClaimType>? = null,
    val claimsSupported: List<String>? = null,
    val serviceDocumentation: String? = null,
    val claimsLocalesSupported: List<String>? = null,
    val uiLocalesSupported: List<String>? = null,
    val claimsParameterSupported: Boolean? = null,
    val requestParameterSupported: Boolean? = null,
    val requestUriParameterSupported: Boolean? = null,
    val requireRequestUriRegistration: Boolean? = null,
    val opPolicyUri: String? = null,
    val opTosUri: String? = null,
    val clientId: String? = null,
    val redirectUris: List<String>? = null,
    val clientName: String? = null,
    val tokenEndpointAuthMethod: String? = null,
    val applicationType: String? = null,
    val responseTypes: String? = null,
    val grantTypes: String? = null,
    val vpFormats: Format? = null,
    val logoUri: String? = null,
    val clientPurpose: String? = null,
    val idTokenTypesSupported: Any? = null, // IdTokenType[] | IdTokenType
    val vpFormatsSupported: Format? = null,
    @JsonProperty("pre-authorized_grant_anonymous_access_supported")
    val preAuthorizedGrantAnonymousAccessSupported: Boolean? = null,
)

val openIdMetadataObjectMapper: ObjectMapper = jacksonObjectMapper().apply {
    propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
    configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true)
    val module = SimpleModule().apply {
        addDeserializer(Scope::class.java, EnumDeserializer(Scope::class))
        addDeserializer(SubjectType::class.java, EnumDeserializer(SubjectType::class))
        addDeserializer(SigningAlgo::class.java, EnumDeserializer(SigningAlgo::class))
        addDeserializer(ResponseMode::class.java, EnumDeserializer(ResponseMode::class))
        addDeserializer(GrantType::class.java, EnumDeserializer(GrantType::class))
        addDeserializer(
            AuthenticationContextReferences::class.java,
            EnumDeserializer(AuthenticationContextReferences::class)
        )
        addDeserializer(
            TokenEndpointAuthMethod::class.java,
            EnumDeserializer(TokenEndpointAuthMethod::class)
        )
        addDeserializer(ClaimType::class.java, EnumDeserializer(ClaimType::class))
        addDeserializer(Format::class.java, EnumDeserializer(Format::class))


    }
    registerModule(module)
}

// https://openid.net/specs/openid-connect-registration-1_0.html
data class RPRegistrationMetadataPayload(
    val scopesSupported: List<Scope>? = null,
    val subjectTypesSupported: List<SubjectType>? = null,
    val idTokenSigningAlgValuesSupported: List<SigningAlgo>? = null,
    val requestObjectSigningAlgValuesSupported: List<SigningAlgo>? = null,
    val subjectSyntaxTypesSupported: List<String>? = null,
    val requestObjectSigningAlg: SigningAlgo? = null,
    val requestObjectEncryptionAlgValuesSupported: List<SigningAlgo>? = null,
    val requestObjectEncryptionEncValuesSupported: List<String>? = null,
    val clientId: String? = null,
    val clientName: String? = null,
    val vpFormats: Map<String, Map<String, List<String>>>? = null,
    // val vpFormats: Format? = null,
    val logoUri: String? = null,
    val policyUri: String? = null,
    val tosUri: String? = null,
    val clientPurpose: String? = null,
    val jwks: String? = null, // todo [プロジェクト完了後] JWK Setの型で受け取る
    val jwksUri: String? = null,
    val vpFormatsSupported: Format? = null,
    val redirectUris: List<String>? = null,
)

fun createRequestObjectPayloadFromMap(map: Map<String, Any?>): RequestObjectPayload {
    val mapper = jacksonObjectMapper().apply {
        propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
    }

    val clientMetadata = map["client_metadata"]?.let {
        // マップをJSON文字列にシリアライズ
        val json = mapper.writeValueAsString(it)
        // JSON文字列をRPRegistrationMetadataPayloadにデシリアライズ
        mapper.readValue<RPRegistrationMetadataPayload>(json)
    }
    return RequestObjectPayloadImpl(
        // JWTPayloadのプロパティ
        iss = map["iss"] as? String,
        sub = map["sub"] as? String,
        // aud は String または String のリストか、Audience インスタンスになります
        aud = when (val audValue = map["aud"]) {
            is String -> Audience.Single(audValue)
            is List<*> -> Audience.Multiple(audValue.filterIsInstance<String>())
            is Audience -> audValue
            else -> null
        },
        iat = (map["iat"] as? Number)?.toLong(),
//        nbf = (map["nbf"] as? Number)?.toLong(),
//        type = map["type"] as? String,
        exp = (map["exp"] as? Number)?.toLong(),
//        jti = map["jti"] as? String,

        // RequestCommonPayloadのプロパティ
        scope = map["scope"] as? String,
        responseType = map["response_type"] as? String,
        clientId = map["client_id"] as? String,
        redirectUri = map["redirect_uri"] as? String,
        responseUri = map["response_uri"] as? String,
        idTokenHint = map["id_token_hint"] as? String,
        nonce = map["nonce"] as? String,
        state = map["state"] as? String,
        responseMode = map["response_mode"]?.let {
            try {
                ResponseMode.valueOf((it as String).toUpperCase())
            } catch (e: IllegalArgumentException) {
                null
            }
        },
        maxAge = map["max_age"] as? Int,

        clientMetadata = clientMetadata,
        clientMetadataUri = map["client_metadata_uri"] as? String,
        clientIdScheme = map["client_id_scheme"] as? String
    )
}

fun createAuthorizationRequestPayloadFromMap(map: Map<String, Any?>): AuthorizationRequestPayload {
//    val mapper = jacksonObjectMapper().apply {
//        propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
//    }
    val mapper: ObjectMapper = jacksonObjectMapper().apply {
        propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
        configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true)
        val module = SimpleModule().apply {
            addDeserializer(LimitDisclosure::class.java, EnumDeserializer(LimitDisclosure::class))
            addDeserializer(Rule::class.java, EnumDeserializer(Rule::class))
        }
        registerModule(module)
    }

    val clientMetadata = map["client_metadata"]?.let {
        val json = mapper.writeValueAsString(it)
        mapper.readValue<RPRegistrationMetadataPayload>(json)
    }

    val presentationDefinition = map["presentation_definition"]?.let {
        val json = mapper.writeValueAsString(it)
        mapper.readValue<PresentationDefinition>(json)
    }

    return AuthorizationRequestPayloadImpl(
        // RequestCommonPayloadのプロパティ
        scope = map["scope"] as? String,
        responseType = map["response_type"] as? String,
        clientId = map["client_id"] as? String,
        redirectUri = map["redirect_uri"] as? String,
        idTokenHint = map["id_token_hint"] as? String,
        nonce = map["nonce"] as? String,
        state = map["state"] as? String,
        responseMode = map["response_mode"]?.let {
            try {
                ResponseMode.valueOf((it as String).toUpperCase())
            } catch (e: IllegalArgumentException) {
                null
            }
        },
        maxAge = map["max_age"] as? Int,
        clientMetadata = clientMetadata,
        clientMetadataUri = map["client_metadata_uri"] as? String,

        presentationDefinition = presentationDefinition,
        presentationDefinitionUri = map["presentation_definition_uri"] as? String,

        // AuthorizationRequestCommonPayloadのプロパティ
        request = map["request"] as? String,
        requestUri = map["request_uri"] as? String,
        clientIdScheme = map["client_id_scheme"] as? String
    )
}