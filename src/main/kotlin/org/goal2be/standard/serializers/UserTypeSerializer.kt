package org.goal2be.standard.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.goal2be.standard.auth.user.UserType

class UserTypeSerializer : KSerializer<UserType> {

    override val descriptor = PrimitiveSerialDescriptor("UserType", PrimitiveKind.SHORT)

    override fun serialize(encoder: Encoder, value: UserType) {
        encoder.encodeShort(value.value)
    }

    override fun deserialize(decoder: Decoder): UserType {
        return UserType.fromShort(decoder.decodeShort())
    }
}