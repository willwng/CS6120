package analysis

import analysis.dataflow.DataflowAnalysis.DataflowResult
import analysis.dataflow.ReachingDefsAnalysis.ReachingDefsBeta.ReachingDefs
import trees.ConstantInstruction
import trees.CookedInstruction
import trees.ReadInstruction
import util.CFG
import util.CFGNode

/** A natural loop consists of a header, and the set of nodes in the loop */
data class NaturalLoop(val header: CFGNode, val nodes: Set<CFGNode>) {
    val exits: Set<CFGNode> by lazy {
        nodes.flatMap { it.successors.minus(nodes) }.toSet()
    }
}

data class BackEdge(val source: CFGNode, val sink: CFGNode)

typealias LoopInvariantInstructions = Map<CookedInstruction, CFGNode>

object LoopAnalysis {
    /** Identifies all the back-edges in the CFG, which are edges from A to B where B dominates A */
    private fun getBackEdges(cfg: CFG, dominatedMap: DominatedMap): Set<BackEdge> {
        val backEdges = mutableSetOf<BackEdge>()
        cfg.nodes.forEach { a ->
            a.successors.forEach { b ->
                if (a in dominatedMap[b]!!) {
                    backEdges.add(BackEdge(source = a, sink = b))
                }
            }
        }
        return backEdges
    }

    /** Gets the set of loops associated with the CFG */
    fun getLoops(cfg: CFG, dominatedMap: DominatedMap): Set<NaturalLoop> {
        val backEdges = getBackEdges(cfg = cfg, dominatedMap = dominatedMap) // maps n to the nodes n dominates
        val loops = mutableSetOf<NaturalLoop>()
        backEdges.forEach {
            // Identify the loop for this given back-edge
            val loop = mutableSetOf<CFGNode>()
            funnyDFS(header = it.sink, curr = it.source, visited = loop)
            loops.add(NaturalLoop(header = it.sink, nodes = loop))
        }
        return loops
    }

    /** A DFS to find all the predecessors leading up to the header */
    private fun funnyDFS(header: CFGNode, curr: CFGNode, visited: MutableSet<CFGNode>) {
        if (curr in visited) return
        visited.add(curr)
        if (curr == header) return
        curr.predecessors.forEach { funnyDFS(header = header, curr = it, visited = visited) }
    }

    /** Returns the loop invariant instructions for the given loop: format is a map from instruction -> cfg node */
    fun getLoopInvariantInstructions(
        loop: NaturalLoop,
        reachingDefsResult: DataflowResult<ReachingDefs>
    ): LoopInvariantInstructions {
        val loopInvariant = mutableMapOf<CookedInstruction, CFGNode>()
        val instructionsSet = loop.nodes.fold(listOf<CookedInstruction>()) { acc, cfgNode ->
            acc + cfgNode.block.instructions.filterIsInstance<CookedInstruction>()
        }.toSet()

        var change = true
        while (change) {
            change = false
            loop.nodes.forEach { node ->
                val reachingDefs = reachingDefsResult.result[node]!!.first.defs
                node.block.instructions.forEach { insn ->
                    // Constant instructions are always loop invariant
                    if (insn is ConstantInstruction) {
                        change = insn in loopInvariant
                        loopInvariant[insn] = node
                    }
                    // For every argument of the instruction, check if the reaching def is either:
                    //  outside the loop or already loop-invariant
                    if (insn is ReadInstruction && insn.isPure()) {
                        val isLoopInvariant = insn.args.map { arg ->
                            reachingDefs.filter { it.dest == arg }.map {
                                it !in instructionsSet || it in loopInvariant
                            }
                        }.fold(true) { acc, booleans -> acc && booleans.fold(true) { acc2, b -> acc2 && b } }
                        if (isLoopInvariant) {
                            change = insn in loopInvariant
                            loopInvariant[insn] = node
                        }
                    }
                }

            }
        }
        return loopInvariant
    }
}