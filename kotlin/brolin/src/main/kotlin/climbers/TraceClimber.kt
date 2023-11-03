package climbers

import trees.CookedInstruction
import trees.CookedLabel
import trees.EffectOperation
import trees.ReadInstruction
import util.*

data class Trace(
    val instructions: List<CookedInstruction>,
    val label: CookedLabel?,
    val traceStart: CFGNode,
    val traceEnd: CFGNode
)

/*
TODO
    br only guard if there's something afterwards
    why not more speculation

 */

object TraceClimber : CFGClimber {
    override fun applyToCFG(cfgProgram: CFGProgram): CFGProgram {
        val gearLoop = FreshLabelGearLoop(cfgProgram)
        cfgProgram.graphs.forEach { cfg ->
            val trace = buildTrace(cfg = cfg, gearLoop = gearLoop)
            buildSpeculateBlock(trace = trace, gearLoop = gearLoop, cfg = cfg)
        }
        return cfgProgram
    }

    private fun buildSpeculateBlock(trace: Trace, gearLoop: FreshLabelGearLoop, cfg: CFG): CFGNode {
        val traceNode = CFGNode(
            name = trace.label?.label ?: gearLoop.get(),
            block = BasicBlock(instructions = listOf(EffectOperation.speculate()) + trace.instructions + EffectOperation.commit()),
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

    // TODO trace through calls
    private fun buildTrace(cfg: CFG, gearLoop: FreshLabelGearLoop): Trace {
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
            val count = curr.block.instructions.firstOrNull()?.count ?: 0
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
                            it as ReadInstruction
                            traceInsns.add(EffectOperation.guard(arg = it.args[0], recover = curr.name))
                        }
                        it.isControlFlow() -> {}
                        else -> traceInsns.add((it as CookedInstruction).clone())
                    }
                }
                // Rid the label in the original block
                curr.replaceInsns(curr.block.instructions.filterIsInstance<CookedInstruction>())
            }

            // Find the hottest path
            if (curr.successors.size == 1) {
                curr = curr.successors[0]
            }
            // If there are two successors, pick the path that is "hotter"
            else if (curr.successors.size == 2) {
                val succ1 = curr.successors.first()
                val succ2 = curr.successors.last()
                val count1 = succ1.block.instructions.firstOrNull()?.count ?: 0
                val count2 = succ2.block.instructions.firstOrNull()?.count ?: 0
                curr = if (count1 > count2) succ1 else succ2
            } else {
                break
            }
        }

        return Trace(
            instructions = traceInsns,
            label = label,
            traceStart = traceStart,
            traceEnd = traceEnd,
        )
    }

}