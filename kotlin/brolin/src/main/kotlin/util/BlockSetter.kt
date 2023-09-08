package util

import trees.*

data class BasicBlock(val instructions: List<CookedInstructionOrLabel>)

data class BlockedProgram(val blockedFunctions: List<BlockedFunction>) {
    /** Returns the CookedProgram represented by the BlockedProgram */
    fun toCookedProgram(): CookedProgram =
        CookedProgram(blockedFunctions.map { (originalFunction, blocks) ->
            CookedFunction(
                originalFunction.name,
                originalFunction.args,
                originalFunction.type,
                blocks.map { block -> block.instructions }.flatten()
            )
        })
}

data class BlockedFunction(
    /** The function from which this blocked function was constructed. */
    val function: CookedFunction,
    val basicBlocks: List<BasicBlock>
)

/** Setters create the routes upon which climbers operate. */
class BlockSetter {
    // Operations that have more complicated control-flow (signifies end of block)
    private val terminators = listOf(Operator.JMP, Operator.RET, Operator.BR)

    /** Takes a function [f] at the granularity of basic blocks and returns the result of applying it to the program */
    fun applyToProgramBlocks(program: CookedProgram, f: (block: BasicBlock) -> BasicBlock): CookedProgram =
        BlockedProgram(
            block(program).blockedFunctions.map { (function, blocks) ->
                BlockedFunction(function = function, basicBlocks = blocks.map(f))
            }).toCookedProgram()

    /** Converts [program] into a BlockedProgram with BlockedFunctions */
    fun block(program: CookedProgram): BlockedProgram =
        BlockedProgram(blockedFunctions = program.functions.map { function ->
            BlockedFunction(
                function,
                block(function)
            )
        })

    /** Converts [function] into a list of basic blocks, which are guaranteed to be nonempty */
    fun block(function: CookedFunction): List<BasicBlock> {
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
        return blocks.filter{ it.instructions.isNotEmpty() }
    }
}
