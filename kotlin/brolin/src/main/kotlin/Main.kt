import climbers.BlockClimber
import climbers.DCEClimber
import climbers.LVNClimber
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

    val lvn = LVNClimber()
    val dce = DCEClimber()
    val blocks = BlockClimber().basicBlocker(cookedProgram)
    val lvnBlocks = lvn.lvnProgram(cookedProgram)
    val elimBlocks = lvnBlocks.map {
        it.map { block -> dce.reverseDCE(block) }
    }
    println(blocks)
    println(lvnBlocks)
    println(elimBlocks)

    assert(lvnBlocks.map { it.map { block -> dce.reverseDCE(block) } } == lvnBlocks.map {
        it.map { block -> dce.forwardDCE(block) }
    })
}
