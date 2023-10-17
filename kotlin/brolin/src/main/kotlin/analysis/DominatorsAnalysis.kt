package analysis

import util.CFG
import util.CFGNode
import util.CFGProgram

typealias ImmediateDominatorMap = Map<CFGNode, Set<CFGNode>>
typealias DominatorMap = Map<CFGNode, Set<CFGNode>>
typealias DominatedMap = Map<CFGNode, Set<CFGNode>>
typealias TreeTranslator = Map<CFGNode, DominatorTreeNode>

object DominatorsAnalysis {

    /**
     * Builds a DominatorTree from the given dominators.
     * Requires dominators to be a map from nodes to its immediate dominators
     */
    private fun buildTree(cfg: CFG, dominators: ImmediateDominatorMap): Pair<DominatorTree, TreeTranslator> {
        // Convert each CFG node to a dominator tree node
        val dominatorNodesMap = cfg.nodes.associateWith { DominatorTreeNode(cfgNode = it) }
        // Populate the dominates/dominated field
        cfg.nodes.forEach { node ->
            val treeNode = dominatorNodesMap[node]!!
            val treeDominators = dominators[node]!!.map { dominatorNodesMap[it]!! }
            treeNode.dominates = treeDominators.toSet()
            treeDominators.forEach { it.dominators.add(treeNode) }
        }

        return Pair(
            DominatorTree(
                cfg = cfg,
                entry = dominatorNodesMap[cfg.entry]!!,
                nodes = dominatorNodesMap.values.toSet()
            ), dominatorNodesMap
        )
    }

    /** Gets the dominators/dominated for the given CFG (maps from node to its dominators) */
    private fun getDominates(cfg: CFG): Pair<DominatedMap, DominatorMap> {
        // Maintain a from a node to its dominators, and a map from a node to its dominated nodes
        val nodeToDominated = cfg.nodes.associateWith { _ -> cfg.nodes.toSet() }.toMutableMap()
        val nodeToDominators = cfg.nodes.associateWith { setOf<CFGNode>() }.toMutableMap()
        var changed = true
        while (changed) {
            changed = false
            cfg.nodes.forEach { node ->
                val prevDominators = nodeToDominated[node]
                // Determine the dominators of the node's predecessors
                val predDominators = if (node.predecessors.isNotEmpty() && node != cfg.entry)
                    (node.predecessors.map { pred -> nodeToDominated[pred]!! }).reduce { acc, t -> acc.intersect(t) }
                else emptySet()

                val newDominators = predDominators union setOf(node)
                if (prevDominators != newDominators) changed = true

                nodeToDominated[node] = newDominators
            }
        }
        // Create the map from node -> {n | node dominates n}
        nodeToDominated.forEach { (node, dominators) ->
            dominators.forEach { nodeToDominators[it] = nodeToDominators[it]!!.union(setOf(node)) }
        }
        return Pair(nodeToDominated, nodeToDominators)
    }

    /** Converts a dominator map to a strict dominator map */
    private fun DominatorMap.toStrict(): DominatorMap {
        val strictMap = this.toMutableMap()
        strictMap.forEach { (node, dom) -> strictMap[node] = dom.filter { it != node }.toSet() }
        return strictMap
    }

    /** Creates a dominator tree for the given CFG */
    private fun getDominatorTrees(cfg: CFG): Pair<DominatorTree, TreeTranslator> {
        val (nodeToDominators, nodeToDominated) = getDominates(cfg = cfg)
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
        val nodeToDominated = getDominates(cfg = cfg).second
        val dominatedNodes = nodeToDominated[cfgNode]!!

        // Add the successors, but not anything that is dominated
        dominatedNodes.forEach { dom -> dominanceFrontier.addAll(dom.successors) }
        dominanceFrontier.removeAll(dominatedNodes)

        // Edge case: A dominates predecessors of itself
        if (cfgNode in dominatedNodes.map { dom -> dom.successors }
                .fold(emptySet<CFGNode>()) { acc, cfgNodes -> acc + cfgNodes }) {
            dominanceFrontier.add(cfgNode)
        }

        return dominanceFrontier
    }

    /** Builds and returns a map for function CFGs to DominatorTrees */
    fun getDominatorTrees(program: CFGProgram): Map<String, Pair<DominatorTree, TreeTranslator>> =
        program.graphs.associate { cfg -> cfg.fnName to getDominatorTrees(cfg = cfg) }

    /** Returns a DominatorMap for each CFG. The DominatorMap maps a cfg node to the nodes it dominates */
    fun getDominatedMap(program: CFGProgram): Map<String, DominatedMap> =
        program.graphs.associate { cfg -> cfg.fnName to getDominates(cfg = cfg).first }

    /** Returns a DominatorMap for each CFG. The DominatorMap maps a cfg node to the nodes that dominate it */
    fun getDominatorsMap(program: CFGProgram): Map<String, DominatorMap> =
        program.graphs.associate { cfg -> cfg.fnName to getDominates(cfg = cfg).second }

    /** Returns a DominanceFrontier for each CFG. The DominanceFrontier maps a cfg node to the nodes in its frontier */
    fun getDominanceFrontiers(program: CFGProgram): Map<String, DominatorMap> =
        program.graphs.associate { cfg ->
            cfg.fnName to cfg.nodes.associateWith { computeDominanceFrontier(cfgNode = it, cfg = cfg) }
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
            sb.appendLine("$node -> ${node.dominators}")
        }
        return sb
    }
}

/** [DominatorTreeNode] represents a node in the dominator tree */
class DominatorTreeNode(
    val cfgNode: CFGNode,
) {
    // The list of nodes that this node is immediately dominates/is dominated by
    var dominators = mutableSetOf<DominatorTreeNode>()
    // Nodes that are immediately dominated by this
    lateinit var dominates: Set<DominatorTreeNode>

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

fun Map<String, Pair<DominatorTree, TreeTranslator>>.prettyPrintTrees(): String {
    val sb = StringBuilder()
    sb.appendLine("---Dominator tree---")
    forEach { (func, tt) ->
        sb.appendLine("Function $func:")
        tt.first.prettyPrint(sb = sb)
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
