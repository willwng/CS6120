package climbers

import analysis.DominatedMap
import analysis.DominatorsAnalysis
import analysis.LoopAnalysis
import analysis.dataflow.DataflowAnalysis.DataflowResult
import analysis.dataflow.ReachingDefsAnalysis
import analysis.dataflow.ReachingDefsAnalysis.ReachingDefsBeta.ReachingDefs
import trees.CookedProgram
import trees.EffectOperation
import trees.ValueOperation
import trees.WriteInstruction
import util.*

/** This climber performs loop-invariant code motion
 * Note from the climber: Do me after SSA! */
object LICMClimber : Climber {
    override fun applyToProgram(program: CookedProgram): CookedProgram {
        val ssaProgram = SSAClimber.applyToProgram(program)
        val cfgSsaProgram = CFGProgram.of(ssaProgram)
        // todo call licm
        return cfgSsaProgram.toCookedProgram()
    }

    fun loopInvariantCodeMotion(ssa: CFGProgram): CFGProgram {
        val reachingDefs = ReachingDefsAnalysis.analyze(ssa)
        // TODO call licm
    }

    private fun loopInvariantCodeMotion(
        ssa: CFG,
        freshLabelGearLoop: FreshLabelGearLoop,
        dominatedMap: DominatedMap,
        reachingDefs: Map<String, DataflowResult<ReachingDefs>>
    ) {
        val loops = LoopAnalysis.getLoops(cfg = ssa, dominatedMap = dominatedMap)
        loops.forEach { loop ->
            val loopInvIns =
                LoopAnalysis.getLoopInvariantInstructions(loop = loop, reachingDefsResult = reachingDefs[ssa.fnName]!!)
            val movable = loopInvIns.filter { (_, node) ->
                dominatedMap[node]!!.containsAll(loop.exits) // if it dominates all loop.exits, we can move it
            }

            // Add LI instructions before the loop
            val phName = freshLabelGearLoop.get(ssa.fnName)
            val preHeader = CFGNode(
                name = phName,
                block = BasicBlock(movable.keys.toList()),
                predecessors = loop.header.predecessors,
                successors = mutableListOf(loop.header)
            )
            loop.header.predecessors.forEach {
                it.successors.clear()
                it.successors.add(preHeader)
            }
            loop.header.predecessors.clear()
            loop.header.predecessors.add(preHeader)

            // Remove LI instructions from loop
            loop.nodes.forEach {
                it.replaceInsns(
                    it.block.instructions.filter { ins -> ins !in movable }
                )
            }
        }
        return
    }

}