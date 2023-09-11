package util

import trees.CookedProgram
import trees.WriteInstruction

object ReachingDefs {
    class ReachingDefsBeta : DataflowBeta<ReachingDefsBeta.ReachingDefs> {
        data class ReachingDefs(val defs: Set<WriteInstruction>) : DataflowValue

        override val forward = true

        override val init: ReachingDefs = ReachingDefs(setOf())

        override fun merge(predecessors: List<ReachingDefs>): ReachingDefs =
            ReachingDefs(predecessors.fold(setOf()) { acc, pred -> acc.union(pred.defs) })

        override fun transfer(node: CFGNode, inEdge: ReachingDefs): ReachingDefs {
            val defsIn = mutableMapOf<String, WriteInstruction>()
            inEdge.defs.forEach { defsIn[it.dest] = it }
            val kills = defsIn.filterKeys { it in node.definedNames }.values.toSet()
            return ReachingDefs(node.defines.union(inEdge.defs subtract kills))
        }
    }

    fun analyze(program: CookedProgram): Map<String, DataflowAnalysis.DataflowResult<ReachingDefsBeta.ReachingDefs>> =
        DataflowAnalysis(ReachingDefsBeta()).applyToProgram(program)
}
