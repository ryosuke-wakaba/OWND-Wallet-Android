package com.ownd_project.tw2023_wallet_android.oid

import com.ownd_project.tw2023_wallet_android.utils.EnumDeserializer
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

enum class LimitDisclosure {
    REQUIRED,
    PREFERRED
}

enum class Rule {
    PICK
}

data class PresentationDefinition(
    val id: String,
    val inputDescriptors: List<InputDescriptor>,
    val submissionRequirements: List<SubmissionRequirement>?,
    val name: String?,
    val purpose: String?
)

data class InputDescriptor(
    val id: String,
    val name: String?,
    val purpose: String?,
    val format: Map<String, Any>?,
    val group: List<String>?,
    val constraints: InputDescriptorConstraints
)

data class InputDescriptorConstraints(
    val limitDisclosure: LimitDisclosure?,
    val fields: List<Field>?
)

data class Field(
    val path: List<String>,
    val filter: Map<String, Any>?,
    val optional: Boolean?
)

data class SubmissionRequirement(
    val name: String?,
    val rule: Rule,
    val count: Int?,
    val from: String
)

data class Path(
    val format: String,
    val path: String,
)

// https://identity.foundation/presentation-exchange/spec/v2.0.0/#presentation-submission
data class DescriptorMap(
    val id: String, // The value of this property MUST be a string that matches the id property of the Input Descriptor in the Presentation Definition that this Presentation Submission is related to.
    val format: String, // The value of this property MUST be a string that matches one of the Claim Format Designation. This denotes the data format of the Claim.
    val path: String, //  The value of this property MUST be a JSONPath string expression. The path property indicates the Claim submitted in relation to the identified Input Descriptor, when executed against the top-level of the object the Presentation Submission is embedded within.
    val pathNested: Path? = null,
)

data class PresentationSubmission(
    val id: String, // The value of this property MUST be a unique identifier, such as a UUID.
    val definitionId: String, // The value of this property MUST be the id value of a valid Presentation Definition.
    val descriptorMap: List<DescriptorMap>,
)

fun deserializePresentationDefinition(json: String): PresentationDefinition {
    val objectMapper: ObjectMapper = jacksonObjectMapper().apply {
        propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
        configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true)
        val module = SimpleModule().apply {
            addDeserializer(LimitDisclosure::class.java, EnumDeserializer(LimitDisclosure::class))
            addDeserializer(Rule::class.java, EnumDeserializer(Rule::class))
        }
        registerModule(module)
    }
    return objectMapper.readValue(json, PresentationDefinition::class.java)
}

fun convertPresentationDefinition(map: Map<String, Any>): PresentationDefinition {
    val objectMapper: ObjectMapper = jacksonObjectMapper().apply {
        propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
        val module = SimpleModule().apply {
            addDeserializer(LimitDisclosure::class.java, EnumDeserializer(LimitDisclosure::class))
            addDeserializer(Rule::class.java, EnumDeserializer(Rule::class))
        }
        registerModule(module)
    }
    return objectMapper.convertValue(map, PresentationDefinition::class.java)
}

// todo Request Objectのプロパティとしてデシリアライズする場合のカスタムでシリアライザ定義(うまく動かないけど必要ないので後回し。ただ今後他のプロパティで類似の問題に直面する可能性はある)
//class PresentationDefinitionDeserializer : JsonDeserializer<PresentationDefinition>() {
//    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): PresentationDefinition {
//        // JsonParserからJSON文字列を取得
//        val jsonNode = p.codec.readTree<JsonNode>(p)
//        val json = jsonNode.toString()
//
//        // 独自のObjectMapperを設定
//        val objectMapper: ObjectMapper = jacksonObjectMapper().apply {
//            propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
//            configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true)
//            val module = SimpleModule().apply {
//                addDeserializer(LimitDisclosure::class.java, EnumDeserializer(LimitDisclosure::class))
//                addDeserializer(Rule::class.java, EnumDeserializer(Rule::class))
//            }
//            registerModule(module)
//        }
//
//        return objectMapper.readValue(json, PresentationDefinition::class.java)
//    }
//}
