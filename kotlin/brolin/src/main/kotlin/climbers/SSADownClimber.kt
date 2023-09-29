package climbers

import trees.CookedInstructionOrLabel
import trees.CookedProgram
import trees.Operator
import trees.ValueOperation
import util.CFG
import util.CFGProgram

/** A down-climber climbs in reverse. This one converts out of SSA form (replaces phi nodes with proper copies) */
object SSADownClimber : Climber {
    override fun applyToProgram(program: CookedProgram): CookedProgram {
        val cfgProgram = CFGProgram.of(program)
        cfgProgram.graphs.forEach { cfg -> convertFromSSA(cfg) }
        return cfgProgram.toCookedProgram()
    }

    private fun convertFromSSA(cfg: CFG) {
        val nameToNode = cfg.nodes.associateBy { it.name } // mapping from labels to CFG nodes
        fun CookedInstructionOrLabel.isPhi() = this is ValueOperation && this.op == Operator.PHI

        // Replace phi instructions, add a copy along the path from label to block
        cfg.nodes.forEach { node ->
            node.block.instructions.filter { it.isPhi() }.forEach { phi ->
                (phi as ValueOperation)
                phi.args.zip(phi.labels)
                    .filter { (arg, _) -> arg != PhiNode.UNDEFINED && arg != phi.dest }
                    .forEach { (arg, label) ->
                        val defNode = nameToNode[label]!!
                        val copyInstruction = ValueOperation(
                            op = Operator.ID,
                            dest = phi.dest,
                            type = phi.type,
                            args = listOf(arg)
                        )
                        // Maintain control flow, keep the last instruction if needed
                        val last = defNode.block.instructions.last()
                        if (last.isControlFlow()) {
                            defNode.replaceInsns(defNode.block.instructions.dropLast(1) + copyInstruction + last)
                        } else {
                            defNode.replaceInsns(defNode.block.instructions + copyInstruction)
                        }
                    }
            }

            node.replaceInsns(node.block.instructions.filter { !it.isPhi() })
        }
    }
}