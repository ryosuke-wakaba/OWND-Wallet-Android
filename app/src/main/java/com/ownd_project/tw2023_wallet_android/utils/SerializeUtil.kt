package com.ownd_project.tw2023_wallet_android.utils

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import kotlin.reflect.KClass

class EnumDeserializer<T : Enum<T>>(private val enumClass: KClass<T>) : JsonDeserializer<T>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): T {
        val value = p.text.uppercase()
        return java.lang.Enum.valueOf(enumClass.java, value)
    }
}