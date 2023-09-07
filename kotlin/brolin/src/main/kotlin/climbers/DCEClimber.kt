package climbers

import trees.*

class DCEClimber : Climber {

    override fun applyToProgram(program: CookedProgram): CookedProgram = forwardDCEprogram(program)

    fun forwardDCEprogram(program: CookedProgram): CookedProgram = BlockSetter().applyToProgramBlocks(program, ::forwardDCE)

    fun reverseDCEprogram(program: CookedProgram): CookedProgram = BlockSetter().applyToProgramBlocks(program, ::reverseDCE)

    fun forwardDCE(basicBlock: BasicBlock): BasicBlock {
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

    fun reverseDCE(basicBlock: BasicBlock): BasicBlock {
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