package trees

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*

object CookedInstructionSerializer : KSerializer<CookedInstruction> {
    override val descriptor: SerialDescriptor
        get() = TODO("Not yet implemented")

    override fun deserialize(decoder: Decoder): CookedInstruction {
        TODO("Not yet implemented")
    }

    override fun serialize(encoder: Encoder, value: CookedInstruction) {
        TODO("Not yet implemented")
    }

}

/** Helper serializer for operators */
object OperatorSerializer : KSerializer<Operator> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Operator")

    override fun deserialize(decoder: Decoder): Operator {
        throw (Error("Cooked operator should not be deserialized"))
    }

    override fun serialize(encoder: Encoder, value: Operator) {
        encoder.encodeString(value.toString().lowercase(Locale.getDefault()))
    }
}

/** Helper serializer for operators */
object ValueSerializer : KSerializer<Value> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Value")

    override fun deserialize(decoder: Decoder): Value {
        throw (Error("Cooked value should not be deserialized"))
    }

    override fun serialize(encoder: Encoder, value: Value) {
        when (value) {
            is IntValue -> encoder.encodeInt(value.value)
            is FloatValue -> encoder.encodeFloat(value.value)
            is BooleanValue -> encoder.encodeBoolean(value.value)
        }
    }
}
