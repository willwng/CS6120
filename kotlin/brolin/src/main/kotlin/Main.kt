import climbers.DCEClimber
import climbers.LVNClimber
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import trees.RawInstruction
import trees.RawLabel
import trees.RawProgram
import trees.TreeCooker
import util.CFGProgram
import util.GraphGenerator
import util.ReachingDefs
import java.io.FileOutputStream
import java.io.PrintWriter

fun main() {
    val input = generateSequence(::readLine).joinToString("\n")
    val jsonElement = Json.parseToJsonElement(input)
    val rawProgram = Json.decodeFromJsonElement<RawProgram>(jsonElement)
    val cookedProgram = TreeCooker.cookProgram(rawProgram)
    val prettyJsonPrinter = Json { prettyPrint = true }
//    println(prettyJsonPrinter.encodeToString(cookedProgram))
//    println(Json.encodeToString(rawProgram))
//    println(cookedProgram)

//    val lvn = LVNClimber.applyToProgram(cookedProgram)
//    val dce = DCEClimber.applyToProgram(lvn)
//    val lvn2 = LVNClimber.applyToProgram(dce)
//    val dce2 = DCEClimber.applyToProgram(lvn2)

//    assert(dceClimber.forwardLocalDCE(lvn) == dceClimber.reverseLocalDCE(lvn))
//    val cfg = CFGProgram.of(cookedProgram)
//    cfg.graphs.forEach {
//        val out = PrintWriter(FileOutputStream("${it.function.name}.dot"))
//        GraphGenerator.createGraphOutput(it).writeToFile(out)
//        out.close()
//    }
    println(ReachingDefs.analyze(cookedProgram))

}
