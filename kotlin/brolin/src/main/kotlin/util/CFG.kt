package util

import trees.*

open class CFGNode(
    var name: String,
    var block: BasicBlock,
    val predecessors: MutableList<CFGNode>,
    val successors: MutableList<CFGNode>
) {

    val hasSpeculate by lazy {
        block.instructions.any {
            it is EffectOperation && it.op == Operator.SPECULATE
        }
    }

    fun defines(): Set<WriteInstruction> {
        val nameToDefn = mutableMapOf<String, WriteInstruction>()
        block.instructions.filterIsInstance<WriteInstruction>().forEach { nameToDefn[it.dest] = it }
        return nameToDefn.values.toSet()
    }

    fun definedNamesWithType(): Map<String, Type> = defines().associate { it.dest to it.type }


    fun definedNames(): Set<String> = defines().map { it.dest }.toSet()


    fun replaceInsns(insns: List<CookedInstructionOrLabel>) {
        block = BasicBlock(insns)
    }

    override fun toString() = "{name: $name, block: $block}"

    companion object EmptyCFG : CFGNode(
        name = "Empty",
        block = BasicBlock(listOf()),
        successors = mutableListOf(),
        predecessors = mutableListOf()
    )
}

/** A class to represent a control-flow graph of a function */
data class CFG(
    // The function from which this CFG was constructed
    val fnName: String,
    val fnArgs: List<Argument> = listOf(),
    val fnType: Type? = null,
    var entry: CFGNode,
    val nodes: MutableList<CFGNode> = mutableListOf()
) {

    val nodesWithSpeculate by lazy { nodes.filter { it.hasSpeculate } }


    /** A simple optimization that simple removes any CFG nodes that are not reachable from entry */
    fun pruneUnreachableNodes(): CFG {
        val reachableNodes = mutableSetOf<CFGNode>()

        val queue = ArrayDeque<CFGNode>()
        queue.add(entry)
        while (queue.isNotEmpty()) {
            val currNode = queue.removeFirst()
            queue.addAll(currNode.successors.filter { it !in reachableNodes })
            reachableNodes.add(currNode)
        }
        val unreachableNodes = nodes.toSet().minus(reachableNodes)
        nodes.forEach {
            it.predecessors.removeAll(unreachableNodes)
        }

        return CFG(fnName, fnArgs, fnType, entry, nodes = reachableNodes.toMutableList())
    }

    /** Converts a CFG back into a function */
    fun toCookedFunction(): CookedFunction {
        val instructions = mutableListOf<CookedInstructionOrLabel>()
        nodes.forEachIndexed { i, node ->
            val blockInstructions = node.block.instructions

            // If we generated a fresh name for this node, we need to give it a label
            if (blockInstructions.firstOrNull() !is CookedLabel) {
                instructions.add(CookedLabel(node.name))
            }
            instructions.addAll(blockInstructions)
            // Add jumps for non-terminators to retain control flow
            val lastInstruction = blockInstructions.lastOrNull()
            if (lastInstruction?.isControlFlow()?.not() == true) {
                assert(node.successors.size <= 1) // ret is optional -> possible to have no successors
                if (node.successors.size == 1) {
                    val succ = node.successors.first()
                    // No fall-through, require a jump
                    if (i < nodes.size - 1 && nodes[i + 1] != succ)
                        instructions.add(EffectOperation.jump(node.successors.first().name))
                } else {
                    instructions.add(EffectOperation.ret())
                }
            }
        }

        return CookedFunction(
            name = fnName,
            args = fnArgs,
            type = fnType,
            instructions = instructions
        )
    }

    companion object {
        /** Constructs a CFG from a function */
        fun of(function: CookedFunction, freshLabels: FreshLabelGearLoop): CFG {
            val labelToNode = mutableMapOf<String, CFGNode>()
            val nodes = mutableListOf<CFGNode>()
            val fnName = function.name

            // Initialize nodes. If the block already starts with a label, use it
            BlockSetter().block(function).forEach { block ->
                val first = block.instructions.first()
                val name = if (first is CookedLabel) first.label else freshLabels.get(fnName)
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

                    last is EffectOperation && last.op == Operator.RET -> listOf()
                    // Handle potential fall-through
                    else -> if (i + 1 != nodes.size) listOf(nodes[i + 1]) else listOf()
                }
                successors.forEach { succ -> succ.predecessors.add(node) }
                node.successors.addAll(successors)
            }

            val entry = nodes.firstOrNull() ?: CFGNode.EmptyCFG

            // Hack: Add an empty entry node to each CFG. This allows us to have phi instructions at the beginning of
            // any node
            val newName = freshLabels.get()
            val entrier = if (entry == CFGNode.EmptyCFG) entry else CFGNode(
                name = newName,
                block = BasicBlock(listOf(CookedLabel(newName))),
                successors = mutableListOf(entry),
                predecessors = mutableListOf()
            )
            if (entry != CFGNode.EmptyCFG) entry.predecessors.add(entrier)

            return CFG(
                function.name,
                function.args,
                function.type,
                entrier,
                (if (nodes.isNotEmpty()) listOf(entrier) + nodes else mutableListOf(CFGNode.EmptyCFG)) as MutableList<CFGNode>
            )
        }
    }
}

/** A list of CFGs, each corresponding to one function. It is guaranteed that no two CFG nodes have the same name. */
data class CFGProgram(val graphs: List<CFG>) {
    val freshLabels = FreshLabelGearLoop(this)
    val freshNames = FreshNameGearLoop(this)

    companion object {
        fun of(program: CookedProgram): CFGProgram {
            val freshLabels = FreshLabelGearLoop(program)
            return CFGProgram(program.functions.map { CFG.of(it, freshLabels).pruneUnreachableNodes() })
        }
    }

    fun toCookedProgram(): CookedProgram {
        return CookedProgram(functions = graphs.map { it.toCookedFunction() })
    }
}
