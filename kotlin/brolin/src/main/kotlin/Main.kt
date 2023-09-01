
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import rawTypes.Instruction
import rawTypes.Program

fun main() {
    val input = generateSequence(::readLine).joinToString("\n")
    val jsonElement = Json.parseToJsonElement(input)
    val program = Json.decodeFromJsonElement<Program>(jsonElement)

    program.functions.forEach { it ->
        it.instructions.forEach {
            if(it is Instruction)
                println(it.pos?.col)
        }
    }
    print(program)

}