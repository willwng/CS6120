package dataflow

import util.CFG
import util.CFGNode
import util.CFGProgram

typealias DominatorMap = Map<CFGNode, Set<CFGNode>>

object DominatorsAnalysis {

    private fun buildTree(cfg: CFG, dominators: DominatorMap): DominatorTree {
        // Convert each CFG node to a dominator tree node
        val dominatorNodesMap = cfg.nodes.associateWith { DominatorTreeNode(cfgNode = it) }
        // Populate the dominates field
        cfg.nodes.forEach { node ->
            val dominatorNode = dominatorNodesMap[node]!!
            dominatorNode.dominated = dominators[node]!!.map { dominatorNodesMap[it]!! }.toSet()
        }

        return DominatorTree(
            cfg = cfg,
            entry = dominatorNodesMap[cfg.entry]!!,
            nodes = dominatorNodesMap.values.toSet()
        )
    }

    private fun analyze(cfg: CFG): DominatorTree {
        // Map from a node to its dominators
        val nodeToDominators = cfg.nodes.associateWith { _ -> setOf<CFGNode>() }.toMutableMap()
        // Map from a node to the nodes that it STRICTLY dominates
        val nodeToStrictDom = nodeToDominators.toMutableMap()

        var changed = true
        while (changed) {
            changed = false
            cfg.nodes.forEach { node ->
                val prevDominators = nodeToDominators[node]
                // Determine the dominators of the node's predecessors
                val predDominators = if (node.predecessors.isNotEmpty())
                    (node.predecessors.map { pred -> nodeToDominators[pred]!! }).reduce { acc, t -> acc.intersect(t) }
                else emptySet()

                val newDominators = predDominators union setOf(node)
                if (prevDominators != newDominators) changed = true
                nodeToDominators[node] = newDominators
                newDominators.filter { it != node }
                    .forEach { nodeToStrictDom[it] = nodeToStrictDom[it]!!.union(setOf(node)) }
            }
        }
        // Create a map of strict dominators
        val strictDominators = nodeToDominators.toMutableMap()
        strictDominators.forEach { (node, dom) -> strictDominators[node] = dom.filter { it != node }.toSet() }

        // Convert map to immediate dominators
        // for each dominator: we check if it dominates any of the other dominators
        val immediateDominators = strictDominators.toMutableMap()
        immediateDominators.forEach { (node, dom) ->
            immediateDominators[node] = dom.filter { potDom ->
                (dom.map { otherDom -> otherDom !in nodeToStrictDom[potDom]!! }).fold(true) { acc, it -> acc && it }
            }.toSet()
        }

        return buildTree(cfg = cfg, dominators = immediateDominators)
    }

    /** Builds and returns a map for function CFGs to DominatorTrees */
    fun analyze(program: CFGProgram): Map<String, DominatorTree> =
        program.graphs.associate { cfg -> cfg.function.name to analyze(cfg = cfg) }

}

/** [DominatorTree] represents the dominator tree for a given CFG */
data class DominatorTree(
    val cfg: CFG,
    val entry: DominatorTreeNode,
    val nodes: Set<DominatorTreeNode>
)

/** [DominatorTreeNode] represents a node in the dominator tree */
class DominatorTreeNode(
    val cfgNode: CFGNode,
) {
    // The list of nodes that this node is strictly dominated by
    lateinit var dominated: Set<DominatorTreeNode>
}