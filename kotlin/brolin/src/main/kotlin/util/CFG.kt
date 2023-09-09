package util

import trees.*

class CFGNode(
    val name: String,
    val block: BasicBlock,
    val predecessors: MutableList<CFGNode>,
    val successors: MutableList<CFGNode>
) {
    val defines: Set<WriteInstruction> by lazy {
        val nameToDefn = mutableMapOf<String, WriteInstruction>()
        block.instructions.filterIsInstance<WriteInstruction>().forEach { nameToDefn[it.dest] = it }
        nameToDefn.values.toSet()
    }

    val definedNames: Set<String> by lazy {
        defines.map { it.dest }.toSet()
    }
}

data class CFG(
    /** The function from which this CFG was constructed. */
    val function: CookedFunction,
    val entry: CFGNode,
    val nodes: MutableList<CFGNode> = mutableListOf()
) {
    companion object {
        fun of(function: CookedFunction, freshLabels: FreshLabelGearLoop): CFG {
            val labelToNode = mutableMapOf<String, CFGNode>()
            val nodes = mutableListOf<CFGNode>()
            val fnName = function.name
            // Initialize nodes
            BlockSetter().block(function).forEach { block ->
                val first = block.instructions.first()
                val name = freshLabels.get("${fnName}${if (first is CookedLabel) "_" + first.label else ""}")
                val node = CFGNode(name, block, mutableListOf(), mutableListOf())
                if (first is CookedLabel) labelToNode[first.label] = node
                nodes.add(node)
            }
            // Add edges
            nodes.forEachIndexed { i, node ->
                val last = node.block.instructions.last()
                val successors: List<CFGNode> = when {
                    last is EffectOperation && last.op == Operator.JMP -> listOf(labelToNode[last.labels[0]]!!)
                    last is EffectOperation && last.op == Operator.BR ->
                        last.labels.map { label -> labelToNode[label]!! }
                    else -> if (i + 1 != nodes.size) listOf(nodes[i + 1]) else listOf()
                }
                successors.forEach { succ -> succ.predecessors.add(node) }
                node.successors.addAll(successors)
            }
            val entry = nodes.first()
            return CFG(function, entry, nodes)
        }
    }
}

/** A list of CFGs, each corresponding to one function. It is guaranteed that no two CFG nodes have the same name. */
data class CFGProgram(val graphs: List<CFG>) {
    companion object {
        fun of(program: CookedProgram): CFGProgram {
            val freshLabels = FreshLabelGearLoop(program)
            return CFGProgram(program.functions.map { CFG.of(it, freshLabels) })
        }
    }
}
