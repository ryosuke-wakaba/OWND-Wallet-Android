package com.ownd_project.tw2023_wallet_android.oid

import com.ownd_project.tw2023_wallet_android.utils.EnumDeserializer
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import org.json.JSONException


suspend fun <T> getWithUrl(url: String, responseType: Class<T>, textResponse: Boolean = false): T {
    val client = OkHttpClient()

    return withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .build()

        var customError: String? = null
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("${SIOPErrors.RESPONSE_STATUS_UNEXPECTED} ${response.code}:${response.message} URL: $url")
            }

            @Suppress("UNCHECKED_CAST")
            if (textResponse) {
                response.body?.string() as T
            } else {
                val responseText = response.body?.string() ?: ""
                // val mapper = jacksonObjectMapper()
                val mapper: ObjectMapper = jacksonObjectMapper().apply {
                    propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
                    configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true)
                    val module = SimpleModule().apply {
                        // enum型のプロパティが増えたら追加する(todo 設計を見直したい)
                        addDeserializer(LimitDisclosure::class.java, EnumDeserializer(LimitDisclosure::class))
                        addDeserializer(Rule::class.java, EnumDeserializer(Rule::class))
                    }
                    registerModule(module)
                }
                try {
                    mapper.readValue(responseText, responseType)
                } catch (e: JSONException) {
                    customError = "JSON parse error"
                    throw Error(customError)
                }
            }
        }
    }
}

//suspend fun <T> fetchByReferenceOrUseByValue(referenceURI: String?, valueObject: T, responseType: Class<T>, textResponse: Boolean = false): T {
//    var response: T = valueObject
//
//    if (!referenceURI.isNullOrEmpty()) {
//        try {
//            response = getWithUrl(referenceURI, responseType, textResponse)
//        } catch (e: IOException) {
//            println(e)
//            throw IOException("${SIOPErrors.REG_PASS_BY_REFERENCE_INCORRECTLY}: ${e.message}, URL: $referenceURI")
//        }
//    }
//
//    return response
//}
suspend fun <T> fetchByReferenceOrUseByValue(referenceURI: String?, valueObject: T?, responseType: Class<T>, textResponse: Boolean = false): T {
    // referenceURIとvalueObjectの少なくとも一方が非nullであることを保証
    if (referenceURI == null && valueObject == null) {
        throw IllegalArgumentException("Both referenceURI and valueObject cannot be null")
    }

    var response: T? = valueObject

    if (!referenceURI.isNullOrEmpty()) {
        try {
            response = getWithUrl(referenceURI, responseType, textResponse)
        } catch (e: IOException) {
            println(e)
            throw IOException("${SIOPErrors.REG_PASS_BY_REFERENCE_INCORRECTLY}: ${e.message}, URL: $referenceURI")
        }
    }

    // responseがnullの場合、例外をスロー
    return response ?: throw IllegalStateException("Response cannot be null")
}

