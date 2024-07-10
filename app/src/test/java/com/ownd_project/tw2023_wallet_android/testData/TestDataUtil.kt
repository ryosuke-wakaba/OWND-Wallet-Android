package com.ownd_project.tw2023_wallet_android.testData

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.authlete.sd.Disclosure
import com.authlete.sd.SDObjectBuilder
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.protobuf.Timestamp
import com.ownd_project.tw2023_wallet_android.datastore.Claim
import com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistories
import com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistory
import com.ownd_project.tw2023_wallet_android.utils.generateEcKeyPair
import com.ownd_project.tw2023_wallet_android.utils.generateRsaKeyPair
import com.ownd_project.tw2023_wallet_android.utils.publicKeyToJwk
import com.ownd_project.tw2023_wallet_android.vci.CredentialIssuerMetadata
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPrivateKey
import java.util.UUID

object TestDataUtil {
    private val mapper = jacksonObjectMapper().apply {
        propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
    }
    private fun generateVCJwtCredential(): String {
        val keyPair = generateRsaKeyPair()
        val algorithm = when (keyPair.private) {
            is RSAPrivateKey -> Algorithm.RSA256(null, keyPair.private as RSAPrivateKey)
            is ECPrivateKey -> Algorithm.ECDSA256(null, keyPair.private as ECPrivateKey)
            else -> throw IllegalArgumentException("未サポートの秘密鍵のタイプです。")
        }

        val x5cMap = mapOf(
            "x5c" to listOf(
                "MIIC1jCCAb6gAwIBAgIUWwvaSTr281Wz5GL+B3l+VN5mXZowDQYJKoZIhvcNAQELBQAwJTEjMCEGA1UEAwwaVGVzdCBDZXJ0aWZpY2F0ZSBBdXRob3JpdHkwHhcNMjMxMTA5MDE1NTMzWhcNMjQxMTA4MDE1NTMzWjBqMQswCQYDVQQGEwJKUDESMBAGA1UECAwJ5p2x5Lqs6YO9MRIwEAYDVQQHDAnmlrDlrr/ljLoxHTAbBgNVBAoMFOagquW8j+S8muekvkRhdGFTaWduMRQwEgYDVQQDDAtkYXRhc2lnbi5qcDBWMBAGByqGSM49AgEGBSuBBAAKA0IABM25nwomPvVuvGs8ggeU6vu32d++B7yby1b5GBTnG+hRqwXg/LYLX4FWsCHmeqGg1Ug050HNLs9YPj2GZTJkYQKjgYYwgYMwFgYDVR0RBA8wDYILZGF0YXNpZ24uanAwHQYDVR0OBBYEFEUSeJM8KSqx53G4eU1B4/1njAHYMEoGA1UdIwRDMEGhKaQnMCUxIzAhBgNVBAMMGlRlc3QgQ2VydGlmaWNhdGUgQXV0aG9yaXR5ghQtwA/xs2lqo1SEBWNXmmeEhbjuqzANBgkqhkiG9w0BAQsFAAOCAQEAHdR3uutoC+RQ750McLz9eFtzEruYkGU0aCnCMzpMJ3HMW63pOKFVVhpNxirz+pm/FpDwAcLT1jgKvdbH4cai8oTfd84GuEldxOyNYVrIybkJOJla1tZloW6WjGfKVY8YAaKwHVQBcwa/std18j3g7CA/h9V4wKUtPYLKNobAOk/CSD2BCHSdt49MRdkgyigjxh654qk/DIsrKz6VUR7/UPvuGuwPtZhhIs/89OoNZ2yvMKCffMGHLL9TKeGGVVf9ozVxV/lNbneXmGD2kvZ1zFbRwaYCmw4DcIAYLij29nahboY80hdt86HZe42esQSBDBzaTyA4EvXH5l5AUtSHmg==",
                "MIIC0TCCAbkCFC3AD/GzaWqjVIQFY1eaZ4SFuO6rMA0GCSqGSIb3DQEBCwUAMCUxIzAhBgNVBAMMGlRlc3QgQ2VydGlmaWNhdGUgQXV0aG9yaXR5MB4XDTIzMTEwOTAxNDk0NVoXDTI0MTEwODAxNDk0NVowJTEjMCEGA1UEAwwaVGVzdCBDZXJ0aWZpY2F0ZSBBdXRob3JpdHkwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDCj2g1m7YQZb1LMOlMy2zrOCg9cAEzrK7rctymFFd9r77JMOi1c3nzIm6ZWemwSNxGY2yUSB+CNHJDQ+W9vO2M/9FFvuKxMfVCDDEBV1w9rkNdjIGcvhLA6VjhxoAN0X4VRm8pzW7KKsr9PMr2HZVbqorLTnTkC5aHhoqVcLe/OFnm4NzU02B9xecaaoqajPAXHltFtD+DVKE6mQuRtD8KOIRhPfH9UuorOYV2emLKw1b7MFM5O8IETcKD2tazcHRQbFio/6VSYXikBzHI9bttfd2qmmTmTOLIhFsgTbnZqlGc9mYb3HA2mSynXg0/NzdO4MsF/bhFmwSvCxBPCYmRAgMBAAEwDQYJKoZIhvcNAQELBQADggEBALzHBevOxbVXHD8iHdhjLqbUDiZkOO84UNIFOIceq4J685qxJaJZ65SJV6f3gwbRa3BCeJsLGSktcKJ5kcN8S3sFABmMoYSBn/KLG2672oOCmdaZV/notuhx2pwqrTtN/5Zw14UlA78bH8suCzHKpdSbGR7rZQmXyAkIl2VUGOFCklWKylsXFYcoT/jwggTlf26zwbzlkjIFFY+sXr0n8gCal9o40eHaNWAskNdA2Cviqx5FIlto7/OK0lrKiJxkNI8EcBBQRe0mDnzj6QXxYLOqihC1owQukUcNFHdya4lvVX1f1hx2RDLYG2qgvZePzf6GyRH0mUS9asp6cBR8Asc="
            )
        )
        val type = listOf<String>("VerifiableCredential", "UniversityDegreeCredential")
        val context = listOf<String>(
            "https://www.w3.org/ns/credentials/v2",
            "https://www.w3.org/ns/credentials/examples/v2"
        )
        return JWT.create()
            .withIssuer("https://event.company/issuers/565049") // iss
            .withJWTId("http://event.company/credentials/3732") // jti
            .withSubject("did:example:ebfeb1f712ebc6f1c276e12ec21") // sub
            .withClaim(
                "vc", mapOf(
                    "@context" to context,
                    "id" to "http://event.company/credentials/3732",
                    "type" to type,
                    "issuer" to "https://event.company/issuers/565049",
                    "validFrom" to "2010-01-01T00:00:00Z",
                    "credentialSubject" to mapOf(
                        "id" to "did:example:ebfeb1f712ebc6f1c276e12ec21",
                        "name" to "Sample Business Event",
                        "location" to "Shinjuku Tokyo",
                        "organizer" to "Sample Event Company"
                    )
                )
            )
            .withHeader(x5cMap)
            .sign(algorithm)
    }

