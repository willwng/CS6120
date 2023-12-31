package analysis.dataflow

import analysis.dataflow.ReachingDefsAnalysis.ReachingDefsBeta.ReachingDefs
import analysis.dataflow.DataflowAnalysis.DataflowResult
import trees.WriteInstruction
import util.CFGNode
import util.CFGProgram

object ReachingDefsAnalysis {
    class ReachingDefsBeta : DataflowBeta<ReachingDefs> {
        data class ReachingDefs(val defs: Set<WriteInstruction>) : DataflowValue

        override val forward = true

        override val init: ReachingDefs = ReachingDefs(setOf())

        override fun merge(influencers: List<ReachingDefs>): ReachingDefs =
            ReachingDefs(influencers.fold(setOf()) { acc, pred -> acc.union(pred.defs) })

        override fun transfer(node: CFGNode, inEdge: ReachingDefs): ReachingDefs {
            val defsIn = mutableMapOf<String, WriteInstruction>()
            inEdge.defs.forEach { defsIn[it.dest] = it }
            val kills = defsIn.filterKeys { it in node.definedNames() }.values.toSet()
            return ReachingDefs(node.defines().union(inEdge.defs subtract kills))
        }
    }

    fun analyze(program: CFGProgram): Map<String, DataflowResult<ReachingDefs>> =
        DataflowAnalysis(ReachingDefsBeta()).applyToProgram(program)
}
