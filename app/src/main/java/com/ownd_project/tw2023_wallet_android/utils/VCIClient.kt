package com.ownd_project.tw2023_wallet_android.utils

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ownd_project.tw2023_wallet_android.oid.TokenErrorResponse
import okhttp3.FormBody
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONException
import java.io.IOException

data class TokenResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Int,
    val cNonce: String? = null,
    val cNonceExpiresIn: Int? = null,
)

data class CredentialResponse(
    val format: String,
    val credential: String,
    val cNonce: String? = null,
    val cNonceExpiresIn: Int? = null,
)


abstract class CredentialRequest(
    open val format: String,
    open val proof: Proof? = null
)

class TokenErrorResponseException(val errorResponse: TokenErrorResponse) :
    Exception("Error: ${errorResponse.error}, Description: ${errorResponse.errorDescription}")

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CredentialRequestSdJwtVc(
    override @JsonProperty("format")val format: String,
    override @JsonProperty("proof") val proof: Proof?,
    @JsonProperty("credential_definition")
    // todo: Improve the precision of type definitions.
    val credentialDefinition: Map<String, String>,
): CredentialRequest(format, proof)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CredentialRequestJwtVc(
    override val format: String,
    override val proof: Proof? = null,
    @JsonProperty("credential_definition")
    // todo: Improve the precision of type definitions.
    val credentialDefinition: Map<String, Any>,
): CredentialRequest(format, proof)

data class Proof(
    val proofType: String, val jwt: String,
)

class VCIClient() {
    private val client = OkHttpClient()

    fun postTokenRequest(
        url: String,
        preAuthCode: String,
        userPin: String? = null,
    ): TokenResponse? {
        val objectMapper = jacksonObjectMapper()
        objectMapper.propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE

        val formBodyBuilder = FormBody.Builder()
            .add("grant_type", "urn:ietf:params:oauth:grant-type:pre-authorized_code")
            .add("pre-authorized_code", preAuthCode)

        // userPinが提供されている場合は、リクエストボディに追加(Pre-Authorized Code Flow)
        if (userPin != null) {
            formBodyBuilder.add("user_pin", userPin)
        }

        val formBody = formBodyBuilder.build()
        val request = Request.Builder().url(url).post(formBody)
            .addHeader("Content-Type", "application/x-www-form-urlencoded").build()

        return client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string()
            if (response.code == 400) {
                val errorResponse = objectMapper.readValue(responseBody, TokenErrorResponse::class.java)
                throw TokenErrorResponseException(errorResponse)
            }
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            try {
                val tokenResponse = objectMapper.readValue(responseBody, TokenResponse::class.java)
                println("Response: $tokenResponse")
                return@use tokenResponse
            } catch (e: JSONException) {
                println("Failed to parse JSON: $e")
                return@use null
            }
        }
    }


    fun postCredentialRequest(
        url: String, credentialRequest: CredentialRequest, accessToken: String,
    ): CredentialResponse? {

        val objectMapper = jacksonObjectMapper()
        objectMapper.propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
        val jsonPayload = objectMapper.writeValueAsString(credentialRequest)

        // ... 以前と同様にOkHttpリクエストを構築・実行 ...
        val client = OkHttpClient()
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()

        val body = RequestBody.create(mediaType, jsonPayload)

        val request =
            Request.Builder().url(url).post(body).header("Authorization", "BEARER $accessToken")
                .header("Content-Type", "application/json").build()

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected code $response")
            }
            val responseBody = response.body?.string()
            try {
                val credentialResponse =
                    objectMapper.readValue(responseBody, CredentialResponse::class.java)
                println("Response: $credentialResponse")
                return@use credentialResponse
            } catch (e: JSONException) {
                println("Failed to parse JSON: $e")
                return@use null
            }
        }
    }
}