package climbers

import analysis.DominatedMap
import analysis.DominatorsAnalysis
import analysis.LoopAnalysis
import analysis.dataflow.DataflowAnalysis.DataflowResult
import analysis.dataflow.ReachingDefsAnalysis
import analysis.dataflow.ReachingDefsAnalysis.ReachingDefsBeta.ReachingDefs
import trees.EffectOperation
import trees.ValueOperation
import util.*

/** This climber performs loop-invariant code motion
 * Note from the climber: Do me after SSA! */
object LICMClimber : CFGClimber {
    override fun applyToCFG(cfgProgram: CFGProgram): CFGProgram {
        val cfgSsaProgram = SSAClimber.applyToCFG(cfgProgram)
        val freshLabelGearLoop = FreshLabelGearLoop(cfgSsaProgram)
        val freshNameGearLoop = FreshNameGearLoop(cfgSsaProgram)
        loopInvariantCodeMotion(cfgSsaProgram, freshLabelGearLoop, freshNameGearLoop)
        return cfgSsaProgram
    }

    private fun loopInvariantCodeMotion(
        ssa: CFGProgram,
        freshLabelGearLoop: FreshLabelGearLoop,
        freshNameGearLoop: FreshNameGearLoop,
    ) {
        val reachingDefs = ReachingDefsAnalysis.analyze(ssa)
        val dominatedMap = DominatorsAnalysis.getDominatorsMap(program = ssa)
        ssa.graphs.forEach { cfg ->
            loopInvariantCodeMotion(
                cfg,
                freshLabelGearLoop,
                freshNameGearLoop,
                dominatedMap[cfg.fnName]!!,
                reachingDefs
            )
        }
    }

    private fun loopInvariantCodeMotion(
        ssa: CFG,
        freshLabelGearLoop: FreshLabelGearLoop,
        freshNameGearLoop: FreshNameGearLoop,
        dominatedMap: DominatedMap,
        reachingDefs: Map<String, DataflowResult<ReachingDefs>>
    ) {
        val loops = LoopAnalysis.getLoops(cfg = ssa, dominatedMap = dominatedMap)
        loops.forEach { loop ->
            val loopInvIns =
                LoopAnalysis.getLoopInvariantInstructions(loop = loop, reachingDefsResult = reachingDefs[ssa.fnName]!!)

            // if it dominates all loop.exits, we can move it
            val movable = loopInvIns.filter { (_, node) -> dominatedMap[node]!!.containsAll(loop.exits) }

            // Create a pre-header node and add LI instructions
            val phName = freshLabelGearLoop.get(loop.header.name)
            val preHeader = CFGNode(
                name = phName,
                block = BasicBlock(movable.keys.toList()),
                predecessors = loop.header.predecessors,
                successors = mutableListOf(loop.header)
            )
            // Update control-flow for header
            loop.header.predecessors.forEach {
                it.successors.clear()
                it.successors.add(preHeader)
            }
            loop.header.predecessors.clear()
            loop.header.predecessors.add(preHeader)
            if (loop.header == ssa.entry) ssa.entry = preHeader
            ssa.nodes.add(ssa.nodes.indexOf(loop.header), preHeader)

            // Any node (outside the loop) that can jump to the header should now jump to the pre-header
            ssa.nodes.filter { node -> node !in loop.nodes }.map { it.block.instructions }.forEach { insns ->
                insns.filter { it.isControlFlow() }.forEach { insn ->
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


            // Remove LI instructions from loop
            loop.nodes.forEach {
                it.replaceInsns(
                    it.block.instructions.filter { ins -> ins !in movable }
                )
            }


            // For each phi-node in the header, split it up:
            //  if the label comes from within the loop, keep it
            //  if there are any labels from outside the loop, create a phi node in the pre-header and add the label
            //  (now with a reference to the pre-header in the header phi node)
            val loopLabels = loop.nodes.map { it.name }
            loop.header.block.instructions.filter { it.isPhi() }.forEach { phi ->
                phi as ValueOperation
                // Doesn't come from loop? Ruh roh
                val (nonLoopArgs, nonLoopLabels) = phi.args.zip(phi.labels)
                    .filter { (_, label) -> label !in loopLabels }.unzip()
                if (nonLoopArgs.isNotEmpty()) {
                    val newDest = freshNameGearLoop.get(phi.dest)
                    val newPhiNode = ValueOperation(
                        op = phi.op,
                        dest = newDest,
                        type = phi.type,
                        args = nonLoopArgs,
                        labels = nonLoopLabels,
                    )
                    preHeader.replaceInsns(listOf(newPhiNode) + preHeader.block.instructions)

                    // Add a reference to the new phi node destination and remove the non-loop-args
                    val (args, labels) = phi.args.zip(phi.labels).filter { (_, label) ->
                        label in loopLabels
                    }.unzip()
                    phi.args = args + newDest
                    phi.labels = labels + preHeader.name
                }
            }
        }
        return
    }
}
