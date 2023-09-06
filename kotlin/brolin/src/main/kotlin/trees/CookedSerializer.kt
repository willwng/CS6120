package trees

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import java.util.*


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

object CookedInstructionOrLabelSerializer : KSerializer<CookedInstructionOrLabel> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("InstructionOrLabel")

    override fun deserialize(decoder: Decoder): CookedInstructionOrLabel {
        throw (Error("Cooked instruction should not be deserialized"))
    }

    override fun serialize(encoder: Encoder, value: CookedInstructionOrLabel) {
        when (value) {
            is CookedInstruction -> encoder.encodeSerializableValue(serializer = CookedInstruction.serializer(), value)
            is CookedLabel -> encoder.encodeSerializableValue(serializer = CookedLabel.serializer(), value)
        }
    }
}

object CookedInstructionSerializer : KSerializer<CookedInstruction> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Instruction")

    override fun deserialize(decoder: Decoder): CookedInstruction {
        throw (Error("Cooked instruction should not be deserialized"))
    }

    override fun serialize(encoder: Encoder, value: CookedInstruction) {
        when (value) {
            is ConstantInstruction -> encoder.encodeSerializableValue(
                serializer = ConstantInstruction.serializer(),
                value = value
            )

            is EffectOperation -> encoder.encodeSerializableValue(
                serializer = EffectOperation.serializer(),
                value = value
            )

            is ValueOperation -> {}
        }
    }
}

object ConstantInstructionSerializer : KSerializer<ConstantInstruction> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ConstantInstruction") {
        element<Operator>("op")
        element<String>("dest")
        element<Type>("type")
        element<Value>("value")
    }

    override fun deserialize(decoder: Decoder): ConstantInstruction {
        throw (Error("Cooked constant instruction should not be deserialized"))
    }

    override fun serialize(encoder: Encoder, value: ConstantInstruction) {
        encoder.encodeStructure(descriptor = descriptor) {
            encodeSerializableElement(
                descriptor = descriptor,
                serializer = Operator.serializer(),
                index = 0,
                value = value.op
            )
            encodeStringElement(descriptor = descriptor, index = 1, value = value.dest)
            encodeSerializableElement(
                descriptor = descriptor,
                serializer = Type.serializer(),
                index = 2,
                value = value.type
            )
            encodeSerializableElement(
                descriptor = descriptor,
                serializer = Value.serializer(),
                index = 3,
                value = value.value
            )
        }
    }
}

object EffectOperationSerializer : KSerializer<EffectOperation> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ConstantInstruction") {
        element<Operator>("op")
        element<List<String>>("args")
        element<List<String>>("funcs")
        element<List<String>>("labels")
    }

    override fun deserialize(decoder: Decoder): EffectOperation {
        throw (Error("Cooked effect operation should not be deserialized"))
    }

    override fun serialize(encoder: Encoder, value: EffectOperation) {
        encoder.encodeStructure(descriptor = descriptor) {
            encodeSerializableElement(
                descriptor = descriptor,
                serializer = Operator.serializer(),
                index = 0,
                value = value.op
            )

        }

    }
}
