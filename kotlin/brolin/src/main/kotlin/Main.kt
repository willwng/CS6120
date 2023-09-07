import climbers.DCEClimber
import climbers.LVNClimber
import kotlinx.serialization.encodeToString
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
//    println(cookedProgram)

    val dceClimber = DCEClimber()
    val lvnClimber = LVNClimber()
    val lvn = lvnClimber.applyToProgram(cookedProgram)
    val dce = dceClimber.applyToProgram(lvn)
    val lvn2 = lvnClimber.applyToProgram(dce)
    val dce2 = dceClimber.applyToProgram(lvn2)

//    assert(dceClimber.forwardLocalDCE(lvn) == dceClimber.reverseLocalDCE(lvn))

    val prettyJsonPrinter = Json { prettyPrint = true }
    println(prettyJsonPrinter.encodeToString(dce2))
}
