package climbers

import trees.*

data class BasicBlock(val instructions: List<CookedInstructionOrLabel>)

class BlockClimber {
    private val terminators = listOf(Operator.JMP, Operator.RET, Operator.BR)

    fun basicBlocker(program: CookedProgram): List<List<BasicBlock>> {
        return program.functions.map(::basicBlocker)
    }

    private fun basicBlocker(function: CookedFunction): List<BasicBlock> {
        val blocks: ArrayList<BasicBlock> = arrayListOf()
        var currentBlock: ArrayList<CookedInstructionOrLabel> = arrayListOf()
        function.instructions.forEach { instr ->
            when (instr) {
                is CookedInstruction -> {
                    currentBlock.add(instr)

                    if (instr.op in terminators) {
                        blocks.add(BasicBlock(currentBlock))
                        currentBlock = arrayListOf()
                    }
                }
                // Handle labels
                else -> {
                    instr as CookedLabel
                    if (currentBlock.isNotEmpty()) {
                        val basicBlock = BasicBlock(instructions = currentBlock)
                        blocks.add(basicBlock)
                    }
                    currentBlock = arrayListOf(instr)
                }
            }
        }
        // Case of no terminators at the end
        if (currentBlock.isNotEmpty()) {
            blocks.add(BasicBlock(currentBlock))
        }
        return blocks
    }
}
