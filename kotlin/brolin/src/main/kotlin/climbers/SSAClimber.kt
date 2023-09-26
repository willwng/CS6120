package climbers

import analysis.DominatorMap
import analysis.DominatorTree
import analysis.DominatorsAnalysis
import analysis.TreeTranslator
import trees.CookedProgram
import trees.ReadInstruction
import trees.WriteInstruction
import util.CFG
import util.CFGNode
import util.CFGProgram


object SSAClimber : Climber {

    override fun applyToProgram(program: CookedProgram): CookedProgram {
        val cfgProgram = CFGProgram.of(program)
        val dominanceFrontiers = DominatorsAnalysis.getDominanceFrontiers(cfgProgram)
        val dominatorTrees = DominatorsAnalysis.getDominatorTrees(cfgProgram)
        cfgProgram.graphs.forEach { cfg ->
            val name = cfg.function.name
            convertToSSA(
                cfg = cfg,
                dominanceFrontier = dominanceFrontiers[name]!!,
                dominatorTree = dominatorTrees[name]!!.first,
                treeTranslator = dominatorTrees[name]!!.second
            )
        }
        return cfgProgram.toCookedProgram()
    }

    private fun insertPhiNodes(cfg: CFG, dominanceFrontier: DominatorMap, vars: Set<String>) {
        val assignedBlocks = mutableSetOf<CFGNode>()
        val varsToDef = vars.associateWith { v ->
            cfg.nodes.filter { node -> v in node.definedNames }.toMutableSet()
        }.toMutableMap()

        varsToDef.forEach { (v, defV) ->
            // Blocks where [v] is assigned
            defV.forEach { d ->
                // Dominance frontier of [d], add phi node if we haven't already
                dominanceFrontier[d]?.filter { it !in assignedBlocks }?.forEach { block ->
                    varsToDef[v]!!.add(block)
                    TODO("Add Φ node")
                }
            }
        }
    }

    private fun rename(node: CFGNode, dominatorTree: DominatorTree, treeTranslator: TreeTranslator) {
        node.block.instructions.forEach { instr ->
            // replace each argument to instr with stack[old name]
            if (instr is ReadInstruction) {
                instr.args.map { }
            }
            // replace the destination with a new name
            // push that new name onto stack[old name]
            if (instr is WriteInstruction) {
                val oldName = instr.dest
                TODO()
            }
        }

        node.successors.forEach { s ->
            // for p in s's ϕ-nodes:
            //  Assuming p is for a variable v, make it read from stack[v].
            TODO()
        }

        treeTranslator[node]!!.dominates.forEach {
            rename(node = it.cfgNode, dominatorTree = dominatorTree, treeTranslator = treeTranslator)
        }
    }

    private fun convertToSSA(
        cfg: CFG,
        dominanceFrontier: DominatorMap,
        dominatorTree: DominatorTree,
        treeTranslator: TreeTranslator
    ) {
        // Gather all the variable definitions and the blocks which define them
        val vars = cfg.nodes.map { node -> node.definedNames }.fold(emptySet<String>()) { acc, defs -> acc.union(defs) }
        insertPhiNodes(cfg = cfg, dominanceFrontier = dominanceFrontier, vars = vars)

        // stack[v] is a stack of variable names (for every variable v)
        val stack = mapOf<String, ArrayDeque<String>>().withDefault { ArrayDeque() }
        rename(node = cfg.entry, dominatorTree = dominatorTree, treeTranslator = treeTranslator)
    }

}
