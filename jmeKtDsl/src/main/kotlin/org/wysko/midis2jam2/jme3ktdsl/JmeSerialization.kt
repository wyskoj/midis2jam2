@file:OptIn(ExperimentalSerializationApi::class)

package org.wysko.midis2jam2.jme3ktdsl

import com.jme3.math.Vector3f
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.FloatArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object Vector3fSerializer : KSerializer<Vector3f> {
    private val delegateSerializer = FloatArraySerializer()

    override val descriptor: SerialDescriptor = SerialDescriptor("Vector3f", delegateSerializer.descriptor)

    override fun serialize(encoder: Encoder, value: Vector3f) {
        encoder.encodeSerializableValue(delegateSerializer, value.toArray(null))
    }

    override fun deserialize(decoder: Decoder): Vector3f {
        val array = decoder.decodeSerializableValue(delegateSerializer)
        return Vector3f(array[0], array[1], array[2])
    }
}
