import climbers.DCEClimber
import climbers.LVNClimber
import dataflow.*
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
    val cfgProgram = CFGProgram.of(cookedProgram)

    var optimizedProgram = cookedProgram
    actions.forEach { action ->
        when (action) {
            Actions.DCE -> optimizedProgram = DCEClimber.applyToProgram(optimizedProgram)
            Actions.LVN -> optimizedProgram = LVNClimber.applyToProgram(optimizedProgram)
            Actions.REACH -> {
                val reachingDefsAnalysis = ReachingDefsAnalysis.analyze(cfgProgram)
                println(reachingDefsAnalysis)
            }

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
                    GraphGenerator.createDominatorTreeOutput(tree).writeToFile(out)
                    out.close()
                }
            }

            Actions.DOMINATORS -> {
                val dominatorAnalysis = DominatorsAnalysis.getDominators(cfgProgram)
                println(dominatorAnalysis.prettyPrintMaps())
            }

            Actions.DOMINANCE_FRONTIER -> {
                val dominatorAnalysis = DominatorsAnalysis.getDominanceFrontiers(cfgProgram)
                println(dominatorAnalysis.prettyPrintFrontiers())
            }
            // Output is handled after optimizations
            Actions.OUT -> {}

            Actions.CFG -> {
                cfgProgram.graphs.forEach { cfg ->
                    val out = PrintWriter(FileOutputStream("${cfg.function.name}-cfg.dot"))
                    GraphGenerator.createGraphOutput<LiveVariablesAnalysis>(cfg = cfg, null).writeToFile(out)
                    out.close()
                }
            }
        }
        if (Actions.OUT in actions) {
            val testProgram = cfgProgram.toCookedProgram()
            val prettyJsonPrinter = Json { prettyPrint = true }
            println(prettyJsonPrinter.encodeToString(testProgram))
        }
    }
}

fun handleArgs(args: Array<String>): List<Actions> {
    val actions = mutableListOf(Actions.OUT)
    args.forEach { arg ->
        when (arg) {
            "--lvn" -> actions.add(Actions.LVN)
            "--dce" -> actions.add(Actions.DCE)
            "--reach" -> actions.add(Actions.REACH)
            "--live" -> actions.add(Actions.LIVE)
            "--cp" -> actions.add(Actions.CONST_PROP)
            "--nout" -> actions.remove(Actions.OUT)
            "--dom" -> actions.add(Actions.DOMINATORS)
            "--domtree" -> actions.add(Actions.DOMINATOR_TREE)
            "--domfront" -> actions.add(Actions.DOMINANCE_FRONTIER)
            "--cfg" -> actions.add(Actions.CFG)
        }
    }
    return actions
}

enum class Actions {
    LVN, DCE, REACH, OUT, LIVE, CONST_PROP, DOMINATORS, DOMINATOR_TREE, DOMINANCE_FRONTIER, CFG
}