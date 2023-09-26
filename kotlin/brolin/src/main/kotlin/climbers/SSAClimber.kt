package climbers

import analysis.DominatorMap
import analysis.DominatorsAnalysis
import trees.CookedProgram
import util.CFG
import util.CFGNode
import util.CFGProgram

object SSAClimber : Climber {

    override fun applyToProgram(program: CookedProgram): CookedProgram {
        val cfgProgram = CFGProgram.of(program)
        val dominanceFrontiers = DominatorsAnalysis.getDominanceFrontiers(cfgProgram)
        cfgProgram.graphs.forEach { cfg ->
            convertToSSA(
                cfg = cfg,
                dominanceFrontier = dominanceFrontiers[cfg.function.name]!!
            )
        }
        return cfgProgram.toCookedProgram()
    }

    private fun insertPhiNodes(cfg: CFG, dominanceFrontier: DominatorMap, vars: Set<String>) {
        val varsToDef = vars.associateWith { v -> cfg.nodes.filter { node -> v in node.definedNames } }

        varsToDef.forEach { (v, defV) ->
            // Blocks where [v] is assigned
            defV.forEach { d ->
                // Dominance frontier of [d]
                dominanceFrontier[d]?.forEach { block ->
                    TODO()
                }
            }
        }
    }

    private fun rename(block: CFGNode) {
        TODO()
    }

    private fun convertToSSA(cfg: CFG, dominanceFrontier: DominatorMap) {
        // Gather all the variable definitions and the blocks which define them
        val vars = cfg.nodes.map { node -> node.definedNames }.fold(emptySet<String>()) { acc, defs -> acc.union(defs) }
        insertPhiNodes(cfg = cfg, dominanceFrontier = dominanceFrontier, vars = vars)

        // varToStack[v] is a stack of variable names (for every variable v)
        val varToStack = mapOf<String, ArrayDeque<String>>().withDefault { ArrayDeque() }
        rename(cfg.entry)
    }

}
