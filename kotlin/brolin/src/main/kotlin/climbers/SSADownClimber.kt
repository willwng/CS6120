package climbers

import trees.*
import util.CFG
import util.CFGNode
import util.CFGProgram

/** A down-climber climbs in reverse. */
object SSADownClimber : Climber {
    override fun applyToProgram(program: CookedProgram): CookedProgram {
        val cfgProgram = CFGProgram.of(program)
        cfgProgram.graphs.forEach { cfg -> convertFromSSA(cfg) }
        return cfgProgram.toCookedProgram()
    }

    private fun convertFromSSA(cfg: CFG) {
        val nameToNode = mutableMapOf<String, CFGNode>()  // Unique mapping since SSA
        val defined = mutableSetOf<String>() // TODO making the possibly false assumption that if something used on RHS of phi is defined at all, it is defined at that exec point. I think this is true
        cfg.nodes.forEach { node ->
            nameToNode[node.name] = node
            defined.addAll(node.definedNames)
        }
        defined.addAll(cfg.fnArgs.map{it.name})

//        println(varToDefiningNode.keys)
//        println(cfg)

        fun CookedInstructionOrLabel.isPhi() = this is ValueOperation && this.op == Operator.PHI

        cfg.nodes.forEach { node ->
            node.block.instructions.filter { it.isPhi() }.forEach { phi ->
                (phi as ValueOperation)
                phi.args.zip(phi.labels).filter { (arg, _) -> arg in defined }. forEach { (arg, label) ->
//                    println(arg)
                    val defNode = nameToNode[label]!!
//                    if (defNode != null) {
                    val copyInsn = listOf(
                        ValueOperation(
                            op = Operator.ID,
                            dest = phi.dest,
                            type = phi.type,
                            args = listOf(arg)
                        )
                    )
                    val last = defNode.block.instructions.last()
                    if (last is EffectOperation && last.op in Operator.TERMINATORS) {
                        defNode.replaceInsns(defNode.block.instructions.dropLast(1) + copyInsn + last)
                    } else {
                        defNode.replaceInsns(defNode.block.instructions + copyInsn)
                    }
                } // Else we'll just skip it. The phi node included an impossible state
//                }

            }

            node.replaceInsns(node.block.instructions.filter { !it.isPhi() })
        }
    }
}