package climbers

import trees.*
import util.BasicBlock
import util.BlockSetter
import util.CFGProgram
import util.FreshNameGearLoop

sealed interface BrilValue

data class Const(val value: Value) : BrilValue

/** Represents a computed value in a Bril program. */
data class ValueTuple(val op: Operator, val args: List<Int>) : BrilValue

/** Represents the value of a variable defined outside the current block. */
data class Outsider(val variable: String) : BrilValue

/** Never equal to any other BrilValue. */
data class ImpureValue(private val i: Int = 0) : BrilValue {
    companion object {
        var i = 0
        fun get(): Int {
            i++
            return i
        }
    }

    constructor() : this(get())
}

object LVNClimber : CFGClimber {

    override fun applyToCFG(program: CFGProgram): CFGProgram {
        val freshNames = FreshNameGearLoop(program)
        program.graphs.forEach { cfg ->
            cfg.nodes.forEach { node -> node.replaceInsns(lvn(node.block, freshNames)) }
        }
        return program
    }

    private fun lvn(basicBlock: BasicBlock, freshNames: FreshNameGearLoop): List<CookedInstructionOrLabel> {
        val tupleToNum: MutableMap<BrilValue, Int> = hashMapOf()  // map to value numbers
        val numToValueCanonicalVar: ArrayList<Pair<BrilValue, String>> =
            arrayListOf()  // map to value tuples, where index is value number
        val env: MutableMap<String, Int> = hashMapOf()  // mapping from variable names to current value number

        /**
         * A stranger is an outsider we haven't seen before.
         * This function should be called upon the first reference to a variable which was defined outside the current
         * blocks.
         * At the end of this function, `env[name]` is guaranteed to not be null.
         */
        fun processStranger(name: String) {
            assert(env[name] == null)
            val stranger = Outsider(name)
            val num = tupleToNum.getOrPut(stranger) { numToValueCanonicalVar.size }
            assert(num == numToValueCanonicalVar.size)
            numToValueCanonicalVar.add(Pair(stranger, name))
            env[name] = num
        }

        /**
         * Adds `value` to the table if it is not in it already. Sets `env[inst.dest]` to the appropriate value number.
         * Adds `inst` to `blockBuilder`, possibly modified to use canonical variables.
         */
        fun processValue(
            inst: WriteInstruction,
            value: BrilValue,
            blockBuilder: MutableList<CookedInstructionOrLabel>
        ) {
            // It is safe to use the list size as the candidate fresh value number since we never remove elements
            val num = tupleToNum.getOrPut(value) { numToValueCanonicalVar.size }
            env[inst.dest] = num

            if (num == numToValueCanonicalVar.size) {
                // Newly computed value since we added to the table.
                numToValueCanonicalVar.add(Pair(value, inst.dest))
                // Add the instruction, modified to use canonical variables as arguments
                when (inst) {
                    is ConstantInstruction -> blockBuilder.add(inst)
                    is ValueOperation -> {
                        blockBuilder.add(
                            inst.withArgs(inst.args.map {
                                if (env[it] == null) processStranger(it)  // unreachable but safer to leave it
                                numToValueCanonicalVar[env[it]!!].second
                            })
                        )
                    }
                }
            } else {
                // The value has been computed before; reuse it.
                blockBuilder.add(
                    ValueOperation(
                        op = Operator.ID,
                        dest = inst.dest,
                        type = inst.type,
                        args = listOf(numToValueCanonicalVar[num].second)
                    )
                )
            }
        }

        /**
         * Sets `env[inst.dest]` to the appropriate value number and adds `inst` to `blockBuilder`, possibly modified to
         * use the canonical variable. Does not modify the table, unless the value we copy from is a stranger.
         */
        fun processCopy(
            inst: ValueOperation,
            blockBuilder: MutableList<CookedInstructionOrLabel>
        ) {
            assert(inst.op == Operator.ID && inst.args.size == 1)
            val name = inst.args[0]
            if (env[name] == null) processStranger(name)  // This is the first reference to a var defined outside
            env[inst.dest] = env[name]!!  // Add to environment; no need to add to the table
            blockBuilder.add(inst.withArgs(listOf(numToValueCanonicalVar[env[name]!!].second)))
        }

        val result: ArrayList<CookedInstructionOrLabel> =
            basicBlock.instructions.fold(initial = arrayListOf()) { acc, inst ->
                if (inst is WriteInstruction) {
                    val overwrittenValue = env[inst.dest]
                    if (overwrittenValue != null) {  // we are indeed overwriting something
                        val (value, oldVar) = numToValueCanonicalVar[overwrittenValue]
                        if (inst.dest == oldVar) {  // only need new var if overwriting canonical var
                            // Declare new canonical variable to be copy of old value
                            val newVar = freshNames.get(base = oldVar)
                            acc.add(
                                ValueOperation(
                                    op = Operator.ID,
                                    dest = newVar,
                                    type = inst.type,
                                    args = listOf(oldVar)
                                )
                            )
                            // Update table to use new canonical variable
                            numToValueCanonicalVar[overwrittenValue] = Pair(value, newVar)
                        }
                    }
                }

                when (inst) {
                    is ConstantInstruction -> processValue(inst, Const(inst.value), acc)
                    is ValueOperation -> {
                        when (inst.op) {
                            // Values with side effects. We create a dummy value to record in the environment that the
                            // variable has changed, so we erase any existing equivalences.
                            Operator.CALL, Operator.PHI, Operator.ALLOC, Operator.LOAD, Operator.PTRADD ->
                                processValue(inst, ImpureValue(), acc)
                            // Values that are always equivalent to an existing value
                            Operator.ID -> processCopy(inst, acc)
                            // Possibly new values
                            else -> {
                                processValue(
                                    inst,
                                    ValueTuple(
                                        op = inst.op,
                                        args = (if (inst.op.commutative) inst.args.sorted() else inst.args).map {
                                            if (env[it] == null) processStranger(it)
                                            env[it]!!
                                        }),
                                    acc
                                )
                            }
                        }
                    }
                    is CookedLabel -> acc.add(inst)
                    is EffectOperation -> acc.add(
                        EffectOperation(
                            op = inst.op,
                            args = inst.args.map {
                                if (env[it] == null) processStranger(it)
                                numToValueCanonicalVar[env[it]!!].second
                            },
                            funcs = inst.funcs,
                            labels = inst.labels
                        )
                    )
                }
                acc
            }
        return result
    }
}
