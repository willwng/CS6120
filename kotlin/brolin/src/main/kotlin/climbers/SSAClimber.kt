package climbers

import analysis.DominatorMap
import analysis.DominatorTree
import analysis.DominatorsAnalysis
import analysis.TreeTranslator
import trees.*
import util.CFG
import util.CFGNode
import util.CFGProgram
import util.FreshNameGearLoop

typealias PhiBlockTranslator = Map<CFGNode, PhiBlock>
typealias StackVarMap = Map<String, ArrayDeque<String>>

private fun PhiBlockTranslator.translate(cfgNode: CFGNode): PhiBlock {
    return get(cfgNode)!!
}

/** A wrapper for CFGNodes: represents the node and any phi instructions added to it*/
data class PhiBlock(
    val cfgNode: CFGNode,
    val phiNodes: MutableList<PhiNode>,
    val name: String = cfgNode.name
) {
    fun has(name: String): Boolean =
        phiNodes.any { it.varName == name }

    fun phiOf(name: String): PhiNode? {
        return phiNodes.firstOrNull { it.varName == name }
    }

    companion object {
        fun of(cfgNode: CFGNode): PhiBlock {
            return PhiBlock(cfgNode = cfgNode, phiNodes = mutableListOf())
        }
    }
}

data class PhiNode(
    val originalVarName: String,
    var varName: String = originalVarName,
    val type: Type,
    /** CFGNode.name to variable name. Originally the source name, then new fresh ones */
    val labelToLastName: MutableMap<String, String>
) {
    fun toInstruction(): CookedInstruction {
        val (labels, args) = labelToLastName.toList().unzip()
        return ValueOperation(
            op = Operator.PHI,
            dest = varName,
            type = type,
            args = args,
            labels = labels
        )
    }
}

object SSAClimber : Climber {

    override fun applyToProgram(program: CookedProgram): CookedProgram {
        val cfgProgram = CFGProgram.of(program)
        val dominanceFrontiers = DominatorsAnalysis.getDominanceFrontiers(cfgProgram)
        val dominatorTrees = DominatorsAnalysis.getDominatorTrees(cfgProgram)
        val freshNameGearLoop = FreshNameGearLoop(program)
        cfgProgram.graphs.forEach { cfg ->
            val name = cfg.fnName
            convertToSSA(
                cfg = cfg,
                dominanceFrontier = dominanceFrontiers[name]!!,
                dominatorTree = dominatorTrees[name]!!.first,
                treeTranslator = dominatorTrees[name]!!.second,
                freshNames = freshNameGearLoop
            )
        }
        return cfgProgram.toCookedProgram()
    }

    private fun insertPhiNodes(cfg: CFG, dominanceFrontier: DominatorMap, vars: Set<String>): PhiBlockTranslator {
        // Wrap each cfg node into a PhiBlock
        val phiBlockTranslator = cfg.nodes.associateWith { PhiBlock.of(it) }

        // Get the PhiBlocks where each variable is possibly defined
        val varsToDef = vars.associateWith { v ->
            phiBlockTranslator.values.filter { node -> v in node.cfgNode.definedNames }.toSet().toMutableList()
        }.toMutableMap()

        varsToDef.forEach { (v, defV) ->
            // Blocks where [v] is assigned
            for (i in 0 until defV.size) {
                val d = defV[i]
                // Dominance frontier of [d], add phi node if we haven't already
                dominanceFrontier[d.cfgNode]?.mapNotNull { phiBlockTranslator[it] }
                    ?.forEach { block ->
                        if (block.has(v)) {
                            block.phiOf(v)?.labelToLastName?.set(d.name, v)
                        } else {
                            val type =
                                (d.cfgNode.block.instructions.first { insn -> insn is WriteInstruction && insn.dest == v } as WriteInstruction).type
                            block.phiNodes.add(
                                PhiNode(
                                    varName = v,
                                    type = type,
                                    labelToLastName = mutableMapOf(d.name to v)
                                )
                            )
                            defV.add(block)
                        }
                    }
            }
        }
        return phiBlockTranslator
    }

