package climbers

import trees.*
import util.BasicBlock
import util.BlockSetter

class DCEClimber : Climber {

    override fun applyToProgram(program: CookedProgram): CookedProgram = globalDCE(forwardLocalDCE(program))

    /**
     * This pass is stronger than local DCE in that it can view the entire program, but weaker in that if it sees
     * any variable being used, it preserves all definitions of it.
     */
    private fun globalDCE(program: CookedProgram): CookedProgram {
        val used = hashSetOf<String>()
        program.functions.forEach { function ->
            function.instructions.filterIsInstance<ReadInstruction>().forEach {
                used.addAll(it.args)
            }
        }
        return CookedProgram(program.functions.map { function ->
            CookedFunction(function.name, function.args, function.type,
                function.instructions.filter {
                    when (it) {
                        is WriteInstruction -> it.dest in used
                        else -> true
                    }
                }
            )
        })
    }

    private fun forwardLocalDCE(program: CookedProgram): CookedProgram =
        BlockSetter().applyToProgramBlocks(program, ::forward)

    private fun reverseLocalDCE(program: CookedProgram): CookedProgram =
        BlockSetter().applyToProgramBlocks(program, this::reverse)

    private fun forward(basicBlock: BasicBlock): BasicBlock {
        var instructions = basicBlock.instructions
        var result = arrayListOf<CookedInstructionOrLabel>()
        var changed = true
        while (changed) {
            changed = false
            val unusedDefs: MutableMap<String, CookedInstruction> = hashMapOf()
            instructions.forEach { instr ->
                result.add(instr)
                if (instr is ReadInstruction) {
                    instr.args.forEach { unusedDefs.remove(it) }
                }
                if (instr is WriteInstruction) {
                    if (instr.dest in unusedDefs) {
                        result.removeAt(result.lastIndexOf(unusedDefs[instr.dest]!!))
                        changed = true
                    }
                    unusedDefs[instr.dest] = instr
                }
            }
            instructions = result
            result = arrayListOf()
        }
        return BasicBlock(instructions = instructions)
    }

    private fun reverse(basicBlock: BasicBlock): BasicBlock {
        var instructions = basicBlock.instructions
        var result = arrayListOf<CookedInstructionOrLabel>()
        var changed = true
        while (changed) {
            changed = false
            val deadVars = hashSetOf<String>()
            instructions.reversed().forEach { instr ->
                result.add(instr)
                if (instr is ReadInstruction) {
                    deadVars.removeAll(instr.args.toSet())
                }
                if (instr is WriteInstruction) {
                    if (instr.dest in deadVars) {
                        result.removeLast()
                        changed = true
                    }
                    deadVars.add(instr.dest)
                }
            }
            instructions = result.reversed()
            result = arrayListOf()
        }
        return BasicBlock(instructions = instructions)
    }
}