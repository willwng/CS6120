
import climbers.DCEClimber
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import trees.RawProgram
import trees.TreeCooker

fun main() {
    val input = generateSequence(::readLine).joinToString("\n")
    val jsonElement = Json.parseToJsonElement(input)
    val rawProgram = Json.decodeFromJsonElement<RawProgram>(jsonElement)
    val treeCooker = TreeCooker()
    val cookedProgram = treeCooker.cookProgram(rawProgram)
//    println(Json.encodeToString(rawProgram))

    println(cookedProgram)
    val trivialDCE = DCEClimber()
    val blocks = trivialDCE.trivialDCEProgram(cookedProgram)
    blocks.forEach {
        it.forEach {
            println(blocks)
        }
    }
}