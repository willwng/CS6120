package rawTypes

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement

@Serializable
abstract class SourcedObject {
    val pos: Position? = null

    @SerialName("pos_end")
    val posEnd: Position? = null
    val src: String? = null
}

@Serializable
data class Position(val row: Int, val col: Int)

/** A Program is the top-level object */
@Serializable
data class Program(val functions: List<Function>) : SourcedObject()

/** A Function object represents a (first-order) procedure consisting of a sequence of instructions */
@Serializable
data class Function(
    val name: String,
    val args: List<Argument> = listOf(),
    val type: Type? = null,
    @SerialName("instrs") val instructions: List<InstructionOrLabel> = listOf(),
) : SourcedObject()


/** Arguments to functions */
@Serializable
data class Argument(val name: String, val type: Type) : SourcedObject()

interface InstructionOrLabel

/** A Label marks a position in an instruction sequence as a destination for control transfers */
@Serializable
data class Label(val label: String) : InstructionOrLabel, SourcedObject()

/** An Instruction represents a unit of computational work */
@Serializable
data class Instruction(
    val op: String,
    val dest: String? = null,
    val type: Type? = null,
    val args: List<String> = listOf(),
    @SerialName("funcs") val functions: List<String> = listOf(),
    val labels: List<Label> = listOf(),
    val value: Double? = null,
) : InstructionOrLabel, SourcedObject()


object TypeSerializer : JsonContentPolymorphicSerializer<Type>(Type::class) {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("type", PrimitiveKind.STRING)

    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Type> {
        TODO("Not yet implemented")
    }
}

@Serializable(with = TypeSerializer::class)
class Type(
    val baseType: Type?,
    val type: String
)