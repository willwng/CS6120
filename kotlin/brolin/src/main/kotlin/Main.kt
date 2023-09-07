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
    val lvn = LVNClimber().applyToProgram(cookedProgram)
    assert(dceClimber.forwardDCEprogram(lvn) == dceClimber.reverseDCEprogram(lvn))
    val dce = dceClimber.applyToProgram(lvn)

    println(dce)

    println(Json.encodeToString(dce))
}
