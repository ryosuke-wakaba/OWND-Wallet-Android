package com.ownd_project.tw2023_wallet_android.vci

import com.ownd_project.tw2023_wallet_android.oid.openIdMetadataObjectMapper
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONException

fun Map<String, String>.toHeaders(): Headers {
    val builder = Headers.Builder()
    for ((key, value) in this) {
        builder.add(key, value)
    }
    return builder.build()
}

data class HttpResponseData<T>(
    val success: Boolean,
    val responseBody: T,
    // val responseText: String,
    val statusCode: Int,
    val origResponse: okhttp3.Response,
    val customError: String? = null
)

suspend fun <T> openIdFetch(
    url: String,
    responseType: Class<T>,
    body: RequestBody? = null,
    opts: FetchOptions? = null
): HttpResponseData<T> {
    val headers = mutableMapOf<String, String>()

    opts?.customHeaders?.let { headers.putAll(it) }

    opts?.bearerToken?.let {
        headers["Authorization"] = "Bearer $it"
    }

    val method = opts?.method ?: if (body != null) "POST" else "GET"
    val accept = opts?.accept ?: "application/json"
    headers["Accept"] = accept

    headers["Content-Type"]?.let { contentType ->
        if (opts?.contentType != null && opts.contentType != contentType) {
            throw Error("Mismatch in content-types from custom headers ($contentType) and supplied content type option (${opts.contentType})")
        }
    } ?: run {
        if (opts?.contentType != null) {
            headers["Content-Type"] = opts.contentType
        } else if (method != "GET") {
            headers["Content-Type"] = "application/json"
        }
    }

    val client = OkHttpClient()
    val request = Request.Builder()
        .url(url)
        .headers(headers.toHeaders()) // Convert Map to Headers
        .method(method, body)
        .build()

    println("START fetching url: $url")
    body?.let {
        println("Body:\r\n$body")
    }
    println("Headers:\r\n$headers")

    val origResponse = client.newCall(request).execute()
    val statusCode = origResponse.code

    // todo 完全一致を条件にしているけど前方一致にする必要があるかもしれないので関係するプロトコルを確認する
    val isJSONResponse =
        accept == "application/json" || origResponse.header("Content-Type") == "application/json"
    val success = origResponse.isSuccessful
    val responseText = origResponse.body?.string() ?: ""

    var customError: String? = null
    var responseBody: T = if (isJSONResponse) {
        try {
            openIdMetadataObjectMapper.readValue(responseText, responseType)
        } catch (e: JSONException) {
            customError = "JSON parse error"
        }

        if (responseText.isBlank()) {
            customError = "Blank JSON body"
        }
        // todo 2回目の実行は余分。型エラーをとりあえず回避しているので後で直す
        openIdMetadataObjectMapper.readValue(responseText, responseType)
    } else {
        responseText as T
    }

    println("${if (success) "success" else "error"} status: ${origResponse.code}, body:\r\n$responseText")
    if (customError != null && opts?.exceptionOnHttpErrorStatus == true) {
        throw Error(customError)
    }
    println("END fetching url: $url")

    // todo IFはおいおい調整する
    return HttpResponseData(
        success = success,
        responseBody = responseBody,
//        responseText = responseText,
        statusCode = statusCode,
        origResponse = origResponse,
        customError = customError
    )
}

suspend fun <T> getJson(
    url: String,
    responseType: Class<T>,
    opts: FetchOptions? = null
): HttpResponseData<T> {
    val mergedOptions = FetchOptions(
        method = "GET",
        bearerToken = opts?.bearerToken,
        contentType = opts?.contentType,
        accept = opts?.accept,
        customHeaders = opts?.customHeaders,
        exceptionOnHttpErrorStatus = opts?.exceptionOnHttpErrorStatus
    )
    return openIdFetch<T>(url, responseType, null, mergedOptions)
}

data class FetchOptions(
    val method: String? = null,
    val bearerToken: String? = null,
    val contentType: String? = null,
    val accept: String? = null,
    val customHeaders: Map<String, String>? = null,
    val exceptionOnHttpErrorStatus: Boolean? = null,
//    val deserializer: Deserializer<T>? = null
)
