package com.ownd_project.tw2023_wallet_android.oid

import com.auth0.jwt.JWT
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

suspend fun parseAndResolve(uri: String): ParseAndResolveResult {
    println("parseAndResolve: $uri")
    if (uri.isBlank()) {
        throw IllegalArgumentException(SIOPErrors.BAD_PARAMS.message)
    }

    println("parse")
    val (scheme, authorizationRequestPayload) = parse(uri)
    println("get request jwt")
    val requestObjectJwt = withContext(Dispatchers.IO) {
        fetchByReferenceOrUseByValue(
            referenceURI = authorizationRequestPayload.requestUri,
            valueObject = authorizationRequestPayload.request,
            responseType = String::class.java,
            textResponse = true
        )
    }
    println("request jwt: $requestObjectJwt")

    println("get client metadata")
    val decodedJwt = JWT.decode(requestObjectJwt)
    val clientMetadata = decodedJwt.getClaim("client_metadata")?.let {
        val mapper: ObjectMapper = jacksonObjectMapper().apply {
            propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
            configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true)
        }
        if (!it.isMissing && !it.isNull) mapper.readValue<RPRegistrationMetadataPayload>(it.toString()) else null
    }
    println("client metadata: $clientMetadata")
    val clientMetadataUri = decodedJwt.getClaim("client_metadata_uri").asString()?: authorizationRequestPayload.clientMetadataUri
    println("client metadata uri: $clientMetadataUri")
    // todo client idが数値の時の対応
    val registrationMetadataUri = authorizationRequestPayload.clientMetadataUri
    val registrationMetadataValue = clientMetadata?: authorizationRequestPayload.clientMetadata

    val registrationMetadata: RPRegistrationMetadataPayload = withContext(Dispatchers.IO) {
        if (clientMetadataUri == null && clientMetadata == null && authorizationRequestPayload.clientMetadata == null) {
            RPRegistrationMetadataPayload()
        } else {
            fetchByReferenceOrUseByValue(
                referenceURI = clientMetadataUri,
                valueObject = clientMetadata?: authorizationRequestPayload.clientMetadata,
                responseType = RPRegistrationMetadataPayload::class.java
            )
        }
    }

    println("get presentation definition")
    val presentationDefinitionValue = decodedJwt.getClaim("presentation_definition").let {
        if (!it.isMissing && !it.isNull) deserializePresentationDefinition(it.toString()) else null
    }

    val presentationDefinitionUri = decodedJwt.getClaim("presentation_definition_uri").let {
        if (!it.isMissing && !it.isNull) it.asString() else null
    }

    val presentationDefinition: PresentationDefinition? = if (presentationDefinitionValue != null || presentationDefinitionUri != null) {
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

    // assertValidRPRegistrationMetadataPayload(registrationMetadata)

    return ParseAndResolveResult(
        scheme,
        authorizationRequestPayload,
        requestObjectJwt,
        registrationMetadata,
        presentationDefinition,
        !decodedJwt.signature.isNullOrBlank()
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
