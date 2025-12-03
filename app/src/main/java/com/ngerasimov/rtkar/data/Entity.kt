package com.ngerasimov.rtkar.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object EntityTypeSerializer : KSerializer<EntityType> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("EntityType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: EntityType) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): EntityType {
        return EntityType.valueOf(decoder.decodeString())
    }
}

@Serializable
data class Entity(
    @Serializable(with = EntityTypeSerializer::class)
    val type: EntityType,
    val title: String,
    val url: String
)
