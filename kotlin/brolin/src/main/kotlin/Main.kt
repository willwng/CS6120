import climbers.DCEClimber
import climbers.LVNClimber
import dataflow.ConstantPropAnalysis
import dataflow.DominatorsAnalysis
import dataflow.LiveVariablesAnalysis
import dataflow.ReachingDefsAnalysis
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import trees.RawProgram
import trees.TreeCooker
import util.CFGProgram

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

            Actions.DOMINATOR -> {
                val dominatorAnalysis = DominatorsAnalysis.analyze(cfgProgram)
                println(dominatorAnalysis)
            }

            Actions.OUT -> {
                val prettyJsonPrinter = Json { prettyPrint = true }
                println(prettyJsonPrinter.encodeToString(cookedProgram))
            }

            Actions.CFG -> {
                TODO()
            }
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
            "--dom" -> actions.add(Actions.DOMINATOR)
            "--cfg" -> actions.add(Actions.CFG)
        }
    }
    return actions
}

enum class Actions {
    LVN, DCE, REACH, OUT, LIVE, CONST_PROP, DOMINATOR, CFG
}