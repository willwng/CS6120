package climbers

import trees.*

data class BasicBlock(val instructions: List<CookedInstructionOrLabel>)
typealias BlockedFunction = Pair<CookedFunction, List<BasicBlock>>
typealias BlockedProgram = List<BlockedFunction>

private fun BlockedProgram.toCookedProgram(): CookedProgram =
    CookedProgram(this.map { (originalFunction, blocks) ->
        CookedFunction(
            originalFunction.name,
            originalFunction.args,
            originalFunction.type,
            blocks.map { block -> block.instructions }.flatten()
        )
    })

/** Setters create the routes upon which climbers operate. */
class BlockSetter {
    private val terminators = listOf(Operator.JMP, Operator.RET, Operator.BR)

    /** Takes a function at the granularity of basic blocks and returns the result of applying it to the program */
    fun applyToProgramBlocks(program: CookedProgram, f: (block: BasicBlock) -> BasicBlock): CookedProgram =
        block(program).map { (function, blocks) -> Pair(function, blocks.map(f)) }.toCookedProgram()

    private fun block(program: CookedProgram): BlockedProgram =
        program.functions.map { function -> Pair(function, block(function)) }

    private fun block(function: CookedFunction): List<BasicBlock> {
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
