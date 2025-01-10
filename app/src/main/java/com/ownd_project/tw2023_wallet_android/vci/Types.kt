package com.ownd_project.tw2023_wallet_android.vci

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.ownd_project.tw2023_wallet_android.oid.AuthorizationServerMetadata
import okhttp3.Response

data class ErrorResponse(
    val error: String,
    val error_description: String?,
    val error_uri: String?,
    val state: String?,
)

data class OpenIDResponse<T>(
    val origResponse: Response,
    val successBody: T?,
    val errorBody: ErrorResponse?,
)

enum class WellKnownEndpoints(val endpoint: String) {
    OPENID_CONFIGURATION("/.well-known/openid-configuration"),
    OAUTH_AS("/.well-known/oauth-authorization-server"),
    OPENID4VCI_ISSUER("/.well-known/openid-credential-issuer")
}

sealed class AuthorizationServerType {
    object OIDC : AuthorizationServerType()
    object OAuth2 : AuthorizationServerType()
    object OID4VCI : AuthorizationServerType()
}

data class CredentialResponseEncryption(
    val algValuesSupported: List<String>,
    val encValuesSupported: List<String>,
    val encryptionRequired: Boolean
)

data class CredentialIssuerMetadata(
    val credentialIssuer: String,
    val authorizationServers: List<String>? = null,
    val credentialEndpoint: String? = null,
    val batchCredentialEndpoint: String? = null,
    val deferredCredentialEndpoint: String? = null,
    val notificationEndpoint: String? = null,
    val credentialResponseEncryption: CredentialResponseEncryption? = null,
    val credentialIdentifiersSupported: Boolean? = null,
    val signedMetadata: String? = null,
    val display: List<CredentialsSupportedDisplay>? = null,
    val credentialConfigurationsSupported: Map<String, CredentialConfiguration>,
)

data class EndpointMetadataResult(
    val credentialIssuerMetadata: CredentialIssuerMetadata?,
    val authorizationServerMetadata: AuthorizationServerMetadata?,
)

data class ImageInfo(
    val uri: String? = null,
    val altText: String? = null,
    val additionalProperties: Map<String, Any?>? = null,
)

open class Display(
    open val name: String? = null,
    open val locale: String? = null,
)

data class CredentialsSupportedDisplay(
    val logo: ImageInfo? = null,
    val description: String? = null,
    val backgroundColor: String? = null,
    val backgroundImage: String? = null,
    val textColor: String? = null,
    override val name: String? = null,
    override val locale: String? = null,
) : Display()

typealias ICredentialContextType = Any

@JsonDeserialize(using = IssuerCredentialSubjectDeserializer::class)
data class IssuerCredentialSubject(
    val mandatory: Boolean? = null,
    val valueType: String? = null,
    val display: List<Display>? = null,
)

class IssuerCredentialSubjectDeserializer : JsonDeserializer<IssuerCredentialSubject>() {
    private val mapper = ObjectMapper()
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): IssuerCredentialSubject {
        // your custom deserialization logic here
        val node = p.codec.readTree<JsonNode>(p)
        val mandatory = node.get("mandatory")?.asBoolean()
        val valueType = node.get("value_type")?.asText()
        val display: List<Display>? = node.get("display")?.let {
            mapper.readValue(it.traverse(), object : TypeReference<List<Display>>() {})
        }

        return IssuerCredentialSubject(
            mandatory = mandatory,
            valueType = valueType,
            display = display
        )
    }
}

typealias IssuerCredentialSubjectMap = Map<String, IssuerCredentialSubject>

data class ProofSigningAlgValuesSupported(
    val proofSigningAlgValuesSupported: List<String>?
)

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "format",
    visible = true
)
@JsonSubTypes(
    JsonSubTypes.Type(value = CredentialConfigurationJwtVcJsonLdAndLdpVc::class, name = "ldp_vc"),
    // todo: `jwt_vc_json-ld`の追加
    JsonSubTypes.Type(value = CredentialConfigurationJwtVcJson::class, name = "jwt_vc_json"),
    JsonSubTypes.Type(value = CredentialConfigurationVcSdJwt::class, name = "vc+sd-jwt")
)
interface CredentialConfiguration {
    val format: String
    val scope: String?
    val cryptographicBindingMethodsSupported: List<String>?
    val credentialSigningAlgValuesSupported: List<String>?
    val proofTypesSupported: Map<String, ProofSigningAlgValuesSupported>?
    val display: List<CredentialsSupportedDisplay>?
}

