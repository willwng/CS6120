
import analysis.DominatorsAnalysis
import analysis.dataflow.ConstantPropAnalysis
import analysis.dataflow.LiveVariablesAnalysis
import analysis.dataflow.ReachingDefsAnalysis
import analysis.prettyPrintFrontiers
import analysis.prettyPrintMaps
import analysis.prettyPrintTrees
import climbers.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import trees.RawProgram
import trees.TreeCooker
import util.CFGProgram
import util.GraphGenerator
import java.io.FileOutputStream
import java.io.PrintWriter

fun main(args: Array<String>) {
    val actions = handleArgs(args = args)

    val input = generateSequence(::readLine).joinToString("\n")
    val jsonElement = Json.parseToJsonElement(input)
    val rawProgram = Json.decodeFromJsonElement<RawProgram>(jsonElement)
    val cookedProgram = TreeCooker.cookProgram(rawProgram)
    var cfgProgram = CFGProgram.of(cookedProgram)

    actions.forEach { action ->
        when (action) {
            Actions.DCE -> cfgProgram = DCEClimber.applyToCFG(cfgProgram)
            Actions.LVN -> cfgProgram = LVNClimber.applyToCFG(cfgProgram)
            Actions.REACH -> {
                val reachingDefsAnalysis = ReachingDefsAnalysis.analyze(cfgProgram)
                println(reachingDefsAnalysis)
            }

            Actions.LICM -> cfgProgram = LICMClimber.applyToCFG(cfgProgram)

            Actions.LIVE -> {
                val liveVarsAnalysis = LiveVariablesAnalysis.analyze(cfgProgram)
                println(liveVarsAnalysis)
            }

            Actions.CONST_PROP -> {
                val constantPropAnalysis = ConstantPropAnalysis.analyze(cfgProgram)
                println(constantPropAnalysis)
            }

            Actions.DOMINATOR_TREE -> {
                val dominatorAnalysis = DominatorsAnalysis.getDominatorTrees(cfgProgram)
                println(dominatorAnalysis.prettyPrintTrees())
                dominatorAnalysis.forEach { (func, tree) ->
                    val out = PrintWriter(FileOutputStream("$func.dot"))
                    GraphGenerator.createDominatorTreeOutput(tree.first).writeToFile(out)
                    out.close()
                }
            }

            Actions.DOMINATORS -> {
                val dominatorAnalysis = DominatorsAnalysis.getDominatedMap(cfgProgram)
                println(dominatorAnalysis.prettyPrintMaps())
            }

            Actions.DOMINANCE_FRONTIER -> {
                val dominatorAnalysis = DominatorsAnalysis.getDominanceFrontiers(cfgProgram)
                println(dominatorAnalysis.prettyPrintFrontiers())
            }
            // Output is handled after optimizations
            Actions.OUT -> {}
            Actions.SSA_PHI -> {
//                val ssaProgram = SSAClimber.applyToProgram(cookedProgram)
//                val prettyJsonPrinter = Json { prettyPrint = true }
//                println(prettyJsonPrinter.encodeToString(ssaProgram))
            }

            Actions.CFG -> {
                cfgProgram.graphs.forEach { cfg ->
                    val out = PrintWriter(FileOutputStream("${cfg.fnName}-cfg.dot"))
                    GraphGenerator.createGraphOutput<LiveVariablesAnalysis>(cfg = cfg, null).writeToFile(out)
                    out.close()
                }
            }
        }
    }
    cfgProgram = TraceClimber.applyToCFG(cfgProgram)
//    cfgProgram = SSAClimber.applyToCFG(cfgProgram)
    cfgProgram = LVNClimber.applyToTraces(cfgProgram)
//    cfgProgram = LVNClimber.applyToCFG(cfgProgram)
    cfgProgram = DCELiveClimber.applyToCFG(cfgProgram)
//    cfgProgram = LICMClimber.applyToCFG(cfgProgram)
//    cfgProgram = SSADownClimber.applyToCFG(cfgProgram)
//    cfgProgram = DCEClimber.applyToCFG(cfgProgram)
//    cfgProgram = LVNClimber.applyToCFG(cfgProgram)
//    cfgProgram = DCELiveClimber.applyToCFG(cfgProgram)
//    cfgProgram = DCEClimber.applyToCFG(cfgProgram)
    val prettyJsonPrinter = Json { prettyPrint = true }
    val optimizedProgram = cfgProgram.toCookedProgram()
    println(prettyJsonPrinter.encodeToString(optimizedProgram))
}

fun handleArgs(args: Array<String>): List<Actions> {
    val actions = mutableListOf(Actions.OUT)
    args.forEach { arg ->
        when (arg) {
            "--lvn" -> actions.add(Actions.LVN)
            "--dce" -> actions.add(Actions.DCE)
            "--reach" -> actions.add(Actions.REACH)
            "--licm" -> actions.add(Actions.LICM)
            "--live" -> actions.add(Actions.LIVE)
            "--cp" -> actions.add(Actions.CONST_PROP)
            "--nout" -> actions.remove(Actions.OUT)
            "--dom" -> actions.add(Actions.DOMINATORS)
            "--domtree" -> actions.add(Actions.DOMINATOR_TREE)
            "--domfront" -> actions.add(Actions.DOMINANCE_FRONTIER)
            "--cfg" -> actions.add(Actions.CFG)
            "--ssaphi" -> actions.add(Actions.SSA_PHI)
        }
    }
    return actions
}

enum class Actions {
    LVN, DCE, REACH, OUT, LIVE, CONST_PROP, DOMINATORS, DOMINATOR_TREE, DOMINANCE_FRONTIER, CFG, SSA_PHI, LICM
}
