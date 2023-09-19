package dataflow

import TestFixture
import org.junit.jupiter.api.Test
import util.CFG
import util.CFGNode
import util.CFGProgram
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
    private fun checkCFGDominators(cfg: CFG, dominatedMap: DominatorMap) {
        dominatedMap.forEach { (node, dom) ->
            val paths = getAllPaths(cfg = cfg, currNode = cfg.entry, targetNode = node, prevPath = mutableSetOf())
            // If paths is empty, then the node must be unreachable
            if (paths.isNotEmpty()) {
                val commonNodes = paths.reduce { acc, t -> acc.intersect(t).toMutableSet() }
                assert(dom == commonNodes)
            }
        }

    }

    private fun checkDominators(cfgProgram: CFGProgram) {
        val dominators = DominatorsAnalysis.getDominated(program = cfgProgram)
        cfgProgram.graphs.forEach { cfg ->
            println("\t Running dominator test for ${cfg.function.name}")
            assertNotNull(dominators[cfg.function.name])
            checkCFGDominators(cfg = cfg, dominatedMap = dominators[cfg.function.name]!!)
        }
    }


    @Test
    fun getDominatorsTest() {
        TestFixture.cfgPrograms.forEach { (file, cfgProgram) ->
            println("Checking $file")
            checkDominators(cfgProgram)
        }
    }
}