open class CommonCredentialConfiguration(
    override val format: String,
    override val scope: String? = null,
    override val cryptographicBindingMethodsSupported: List<String>? = null,
    override val credentialSigningAlgValuesSupported: List<String>? = null,
    override val proofTypesSupported: Map<String, ProofSigningAlgValuesSupported>? = null,
    override val display: List<CredentialsSupportedDisplay>? = null,
) : CredentialConfiguration

data class CredentialConfigurationJwtVcJson(
    override val format: String,
    override val scope: String,
    override val cryptographicBindingMethodsSupported: List<String>? = null,
    override val credentialSigningAlgValuesSupported: List<String>? = null,
    override val proofTypesSupported: Map<String, ProofSigningAlgValuesSupported>? = null,
    val credentialDefinition: JwtVcJsonCredentialDefinition,
    val order: List<String>? = null,
) : CommonCredentialConfiguration(format)

data class JwtVcJsonCredentialDefinition(
    val type: List<String>,
    // このプロパティは仕様上キャメルケースなのでスネークケースストラテジーでデシリアライズするためにアノテーションが必要
    @JsonProperty("credentialSubject")
    val credentialSubject: IssuerCredentialSubjectMap? = null,
)

data class CredentialConfigurationVcSdJwt(
    override val format: String,
    override val scope: String,
    override val cryptographicBindingMethodsSupported: List<String>? = null,
    override val credentialSigningAlgValuesSupported: List<String>? = null,
    override val proofTypesSupported: Map<String, ProofSigningAlgValuesSupported>? = null,
    val vct: String,
    val claims: IssuerCredentialSubjectMap,
    val order: List<String>? = null,
) : CommonCredentialConfiguration(format)


data class LdpVcCredentialDefinition(
    @JsonProperty("@context")
    val context: List<ICredentialContextType>,
    val type: List<String>,
    // このプロパティは仕様上キャメルケースなのでスネークケースストラテジーでデシリアライズするためにアノテーションが必要
    @JsonProperty("credentialSubject")
    val credentialSubject: IssuerCredentialSubjectMap? = null,
)

data class CredentialConfigurationJwtVcJsonLdAndLdpVc(
    override val format: String,
    override val cryptographicBindingMethodsSupported: List<String>? = null,
    override val credentialSigningAlgValuesSupported: List<String>? = null,
    val credentialDefinition: LdpVcCredentialDefinition,
    val order: List<String>?,
) : CommonCredentialConfiguration(format)

sealed class OID4VCICredentialFormat {
    object JwtVcJson : OID4VCICredentialFormat()
    object JwtVcJsonLd : OID4VCICredentialFormat()
    object LdpVc : OID4VCICredentialFormat()
    data class Unknown(val value: String) : OID4VCICredentialFormat()
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
    property = "format"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = CredentialsJwtVc::class, name = "jwt_vc_json"),
    JsonSubTypes.Type(value = CredentialsVcSdJwt::class, name = "vc+sd-jwt"),
)
interface Credentials

data class CredentialsJwtVc(
    val types: List<String>,
) : Credentials

data class CredentialsVcSdJwt(
    val types: List<String>,
) : Credentials

data class GrantAuthorizationCode(
    val issuerState: String?,
)

data class TxCode(
    val inputMode: String?,
    val length: Int?,
    val description: String?
)

data class GrantUrnIetf(
    @JsonProperty("pre-authorized_code")
    val preAuthorizedCode: String,
    val txCode: TxCode?,
)

data class Grant(
    val authorizationCode: GrantAuthorizationCode?,
    @JsonProperty("urn:ietf:params:oauth:grant-type:pre-authorized_code")
    val urnIetfParams: GrantUrnIetf?,
)

data class CredentialOffer(
    val credentialIssuer: String,
    val credentials: List<String>,
    val grants: Grant?,
)
