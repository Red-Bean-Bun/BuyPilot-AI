package com.buypilot.core.model.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

object FlexibleStringListSerializer : KSerializer<List<String>> {
    override val descriptor: SerialDescriptor = ListSerializer(String.serializer()).descriptor

    override fun deserialize(decoder: Decoder): List<String> {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return listOf(decoder.decodeString())

        return when (val element: JsonElement = jsonDecoder.decodeJsonElement()) {
            is JsonArray -> element.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            is JsonPrimitive -> element.contentOrNull?.let(::listOf).orEmpty()
            else -> emptyList()
        }
    }

    override fun serialize(encoder: Encoder, value: List<String>) {
        encoder.encodeSerializableValue(ListSerializer(String.serializer()), value)
    }
}
