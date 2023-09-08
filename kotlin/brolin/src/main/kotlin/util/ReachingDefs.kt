package util

import trees.CookedProgram
import trees.WriteInstruction

class ReachingDefs {
    class ReachingDefsBeta : DataflowBeta<ReachingDefsBeta.ReachingDefs> {
        // TODO We must modify the notion of equality on instructions to be diff based on srcloc or physical location
        data class ReachingDefs(val defs: Set<WriteInstruction>) : DataflowValue

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

    fun analyze(program: CookedProgram) {
        val analysis = DataflowAnalysis(ReachingDefsBeta())
        val cfg = CFGProgram.of(program)
        println(analysis.forwardWorklist(cfg))
    }
}