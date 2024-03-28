package com.ownd_project.tw2023_wallet_android.utils

import java.net.URL
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection

object CertificateUtil {
    fun getCertificateInformation(url: String): CertificateInfo {
        val httpsUrl = URL(url)
        val connection = httpsUrl.openConnection() as HttpsURLConnection
        connection.connect()

        val certificates = connection.serverCertificates
        for (certificate in certificates) {
            if (certificate is X509Certificate) {
                val subjectDN = certificate.subjectX500Principal.name
                val subjectParts = subjectDN.split(",")
                var organization: String? = null
                var domain: String? = null
                var locality: String? = null
                var state: String? = null
                var country: String? = null
                var email: String? = null

                for (part in subjectParts) {
                    when {
                        part.trim().startsWith("CN=") -> domain = part.trim().removePrefix("CN=")
                        part.trim().startsWith("O=") -> organization = part.trim().removePrefix("O=")
                        part.trim().startsWith("L=") -> locality = part.trim().removePrefix("L=")
                        part.trim().startsWith("ST=") -> state = part.trim().removePrefix("ST=")
                        part.trim().startsWith("C=") -> country = part.trim().removePrefix("C=")
                        part.trim().startsWith("E=") -> email = part.trim().removePrefix("E=")
                    }
                }

                return CertificateInfo(domain, organization, locality, state, country, email)
            }
        }

        connection.disconnect()
        return CertificateInfo(null, null, null, null, null, null)
    }

    private fun x509Name2CertificateInfo(x509Name: String, issuer: CertificateInfo? = null): CertificateInfo {
        val subjectParts = x509Name.split(",")
        var organization: String? = null
        var domain: String? = null
        var locality: String? = null
        var state: String? = null
        var country: String? = null
        var street: String? = null
        var email: String? = null

        for (part in subjectParts) {
            when {
                part.trim().startsWith("CN=") -> domain = part.trim().removePrefix("CN=")
                part.trim().startsWith("O=") -> organization = part.trim().removePrefix("O=")
                part.trim().startsWith("L=") -> locality = part.trim().removePrefix("L=")
                part.trim().startsWith("ST=") -> state = part.trim().removePrefix("ST=")
                part.trim().startsWith("C=") -> country = part.trim().removePrefix("C=")
                part.trim().startsWith("STREET=") -> street = part.trim().removePrefix("STREET=")
                part.trim().startsWith("E=") -> email = part.trim().removePrefix("E=")
            }
        }
        return CertificateInfo(domain, organization, locality, state, country, street, email, issuer)
    }
    fun x509Certificate2CertificateInfo(certificate: X509Certificate): CertificateInfo {
        val issuerX500Principal = certificate.issuerX500Principal
        val subjectX500Principal = certificate.subjectX500Principal
        val issCert = x509Name2CertificateInfo(issuerX500Principal.name)
        return x509Name2CertificateInfo(subjectX500Principal.name, issCert)
    }
}

data class CertificateInfo(
    val domain: String?,
    val organization: String?,
    val locality: String?,
    val state: String?,
    val country: String?,
    val street: String? = null,
    val email: String? = null,
    val issuer: CertificateInfo? = null
) {
    fun getFullAddress(): String {
        val addressParts = listOf(locality, state, country).filterNotNull()
        return addressParts.joinToString(", ")
    }
}