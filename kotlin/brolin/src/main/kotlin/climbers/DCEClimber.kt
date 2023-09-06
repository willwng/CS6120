package climbers

import trees.CookedInstructionOrLabel
import trees.CookedProgram
import trees.ReadInstruction
import trees.WriteInstruction

class DCEClimber {

    fun trivialDCEProgram(program: CookedProgram): List<List<BasicBlock>> {
        val basicClimber = BlockClimber()
        return basicClimber.basicBlocker(program).map {
            it.map(::trivialDCE)
        }
    }

    private fun trivialDCE(basicBlock: BasicBlock): BasicBlock {
        var instructions = basicBlock.instructions
        var result = arrayListOf<CookedInstructionOrLabel>()
        val deadVars = hashSetOf<String>()
        var changed = true
        while (changed) {
            changed = false
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