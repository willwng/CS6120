package util

import util.DataflowAnalysis.DataflowResult
import util.LiveVariablesAnalysis.LiveVariablesBeta.LiveVars

object LiveVariablesAnalysis {
    class LiveVariablesBeta : DataflowBeta<LiveVars> {
        data class LiveVars(val live: Set<String>) : DataflowValue

        override val init: LiveVars = LiveVars(setOf())
        override val forward = false

        override fun merge(influencers: List<LiveVars>): LiveVars =
            LiveVars(influencers.fold(setOf()) { acc, pred -> acc.union(pred.live) })

        override fun transfer(node: CFGNode, inEdge: LiveVars): LiveVars {
            val liveSet = inEdge.live.toMutableSet()
            liveSet.removeIf { it in node.definedNames }
            liveSet.addAll(node.usedNames)
            return LiveVars(liveSet)
        }

    }

    fun analyze(program: CFGProgram): Map<String, DataflowResult<LiveVars>> =
        DataflowAnalysis(LiveVariablesBeta()).applyToProgram(program)
}
