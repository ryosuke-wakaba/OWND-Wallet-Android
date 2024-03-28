package com.ownd_project.tw2023_wallet_android.vci

import com.ownd_project.tw2023_wallet_android.oid.AuthorizationServerMetadata

class MetadataClient {
    companion object {
        suspend fun retrieveAllMetadata(
            issuer: String,
            opts: Options? = Options(errorOnNotFound = false),
        ): EndpointMetadataResult {
            var tokenEndpoint: String? = null
            var credentialEndpoint: String? = null
            var authorizationEndpoint: String? = null
            var authorizationServerType: AuthorizationServerType = AuthorizationServerType.OID4VCI
            var authorizationServer: String = issuer
            val oid4vciResponse = retrieveOpenID4VCIServerMetadata(
                issuer,
                Options(false)
            ) // We will handle errors later, given we will also try other metadata locations
            val credentialIssuerMetadata = oid4vciResponse?.responseBody
            credentialIssuerMetadata?.let {
                // debug("Issuer $issuer OID4VCI well-known server metadata\r\n${JSON.stringify(it)}")
                credentialEndpoint = it.credentialEndpoint
                // authorizationServers の最初の要素を使用
                val az = it.authorizationServers?.firstOrNull()
                az?.let { authServer ->
                    authorizationServer = authServer
                    // ... その他の処理 ...
                }
            }
            // No specific OID4VCI endpoint. Either can be an OAuth2 AS or an OIDC IDP. Let's start with OIDC first
            // todo WellKnownEndpoints.OPENID_CONFIGURATION で実行する
            var response =
                retrieveOAuthServerMetadata(authorizationServer, Options(errorOnNotFound = false))
//            var response: HttpResponseData<AuthorizationServerMetadata> = retrieveWellKnown(
//                authorizationServer,
//                WellKnownEndpoints.OPENID_CONFIGURATION,
//                Options(errorOnNotFound = false)
//            )

            var authMetadata = response?.responseBody
            if (authMetadata != null) {
                // debug("Issuer $issuer has OpenID Connect Server metadata in well-known location")
                authorizationServerType = AuthorizationServerType.OIDC
            } else {
                // Now let's do OAuth2
//                response = retrieveWellKnown(
//                    authorizationServer,
//                    WellKnownEndpoints.OAUTH_AS,
//                    Options(errorOnNotFound = false)
//                )
//                authMetadata = response.successBody
                response =
                    retrieveOAuthServerMetadata(
                        authorizationServer,
                        Options(errorOnNotFound = false)
                    )
            }

            if (authMetadata == null) {
                // We will always throw an error, no matter whether the user provided the option not to, because this is bad.
                if (issuer != authorizationServer) {
                    throw Error("Issuer $issuer provided a separate authorization server $authorizationServer, but that server did not provide metadata")
                }
            } else {
                if (authorizationServerType == null) {
                    authorizationServerType = AuthorizationServerType.OAuth2
                }
                // debug("Issuer $issuer has $authorizationServerType Server metadata in well-known location")
                authMetadata.authorizationEndpoint?.let {
                    // todo may be changed block structure
                } ?: run {
                    throw Error("Authorization Server $authorizationServer did not provide an authorization_endpoint")
                }

                authMetadata.tokenEndpoint?.let {
                    // todo may be changed block structure
                    if (credentialIssuerMetadata != null) {
                        credentialIssuerMetadata.tokenEndpoint = it
                    }
                } ?: run {
                    throw Error("Authorization Server $authorizationServer did not provide a token_endpoint")
                }
            }
            return EndpointMetadataResult(
                credentialIssuerMetadata = credentialIssuerMetadata,
                authorizationServerMetadata = null
            )
        }

        suspend fun retrieveOAuthServerMetadata(
            issuerHost: String,
            opts: Options? = null,
        ): HttpResponseData<AuthorizationServerMetadata>? {
            return retrieveWellKnown(
                issuerHost,
                WellKnownEndpoints.OAUTH_AS,
                AuthorizationServerMetadata::class.java,
                Options(errorOnNotFound = opts?.errorOnNotFound ?: true)
            )
        }

        suspend fun retrieveOpenID4VCIServerMetadata(
            issuerHost: String,
            opts: Options? = null,
        ): HttpResponseData<CredentialIssuerMetadata>? {
            return retrieveWellKnown(
                issuerHost,
                WellKnownEndpoints.OPENID4VCI_ISSUER,
                CredentialIssuerMetadata::class.java,
                Options(errorOnNotFound = opts?.errorOnNotFound ?: true)
            )
        }
    }
}

data class Options(val errorOnNotFound: Boolean? = null)

suspend fun <T> retrieveWellKnown(
    host: String,
    endpointType: WellKnownEndpoints,
    responseType: Class<T>,
    opts: Options? = null,
): HttpResponseData<T> {
    val url = "${if (host.endsWith('/')) host.dropLast(1) else host}${endpointType.endpoint}"
    val result = getJson(
        url,
        responseType,
        FetchOptions(exceptionOnHttpErrorStatus = opts?.errorOnNotFound)
    )
    if (result.statusCode >= 400) {
        // We only get here when error on not found is false
        // debug("host $host with endpoint type $endpointType status: ${result.origResponse.status}, ${result.origResponse.statusText}")
    }
//    val mapper = jacksonObjectMapper()
//    val body = mapper.readValue(result.responseText, responseType)
    return result
}

