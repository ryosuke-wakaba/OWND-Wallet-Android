package com.ownd_project.tw2023_wallet_android

import com.ownd_project.tw2023_wallet_android.utils.MetadataUtil
import com.ownd_project.tw2023_wallet_android.vci.CredentialIssuerMetadata
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.Assert
import org.junit.Test


class MetadataUtilTest {
    private val mapper = jacksonObjectMapper().apply {
        propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
    }
    @Test
    fun testExtractDisplayByClaimJwtVc() {
        val types = listOf("UniversityDegreeCredential")
        val json =
            this::class.java.classLoader?.getResource("credential_issuer_metadata_jwt_vc.json")
                ?.readText()
                ?: throw IllegalArgumentException("Cannot read test_data.json")
        val metadata = mapper.readValue(json, CredentialIssuerMetadata::class.java)
        val credentialsSupported = MetadataUtil.findMatchingCredentials("jwt_vc_json", types,  metadata)
        val displayMap = MetadataUtil.extractDisplayByClaim(credentialsSupported!!)
        Assert.assertNotNull(displayMap)
        Assert.assertEquals(3, displayMap.size)

        val display1 = displayMap["given_name"]!!
        Assert.assertEquals(2, display1.size)
        Assert.assertEquals("Given Name", display1[0].name)
        Assert.assertEquals("名", display1[1].name)

        val display2 = displayMap["last_name"]!!
        Assert.assertEquals(2, display2.size)
        Assert.assertEquals("Surname", display2[0].name)
        Assert.assertEquals("姓", display2[1].name)

        val display3 = displayMap["gpa"]!!
        Assert.assertEquals(1, display3.size)
        Assert.assertEquals("GPA", display3[0].name)
    }

    @Test
    fun testExtractDisplayByClaimSdJwt() {
        val types = listOf("EmployeeCredential")
        val json =
            this::class.java.classLoader?.getResource("credential_issuer_metadata_sd_jwt.json")
                ?.readText()
                ?: throw IllegalArgumentException("Cannot read test_data.json")
        val metadata = mapper.readValue(json, CredentialIssuerMetadata::class.java)
        val credentialsSupported = MetadataUtil.findMatchingCredentials("vc+sd-jwt", types,  metadata)
        val displayMap = MetadataUtil.extractDisplayByClaim(credentialsSupported!!)
        // val displayMap = MetadataUtil.extractDisplayByClaim(types, metaData)
        Assert.assertNotNull(displayMap)
        Assert.assertEquals(5, displayMap.size)

        val display1 = displayMap["employee_no"]!!
        Assert.assertEquals(2, display1.size)
        Assert.assertEquals("Employee No", display1[0].name)
        Assert.assertEquals("社員番号", display1[1].name)

        val display2 = displayMap["given_name"]!!
        Assert.assertEquals(2, display2.size)
        Assert.assertEquals("Given Name", display2[0].name)
        Assert.assertEquals("名", display2[1].name)

        val display3 = displayMap["family_name"]!!
        Assert.assertEquals(2, display3.size)
        Assert.assertEquals("Family Name", display3[0].name)
        Assert.assertEquals("姓", display3[1].name)

        val display4 = displayMap["gender"]!!
        Assert.assertEquals(2, display4.size)
        Assert.assertEquals("Gender", display4[0].name)
        Assert.assertEquals("性別", display4[1].name)

        val display5 = displayMap["division"]!!
        Assert.assertEquals(2, display5.size)
        Assert.assertEquals("Division", display5[0].name)
        Assert.assertEquals("部署", display5[1].name)
    }
    @Test
    fun testDeserializeDisplayMap() {
        val types = listOf("UniversityDegreeCredential")
        val json =
            this::class.java.classLoader?.getResource("credential_issuer_metadata_jwt_vc.json")
                ?.readText()
                ?: throw IllegalArgumentException("Cannot read test_data.json")
        val metadata = mapper.readValue(json, CredentialIssuerMetadata::class.java)
        val credentialsSupported = MetadataUtil.findMatchingCredentials("jwt_vc_json", types,  metadata)
        val displayMap = MetadataUtil.extractDisplayByClaim(credentialsSupported!!)

        // 一度シリアライズ
        val serialized = MetadataUtil.serializeDisplayByClaimMap(displayMap)
        // デシリアライズ
        val deserialized = MetadataUtil.deserializeDisplayByClaimMap(serialized)
        Assert.assertNotNull(deserialized)
        Assert.assertEquals(3, displayMap.size)

        val display1 = deserialized["given_name"]!!
        Assert.assertEquals(2, display1.size)
        Assert.assertEquals("Given Name", display1[0].name)
        Assert.assertEquals("名", display1[1].name)

        val display2 = deserialized["last_name"]!!
        Assert.assertEquals(2, display2.size)
        Assert.assertEquals("Surname", display2[0].name)
        Assert.assertEquals("姓", display2[1].name)

        val display3 = deserialized["gpa"]!!
        Assert.assertEquals(1, display3.size)
        Assert.assertEquals("GPA", display3[0].name)
    }
}