    val vcJwtCredential = generateVCJwtCredential()
    val jwtVcMetadata =
        this::class.java.classLoader!!.getResource("credential_issuer_metadata_jwt_vc.json")
            ?.readText()
            ?: throw IllegalArgumentException("Cannot read credential_issuer_metadata_jwt_vc.json")

    // スネークケースのオリジナルを一度シリアライズしてからデシリアライズしてキャメルケースで保存させる
    val jwtVcMetadataStr =
        jacksonObjectMapper().writeValueAsString(mapper.readValue(jwtVcMetadata, CredentialIssuerMetadata::class.java))

    val jwtVcJsonCredentialData: com.ownd_project.tw2023_wallet_android.datastore.CredentialData
        get() {
            return com.ownd_project.tw2023_wallet_android.datastore.CredentialData.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setFormat("jwt_vc_json")
                .setCredential(vcJwtCredential)
                .setCNonce("test_CNonce")
                .setCNonceExpiresIn(86400)
                .setIss("https://event.company/issuers/565049")
                .setIat(86400L)
                .setExp(86400L)
                .setType("UniversityDegreeCredential")
                .setAccessToken("test_accessToken")
                .setCredentialIssuerMetadata(jwtVcMetadataStr)
                .build()
        }

    private fun generateSdJwtCredential(): String {
        val ecKeyPair = generateEcKeyPair()
        val algorithm =
            Algorithm.ECDSA256(ecKeyPair.public as ECPublicKey, ecKeyPair.private as ECPrivateKey?)
        val disclosures =
            listOf<Disclosure>(Disclosure("claim1", "value1"), Disclosure("claim2", "value2"))
        val builder = SDObjectBuilder()
        disclosures.forEach { it ->
            builder.putSDClaim(it)
        }
        val claims = builder.build()

        val jwk = publicKeyToJwk(ecKeyPair.public)
        val cnf = mapOf("jwk" to jwk)
        val issuerSignedJwt = JWT.create().withIssuer("https://client.example.org/cb")
            .withAudience("https://server.example.com")
            .withClaim("cnf", cnf)
            .withClaim("vct", "EmployeeCredential")
            .withClaim("_sd", (claims["_sd"] as List<*>))
            .sign(algorithm)
        return "$issuerSignedJwt~${disclosures.joinToString("~") { it.disclosure }}"
    }

