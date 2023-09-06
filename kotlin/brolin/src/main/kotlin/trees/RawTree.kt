package trees

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/** The tree constructed directly from JSON. */

/** A Program is the top-level object */
@Serializable
data class RawProgram(@SerialName("functions") val rawFunctions: List<RawFunction>) : SourcedObject()

/** A Function object represents a (first-order) procedure consisting of a sequence of instructions */
@Serializable
data class RawFunction(
    val name: String,
    val args: List<Argument> = listOf(),
    val type: Type? = null,
    @SerialName("instrs") val instructions: List<RawInstructionOrLabel> = listOf(),
) : SourcedObject()

@Serializable(with = RawInstructionOrLabelSerializer::class)
interface RawInstructionOrLabel

/** An Instruction represents a unit of computational work */
@Serializable
data class RawInstruction(
    val op: String,
    val dest: String? = null,
    val type: Type? = null,
    val args: List<String> = listOf(),
    @SerialName("funcs") val functions: List<String> = listOf(),
    val labels: List<String> = listOf(),
    @SerialName("value") val jsonValue: JsonPrimitive? = null,
) : RawInstructionOrLabel, SourcedObject()

/** A Label marks a position in an instruction sequence as a destination for control transfers */
@Serializable
data class RawLabel(val label: String) : RawInstructionOrLabel, SourcedObject()

/** A helper serializer to distinguish between Instructions and Labels **/
object RawInstructionOrLabelSerializer :
    JsonContentPolymorphicSerializer<RawInstructionOrLabel>(RawInstructionOrLabel::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<RawInstructionOrLabel> = when {
        "label" in element.jsonObject -> RawLabel.serializer()
        else -> RawInstruction.serializer()
    }
}