    private fun rename(
        vars: Set<String>,
        phiBlock: PhiBlock,
        dominatorTree: DominatorTree,
        treeTranslator: TreeTranslator,
        phiBlockTranslator: PhiBlockTranslator,
        freshNames: FreshNameGearLoop,
        stack: StackVarMap,
    ) {
        val pushedVars = vars.associateWith { 0 }.toMutableMap()

        // It is important that we rename phi nodes first. We always assume our RHS was updated by a predecessor
        // But here we make a fresh name for the LHS
        phiBlock.phiNodes.forEach { phiNode ->
            val oldName = phiNode.varName
            val newName = freshNames.get(oldName)
            phiNode.varName = newName
            stack[oldName]!!.addLast(newName)
            pushedVars[oldName] = pushedVars[oldName]!! + 1
        }

        val renamedInstructions = phiBlock.cfgNode.block.instructions.map {
            var instr = it
            // replace each argument to instr with stack[old name]
            if (instr is ReadInstruction) {
                instr = instr.withArgs(instr.args.map { arg -> stack[arg]?.last() ?: arg })
            }
            // replace the destination with a new name
            // push that new name onto stack[old name]
            if (instr is WriteInstruction) {
                val oldName = instr.dest
                val newName = freshNames.get(oldName)
                instr = instr.withDest(newName)
                stack[oldName]!!.addLast(newName)
                pushedVars[oldName] = pushedVars[oldName]!! + 1
            }
            instr
        }

        phiBlock.cfgNode.replaceInsns(renamedInstructions)

        // Update successors to read from the latest new value
        phiBlock.cfgNode.successors.forEach { s ->
            phiBlockTranslator[s]!!.phiNodes.forEach { p ->
                if (p.labelToLastName.containsKey(phiBlock.name))
                    p.labelToLastName[phiBlock.name] = stack[p.originalVarName]!!.last()
            }
        }

        // Rename blocks immediately dominated by this node
        treeTranslator[phiBlock.cfgNode]!!.dominates.forEach {
            rename(
                vars = vars,
                phiBlock = phiBlockTranslator.translate(it.cfgNode),
                dominatorTree = dominatorTree,
                treeTranslator = treeTranslator,
                phiBlockTranslator = phiBlockTranslator,
                freshNames = freshNames,
                stack = stack
            )
        }

        // Pop all the names we just pushed onto the stacks
        pushedVars.forEach { (varName, numPushes) -> repeat(numPushes) { stack[varName]!!.removeLast() } }

    }

    private fun convertToSSA(
        cfg: CFG,
        dominanceFrontier: DominatorMap,
        dominatorTree: DominatorTree,
        treeTranslator: TreeTranslator,
        freshNames: FreshNameGearLoop,
    ) {
        // Gather all the variable definitions and the blocks which define them
        val vars = cfg.nodes.map { node -> node.definedNames }.fold(emptySet<String>()) { acc, defs -> acc.union(defs) }
        val phiBlockTranslator = insertPhiNodes(cfg = cfg, dominanceFrontier = dominanceFrontier, vars = vars)

        // stack[v] is a stack of variable names (for every variable v)
        val stack = vars.associateWith { ArrayDeque<String>() }
        rename(
            vars = vars,
            phiBlock = phiBlockTranslator.translate(cfg.entry),
            dominatorTree = dominatorTree,
            treeTranslator = treeTranslator,
            phiBlockTranslator = phiBlockTranslator,
            freshNames = freshNames,
            stack = stack,
        )
//        phiBlockTranslator.values.forEach {
//            println(it.cfgNode.block)
//            println(it.phiNodes)
//        }

        cfg.nodes.forEach {
            val phiInstructions = phiBlockTranslator[it]!!.phiNodes.map { phi -> phi.toInstruction() }
            val newInstructions =
                if (it.block.instructions.firstOrNull() is CookedLabel)
                    listOf(it.block.instructions.first()) + phiInstructions + it.block.instructions.drop(1)
                else
                    phiInstructions + it.block.instructions
            it.replaceInsns(newInstructions)
        }

        println(cfg)
    }

}