    val sdJwtCredential = generateSdJwtCredential()
    val sdJwtMetadata =
        this::class.java.classLoader!!.getResource("credential_issuer_metadata_sd_jwt.json")
            ?.readText()
            ?: throw IllegalArgumentException("Cannot read credential_issuer_metadata_sd_jwt.json")

    // スネークケースのオリジナルを一度シリアライズしてからデシリアライズしてキャメルケースで保存させる
    val sdJwtMetadataStr =
        jacksonObjectMapper().writeValueAsString(mapper.readValue(sdJwtMetadata, CredentialIssuerMetadata::class.java))
    val vcSdJwtCredentialData: com.ownd_project.tw2023_wallet_android.datastore.CredentialData
        get() {
            // ... vc+sd-jwt 形式の CredentialData の生成

            return com.ownd_project.tw2023_wallet_android.datastore.CredentialData.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setFormat("vc+sd-jwt")
                .setCredential(sdJwtCredential)
                .setCNonce("test_CNonce")
                .setCNonceExpiresIn(86400)
                .setIss("https://event.company/issuers/565049")
                .setIat(86400L)
                .setExp(86400L)
                .setType("EmployeeCredential")
                .setAccessToken("test_accessToken")
                .setCredentialIssuerMetadata(sdJwtMetadataStr)
                .build()
        }

    fun generateHistories(): CredentialSharingHistories {

        val builder1 = CredentialSharingHistory.newBuilder()
            .setRp("Sample RP1-ID")
            .setRpName("RP1 Name")
            .setAccountIndex(1)
            .setCreatedAt(
                Timestamp.newBuilder()
                    .setSeconds((System.currentTimeMillis() - 100000000) / 1000)
                    .setNanos(0)
                    .build()
            )
            .setRpLocation("Sinjuku-ku, Tokyo JP")
            .setRpLogoUrl("https://www.ownd-project.com/img/logo_only.png")
            .setCredentialID("CredentialID")

        listOf("氏名", "年齢", "住所").forEach { claim ->
            builder1.addClaims(
                Claim.newBuilder()
                    .setName(claim)
                    .setValue("value")
                    .build()
            )
        }

        val history1 = builder1.build()

        val builder2 = CredentialSharingHistory.newBuilder()
            .setRp("Sample RP2-ID")
            .setRpName("RP2 Name")
            .setAccountIndex(1)
            .setCreatedAt(
                Timestamp.newBuilder()
                    .setSeconds(System.currentTimeMillis() / 1000)
                    .setNanos(0)
                    .build()
            )
            .setCredentialID("CredentialID")

        listOf("前年年収").forEach { claim ->
            builder2.addClaims(
                Claim.newBuilder()
                    .setName(claim)
                    .setValue("value")
                    .setPurpose("住宅ローン審査の為に提供しました")
                    .build()
            )
        }

        val history2 = builder2.build()


        val builder3 = CredentialSharingHistory.newBuilder()
            .setRp("Sample RP1-ID")
            .setRpName("RP1 Name")
            .setAccountIndex(1)
            .setCreatedAt(
                Timestamp.newBuilder()
                    .setSeconds(System.currentTimeMillis() / 1000)
                    .setNanos(0)
                    .build()
            )
            .setRpLocation("Sinjuku-ku, Tokyo JP")
            .setRpLogoUrl("https://www.ownd-project.com/img/logo_only.png")
            .setCredentialID("CredentialID")

        listOf("claim1", "claim2", "claim3").forEach { claim ->
            builder3.addClaims(
                Claim.newBuilder()
                    .setName(claim)
                    .setValue("value")
                    .build()
            )
        }

        val history3 = builder3.build()


        val builder4 = CredentialSharingHistory.newBuilder()
            .setRp("Sample RP2-ID")
            .setRpName("RP2 Name")
            .setAccountIndex(1)
            .setCreatedAt(
                Timestamp.newBuilder()
                    .setSeconds((System.currentTimeMillis() + 100000000) / 1000)
                    .setNanos(0)
                    .build()
            )
            .setCredentialID("CredentialID")

        listOf("claim1", "claim2", "claim3").forEach { claim ->
            builder4.addClaims(
                Claim.newBuilder()
                    .setName(claim)
                    .setValue("value")
                    .build()
            )
        }

        val history4 = builder4.build()

        val historiesList = listOf(history1, history2, history3, history4)

        val histories = CredentialSharingHistories.newBuilder()
            .addAllItems(historiesList)
            .build()

        return histories
    }
}