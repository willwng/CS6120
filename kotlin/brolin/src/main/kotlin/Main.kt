import dataflow.ConstantPropAnalysis
import dataflow.DominatorsAnalysis
import dataflow.LiveVariablesAnalysis
import dataflow.LiveVariablesAnalysis.LiveVariablesBeta.LiveVars
import dataflow.ReachingDefsAnalysis
import dataflow.ReachingDefsAnalysis.ReachingDefsBeta.ReachingDefs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import trees.RawProgram
import trees.TreeCooker
import util.CFGProgram
import util.GraphGenerator
import java.io.FileOutputStream
import java.io.PrintWriter

fun main() {
    val input = generateSequence(::readLine).joinToString("\n")
    val jsonElement = Json.parseToJsonElement(input)
    val rawProgram = Json.decodeFromJsonElement<RawProgram>(jsonElement)
    val cookedProgram = TreeCooker.cookProgram(rawProgram)
//    val prettyJsonPrinter = Json { prettyPrint = true }
//    println(prettyJsonPrinter.encodeToString(cookedProgram))
//    println(Json.encodeToString(rawProgram))
//    println(cookedProgram)

//    val lvn = LVNClimber.applyToProgram(cookedProgram)
//    val dce = DCEClimber.applyToProgram(lvn)
//    val lvn2 = LVNClimber.applyToProgram(dce)
//    val dce2 = DCEClimber.applyToProgram(lvn2)

//    assert(dceClimber.forwardLocalDCE(lvn) == dceClimber.reverseLocalDCE(lvn))
    val cfgProgram = CFGProgram.of(cookedProgram)
    val reachingDefsAnalysis = ReachingDefsAnalysis.analyze(cfgProgram)
    val liveVarsAnalysis = LiveVariablesAnalysis.analyze(cfgProgram)
    val constantPropAnalysis = ConstantPropAnalysis.analyze(cfgProgram)
    cfgProgram.graphs.forEach {
        val out = PrintWriter(FileOutputStream("${it.function.name}.dot"))
        GraphGenerator.createGraphOutput<ReachingDefs>(
            it,
            reachingDefsAnalysis[it.function.name]
        ).writeToFile(out)
        out.close()
    }
    cfgProgram.graphs.forEach {
        val out = PrintWriter(FileOutputStream("${it.function.name}-live.dot"))
        GraphGenerator.createGraphOutput<LiveVars>(it, liveVarsAnalysis[it.function.name]).writeToFile(out)
        out.close()
    }
    cfgProgram.graphs.forEach {
        val out = PrintWriter(FileOutputStream("${it.function.name}-cp.dot"))
        GraphGenerator.createGraphOutput(
            it,
            constantPropAnalysis[it.function.name]
        ).writeToFile(out)
        out.close()
    }

    val dominatorAnalysis = DominatorsAnalysis.analyze(cfgProgram)
    cfgProgram.graphs.forEach {
        val out = PrintWriter(FileOutputStream("${it.function.name}-dominator.dot"))
        GraphGenerator.createDominatorTreeOutput(dominatorTree = dominatorAnalysis[it.function.name]!!).writeToFile(out)
        out.close()
    }

    println(reachingDefsAnalysis)
    println("---------")
    println(liveVarsAnalysis)

}
