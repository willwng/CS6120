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
        // Populate the dominates/dominated field
        cfg.nodes.forEach { node ->
            val treeNode = dominatorNodesMap[node]!!
            val treeDominators = dominators[node]!!.map { dominatorNodesMap[it]!! }
            treeNode.dominated = treeDominators.toSet()
            treeDominators.forEach { it.dominates.add(treeNode) }
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
        val nodeToDominators = cfg.nodes.associateWith { _ -> cfg.nodes.toSet() }.toMutableMap()
        val nodeToDominated = cfg.nodes.associateWith { setOf<CFGNode>() }.toMutableMap()
        var changed = true
        while (changed) {
            changed = false
            cfg.nodes.forEach { node ->
                val prevDominators = nodeToDominators[node]
                // Determine the dominators of the node's predecessors
                val predDominators = if (node.predecessors.isNotEmpty() && node != cfg.entry)
                    (node.predecessors.map { pred -> nodeToDominators[pred]!! }).reduce { acc, t -> acc.intersect(t) }
                else emptySet()

                val newDominators = predDominators union setOf(node)
                if (prevDominators != newDominators) changed = true

                nodeToDominators[node] = newDominators
            }
        }
        // Create the map from node -> {n | node dominates n}
        nodeToDominators.forEach { (node, dominators) ->
            dominators.forEach { nodeToDominated[it] = nodeToDominated[it]!!.union(setOf(node)) }
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
    private fun getDominatorTrees(cfg: CFG): DominatorTree {
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


    /**
     * Returns the dominator frontier for the given cfg node
     * A's dominance frontier contains B iff A does not strictly dominate B, but A does dominate some predecessor of B
     */
    private fun computeDominanceFrontier(cfgNode: CFGNode, cfg: CFG): Set<CFGNode> {
        val dominanceFrontier = mutableSetOf<CFGNode>()
        val nodeToDominated = getDominators(cfg = cfg).second
        val dominatedNodes = nodeToDominated[cfgNode]!!

        // Add the successors, but not anything that is dominated
        dominatedNodes.forEach { dom -> dominanceFrontier.addAll(dom.successors) }
        dominanceFrontier.removeAll(dominatedNodes)

        return dominanceFrontier
    }

    /** Builds and returns a map for function CFGs to DominatorTrees */
    fun getDominatorTrees(program: CFGProgram): Map<String, DominatorTree> =
        program.graphs.associate { cfg -> cfg.function.name to getDominatorTrees(cfg = cfg) }

    /** Returns a DominatorMap for each CFG. The DominatorMap maps a cfg node to the nodes it dominates */
    fun getDominators(program: CFGProgram): Map<String, DominatorMap> =
        program.graphs.associate { cfg -> cfg.function.name to getDominators(cfg = cfg).first }

    fun getDominated(program: CFGProgram): Map<String, DominatorMap> =
        program.graphs.associate { cfg -> cfg.function.name to getDominators(cfg = cfg).second }

    /** Returns a DominanceFrontier for each CFG. The DominanceFrontier maps a cfg node to the nodes in its frontier */
    fun getDominanceFrontiers(program: CFGProgram): Map<String, DominatorMap> =
        program.graphs.associate { cfg ->
            cfg.function.name to cfg.nodes.associateWith { computeDominanceFrontier(cfgNode = it, cfg = cfg) }
        }
}

/** [DominatorTree] represents the dominator tree for a given CFG */
data class DominatorTree(
    val cfg: CFG,
    val entry: DominatorTreeNode,
    val nodes: Set<DominatorTreeNode>
) {
    fun prettyPrint(sb: StringBuilder): StringBuilder {
        nodes.forEach { node ->
            sb.appendLine("$node -> ${node.dominates}")
        }
        return sb
    }
}

/** [DominatorTreeNode] represents a node in the dominator tree */
class DominatorTreeNode(
    val cfgNode: CFGNode,
) {
    // The list of nodes that this node is immediately dominates/is dominated by
    var dominates = mutableSetOf<DominatorTreeNode>()
    lateinit var dominated: Set<DominatorTreeNode>

    override fun toString(): String {
        return cfgNode.name
    }
}


fun Map<String, DominatorMap>.prettyPrintMaps(): String {
    val sb = StringBuilder()
    sb.appendLine("---Dominators---")
    forEach { (func, map) ->
        sb.appendLine("Function $func:")
        map.forEach { (node, dominatedNodes) ->
            sb.appendLine("${node.name}: ${dominatedNodes.map { it.name }}")
        }
        sb.appendLine()
    }
    return sb.toString()
}

fun Map<String, DominatorTree>.prettyPrintTrees(): String {
    val sb = StringBuilder()
    sb.appendLine("---Dominator tree---")
    forEach { (func, tree) ->
        sb.appendLine("Function $func:")
        tree.prettyPrint(sb = sb)
        sb.appendLine()
    }
    return sb.toString()
}

fun Map<String, Map<CFGNode, Set<CFGNode>>>.prettyPrintFrontiers(): String {
    val sb = StringBuilder()
    sb.appendLine("---Dominance Frontiers---")
    forEach { (func, map) ->
        sb.appendLine("Function $func:")
        map.forEach { (node, dominatedNodes) ->
            sb.appendLine("${node.name}: ${dominatedNodes.map { it.name }}")
        }
        sb.appendLine()
    }
    return sb.toString()
}
