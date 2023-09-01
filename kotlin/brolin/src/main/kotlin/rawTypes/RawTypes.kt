package rawTypes

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

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
    val type: String? = null,
    @SerialName("instrs") val instructions: List<InstructionOrLabel> = listOf(),
) : SourcedObject()


/** Arguments to functions */
@Serializable
data class Argument(val name: String, val type: String) : SourcedObject()

@Serializable(with = InstructionOrLabelSerializer::class)
interface InstructionOrLabel

/** A Label marks a position in an instruction sequence as a destination for control transfers */
@Serializable
data class Label(val label: String) : InstructionOrLabel, SourcedObject()

/** An Instruction represents a unit of computational work */
@Serializable
data class Instruction(
    val op: String,
    val dest: String? = null,
    val type: String? = null,
    val args: List<String> = listOf(),
    @SerialName("funcs") val functions: List<String> = listOf(),
    val labels: List<String> = listOf(),
    val value: Double? = null,
) : InstructionOrLabel, SourcedObject()

/** A helper serializer to distinguish between Instructions and Labels **/
object InstructionOrLabelSerializer : JsonContentPolymorphicSerializer<InstructionOrLabel>(InstructionOrLabel::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<InstructionOrLabel> = when {
        "label" in element.jsonObject -> Label.serializer()
        else -> Instruction.serializer()
    }
}

// TODO: Handle Parametrized types
class Type(
    val baseType: Type?,
    val type: String
)