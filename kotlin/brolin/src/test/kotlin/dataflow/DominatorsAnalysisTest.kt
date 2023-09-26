package dataflow

import TestFixture
import analysis.DominatorMap
import analysis.DominatorsAnalysis
import org.junit.jupiter.api.Test
import util.CFG
import util.CFGNode
import util.CFGProgram
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DominatorsAnalysisTest {

    /** Returns all paths from [currNode] to [targetNode] in the [cfg] */
    private fun getAllPaths(
        cfg: CFG, currNode: CFGNode, targetNode: CFGNode, prevPath: MutableSet<CFGNode>
    ): List<MutableSet<CFGNode>> {
        val currPath = prevPath.union(setOf(currNode)).toMutableSet()
        if (currNode == targetNode) {
            return listOf(currPath)
        }
        val paths = arrayListOf<MutableSet<CFGNode>>()
        currNode.successors.forEach { succ ->
            if (succ !in currPath) {
                val succPaths = getAllPaths(cfg = cfg, currNode = succ, targetNode = targetNode, prevPath = currPath)
                paths.addAll(succPaths)
            }
        }
        return paths
    }

    /** For a given CFG and DominatorMap, check that all paths from entry to each node contains the dominators */
    private fun checkCFGDominators(cfg: CFG, dominatorMap: DominatorMap) {
        dominatorMap.forEach { (node, dom) ->
            val paths = getAllPaths(cfg = cfg, currNode = cfg.entry, targetNode = node, prevPath = mutableSetOf())
            // If paths is empty, then the node must be unreachable
            if (paths.isNotEmpty()) {
                val commonNodes = paths.reduce { acc, t -> acc.intersect(t).toMutableSet() }
                assert(dom == commonNodes)
            }
        }
    }

    /** Checks that the two maps representing dominators are consistent */
    private fun checkDominatorMapConsistency(dominatorMap: DominatorMap, dominatedMap: DominatorMap) {
        dominatorMap.forEach { (node, dominated) ->
            dominated.forEach {
                assert(node in dominatedMap[it]!!)
            }
        }
    }


    private fun checkDominators(cfgProgram: CFGProgram) {
        val nodeToDominated = DominatorsAnalysis.getDominated(program = cfgProgram)
        val nodeToDominators = DominatorsAnalysis.getDominators(program = cfgProgram)
        cfgProgram.graphs.forEach { cfg ->
            val funcName = cfg.function.name
            println("\t Running dominator test for $funcName")
            assertNotNull(nodeToDominated[funcName])
            assertNotNull(nodeToDominators[funcName])
            println("\t\t Checking paths agree")
            checkCFGDominators(cfg = cfg, dominatorMap = nodeToDominators[funcName]!!)
            println("\t\t Checking map consistency")
            checkDominatorMapConsistency(
                dominatorMap = nodeToDominators[funcName]!!,
                dominatedMap = nodeToDominated[funcName]!!
            )
        }
    }

    /** Checks that the dominance frontier satisfies proper conditions for the given CFG */
    private fun checkDominanceFrontiersCFG(
        cfg: CFG,
        frontiers: Map<CFGNode, Set<CFGNode>>,
        dominatorMap: DominatorMap
    ) {
        cfg.nodes.forEach { node ->
            val dominated = dominatorMap[node]!!
            val frontier = frontiers[node]!!
            // Ensure all the dominated nodes are not in the frontier, only the successors
            if(dominated.intersect(frontier) != emptySet<CFGNode>()) {
                print("")
            }
            assertEquals(dominated.intersect(frontier), emptySet())
        }
    }

    private fun checkDominanceFrontiersProgram(cfgProgram: CFGProgram) {
        val dominanceFrontiers = DominatorsAnalysis.getDominanceFrontiers(program = cfgProgram)
        val nodeToDominated = DominatorsAnalysis.getDominated(program = cfgProgram)
        cfgProgram.graphs.forEach { cfg ->
            val funcName = cfg.function.name
            checkDominanceFrontiersCFG(
                cfg = cfg,
                frontiers = dominanceFrontiers[funcName]!!,
                dominatorMap = nodeToDominated[funcName]!!
            )
        }
    }

    @Test
    fun checkDominanceFrontiers() {
        TestFixture.cfgPrograms.forEach { (file, cfgProgram) ->
            println("Checking dominance frontier: $file")
            checkDominanceFrontiersProgram(cfgProgram = cfgProgram)
        }
    }


    @Test
    fun getDominatorsTest() {
        TestFixture.cfgPrograms.forEach { (file, cfgProgram) ->
            println("Checking dominators: $file")
            checkDominators(cfgProgram)
        }
    }
}