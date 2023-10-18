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
        val freshLabelGearLoop = FreshLabelGearLoop(cfgSsaProgram)
        loopInvariantCodeMotion(cfgSsaProgram, freshLabelGearLoop)
        return cfgSsaProgram.toCookedProgram()
    }

    private fun loopInvariantCodeMotion(ssa: CFGProgram, freshLabelGearLoop: FreshLabelGearLoop) {
        val reachingDefs = ReachingDefsAnalysis.analyze(ssa)
        val dominatedMap = DominatorsAnalysis.getDominatorsMap(program = ssa)
        ssa.graphs.forEach { cfg ->
            loopInvariantCodeMotion(cfg, freshLabelGearLoop, dominatedMap[cfg.fnName]!!, reachingDefs)
        }
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


            ssa.nodes.filter { node -> node !in loop.nodes }.map { it.block.instructions }.forEach {
                it.forEach { insn ->
                    when (insn) {
                        is ValueOperation -> {
                            insn.labels = insn.labels.map { label ->
                                if (label == loop.header.name) preHeader.name else label
                            }
                        }

                        is EffectOperation -> {
                            insn.labels = insn.labels.map { label ->
                                if (label == loop.header.name) preHeader.name
                                else label
                            }
                        }

                        else -> {}
                    }
                }
            }



            if (loop.header == ssa.entry) {
                ssa.entry = preHeader
            }
            ssa.nodes.add(ssa.nodes.indexOf(loop.header), preHeader)

            val movedNames = movable.keys.filterIsInstance<WriteInstruction>().map { it.dest }

            ssa.nodes.forEach { node ->
                node.block.instructions.filter { ins -> ins.isPhi() }.forEach { phi ->
                    phi as ValueOperation
                    phi.labels = phi.labels.mapIndexed { i, label ->
                        if (phi.args[i] in movedNames) preHeader.name
                        else label
                    }
                }
            }

            /*
            TODO: replace all references to the header label outside of the loop with a ref to the preheader
             */

        }
        return
    }

}