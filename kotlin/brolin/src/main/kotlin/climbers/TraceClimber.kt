package climbers

import trees.CookedInstruction
import trees.CookedLabel
import trees.EffectOperation
import trees.ValueOperation
import util.*

data class Trace(
    val instructions: List<CookedInstruction>, val label: CookedLabel?, val traceStart: CFGNode, val traceEnd: CFGNode
)

/*
TODO
    br only guard if there's something afterwards
    why not more speculation

 */

object TraceClimber : CFGClimber {
    override fun applyToCFG(cfgProgram: CFGProgram): CFGProgram {
        val gearLoop = FreshLabelGearLoop(cfgProgram)
        val nameLoop = FreshNameGearLoop(cfgProgram)
        cfgProgram.graphs.forEach { cfg ->
            val trace = buildTrace(cfg = cfg, gearLoop = gearLoop, nameLoop = nameLoop)
            buildSpeculateBlock(trace = trace, gearLoop = gearLoop, cfg = cfg)
        }
        return cfgProgram
    }

    private fun buildSpeculateBlock(trace: Trace, gearLoop: FreshLabelGearLoop, cfg: CFG): CFGNode {
//        println(trace.traceEnd.successors.map { it.name })
//        println(trace.traceEnd.name + " ," + trace.traceStart.name)
        val succ = trace.traceEnd.getHotSuccessor()
        val succFlow =
            if (succ != null) listOf(EffectOperation.jump(label = succ.name)) else listOf<CookedInstruction>()
        val traceNode = CFGNode(
            name = trace.label?.label ?: gearLoop.get(),
            block = BasicBlock(instructions = listOf(EffectOperation.speculate()) + trace.instructions + EffectOperation.commit() + succFlow),
            predecessors = trace.traceStart.predecessors.toMutableList(),
            successors = (trace.traceEnd.successors + trace.traceStart).toMutableList()
        )
        traceNode.predecessors.forEach {
            it.successors.remove(trace.traceStart)
            it.successors.add(traceNode)
        }
        traceNode.successors.forEach {
            it.predecessors.add(traceNode)
        }
        cfg.nodes.add(traceNode)
        if (cfg.entry == trace.traceStart) cfg.entry = traceNode
        return traceNode
    }

    /** Helper function to return the count associated with a CFGNode */
    private fun CFGNode.getCount(): Int {
        return block.instructions.map { it.count ?: 0 }.max()
    }

    private fun CFGNode.getHotSuccessor(): CFGNode? {
        return if (successors.size == 1) {
            successors.first()
        }
        // If there are two successors, pick the path that is "hotter"
        else if (successors.size == 2) {
            val succ1 = successors.first()
            val succ2 = successors.last()
            if (succ1.getCount() > succ2.getCount()) succ1 else succ2
        } else {
            null
        }
    }

    // TODO trace through calls
    private fun buildTrace(cfg: CFG, gearLoop: FreshLabelGearLoop, nameLoop: FreshNameGearLoop): Trace {
        val traceInsns = mutableListOf<CookedInstruction>()
        var label: CookedLabel? = null
        var curr = cfg.entry
        var traceStart = curr
        var traceEnd = curr
        var tracing = false
        val threshold = 3
        val seen = mutableSetOf<CFGNode>()
        while (curr.successors.isNotEmpty() && curr !in seen) {
            seen.add(curr)
            val count = curr.getCount()
            if (count >= threshold) {
                if (!tracing) {
                    traceStart = curr
                }
                tracing = true
            } else if (tracing) {
                break
            }
            if (tracing) {
                traceEnd = curr
                // Add the (potentially) modified clones of instructions
                curr.block.instructions.forEach {
                    when {
                        it is CookedLabel -> {
                            if (traceInsns.isEmpty()) {
                                label = it
                                curr.name = gearLoop.get(curr.name)
                            }
                        }

                        it.isBranch() -> {
                            // Guard: recover to the original node
                            it as EffectOperation
                            val trueHot = curr.getHotSuccessor()?.name == it.labels.first()
                            val branchArg = it.args.first()
                            if (trueHot) {
                                traceInsns.add(EffectOperation.guard(arg = branchArg, recover = traceStart.name))
                            } else {
                                val negBranchArg = nameLoop.get(branchArg)
                                traceInsns.add(ValueOperation.negate(dest = negBranchArg, arg = branchArg))
                                traceInsns.add(EffectOperation.guard(arg = negBranchArg, recover = traceStart.name))
                            }
                        }

                        it.isControlFlow() -> {}
                        else -> traceInsns.add((it as CookedInstruction).clone())
                    }
                }
                // Rid the label in the original block
                curr.replaceInsns(curr.block.instructions.filterIsInstance<CookedInstruction>())
            }

            // Continue tracing along the hottest path
            curr = curr.getHotSuccessor() ?: break
        }

        return Trace(
            instructions = traceInsns,
            label = label,
            traceStart = traceStart,
            traceEnd = traceEnd,
        )
    }

}