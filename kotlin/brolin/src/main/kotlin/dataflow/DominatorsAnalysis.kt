package dataflow

import util.CFG
import util.CFGNode
import util.CFGProgram

typealias DominatorMap = Map<CFGNode, Set<CFGNode>>

object DominatorsAnalysis {

    /**
     * Builds a DominatorTree from the given dominators.
     * Requires dominators to be a map from nodes to its immediate dominators
     */
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

    /** Gets the dominators for the given CFG (maps from node to its dominators) */
    private fun getDominators(cfg: CFG): Pair<DominatorMap, DominatorMap> {
        // Maintain a from a node to its dominators, and a map from a node to its dominated nodes
        val nodeToDominators = cfg.nodes.associateWith { _ -> setOf<CFGNode>() }.toMutableMap()
        val nodeToDominated = nodeToDominators.toMutableMap()
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
                newDominators.forEach { nodeToDominated[it] = nodeToDominated[it]!!.union(setOf(node)) }
            }
        }
        return Pair(nodeToDominators, nodeToDominated)
    }

    /** Converts a dominator map to a strict dominator map */
    private fun DominatorMap.toStrict(): DominatorMap {
        val strictMap = this.toMutableMap()
        strictMap.forEach { (node, dom) -> strictMap[node] = dom.filter { it != node }.toSet() }
        return strictMap
    }

    /** Creates a dominator tree for the given CFG */
    private fun analyze(cfg: CFG): DominatorTree {
        val (nodeToDominators, nodeToDominated) = getDominators(cfg = cfg)
        // Convert to strict dominators
        val nodeToStrictDominators = nodeToDominators.toStrict()
        val nodeToStrictDominated = nodeToDominated.toStrict()

        // Convert map to immediate dominators
        // for each dominator: we check if it dominates any of the other dominators
        val immediateDominators = nodeToStrictDominators.toMutableMap()
        immediateDominators.forEach { (node, dom) ->
            immediateDominators[node] = dom.filter { potDom ->
                (dom.map { otherDom -> otherDom !in nodeToStrictDominated[potDom]!! }).fold(true) { acc, it -> acc && it }
            }.toSet()
        }

        return buildTree(cfg = cfg, dominators = immediateDominators)
    }

    /** Builds and returns a map for function CFGs to DominatorTrees */
    fun analyze(program: CFGProgram): Map<String, DominatorTree> =
        program.graphs.associate { cfg -> cfg.function.name to analyze(cfg = cfg) }

    /**
     * Returns the dominator frontier for the given dominatorTreeNode
     * A's dominance frontier contains B iff A does not strictly dominate B, but A does dominate some predecessor of B
     */
    fun computeDominanceFrontier(dominatorTreeNode: DominatorTreeNode, dominatorTree: DominatorTree) {
        TODO()
    }

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
    // The list of nodes that this node is immediately dominated by
    lateinit var dominated: Set<DominatorTreeNode>
}