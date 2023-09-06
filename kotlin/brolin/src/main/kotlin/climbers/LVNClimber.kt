package climbers

import trees.*

sealed interface BrilValue

data class Const(val value: Value) : BrilValue

/** Represents a computed value in a Bril program. */
data class ValueTuple(val op: Operator, val args: List<Int>) : BrilValue

class LVNClimber {
    /*
    for instr in block:
        value = (instr.op, var2num[instr.args[0]], ...)
        if value in table:
            # The value has been computed before; reuse it.
            num, var = table[value]
            replace instr with copy of var
        else:
            # A newly computed value.
            num = fresh value number
            dest = instr.dest
            if instr will be overwritten later:
                 dest = fresh variable name
                 instr.dest = dest
            else:
                 dest = instr.dest
            table[value] = num, dest
            for a in instr.args:
                replace a with table[var2num[a]].var
        var2num[instr.dest] = num
     */
    private fun lvn(basicBlock: BasicBlock): BasicBlock {
        val tupleToNum: MutableMap<BrilValue, Int> = hashMapOf()  // map to value numbers
        val numToTupleCanonicalVar: ArrayList<Pair<BrilValue, String>> =
            arrayListOf()  // map to value tuples, where index is value number
        val env: MutableMap<String, Int> = hashMapOf()  // mapping from variable names to current value number

        fun processValue(
            inst: WriteInstruction,
            value: BrilValue,
            blockBuilder: MutableList<CookedInstructionOrLabel>
        ) {
            // Add to environment. if not in table, add new row and add the instruction.
            // if in table, replace the instruction with dest = id var.
            val index = tupleToNum.getOrPut(value) { numToTupleCanonicalVar.size }
            env[inst.dest] = index
            if (index == numToTupleCanonicalVar.size) {  // We added to the table
                numToTupleCanonicalVar.add(Pair(value, inst.dest))
                blockBuilder.add(inst)
                // TODO: This is wrong, instead we want to add a new version of the instruction which uses the
                //  canonical variable
            } else {
                blockBuilder.add(
                    ValueOperation(
                        op = Operator.ID,
                        dest = inst.dest,
                        type = inst.type,
                        args = listOf(numToTupleCanonicalVar[index].second)
                    )
                )
            }
        }

        val result: ArrayList<CookedInstructionOrLabel> =
            basicBlock.instructions.fold(initial = arrayListOf()) { acc, inst ->
                when (inst) {
                    is ConstantInstruction -> {
                        processValue(inst, Const(inst.value), acc)
                    }
                    is ValueOperation -> {
                        // TODO: Consider overwriting variables. At overwrite spot, make fresh canonical var to use for
                        //  old value from now on
                        when (inst.op) {
                            Operator.ID -> {
                                val copy = env[inst.args[0]]
                                assert(copy != null)  // We should be referencing an existing variable
                                env[inst.dest] = copy!!
                                acc.add(inst)
                                // TODO: We don't really need to add this, since if we use the first binding of a value
                                //  as its canonical variable then the dest of this instruction will never be useful.
                                //  But we may decide to reassign canonical variables later, so maybe it's just better
                                //  to handle this with DCE
                            }
                            Operator.CALL -> {
                                acc.add(inst)
                                // TODO: Other side-effect-y operators here
                            }
                            else -> {
                                processValue(
                                    inst,
                                    ValueTuple(
                                        op = inst.op,
                                        // TODO: Account for commutativity here
                                        args = inst.args.map {
                                            assert(env[it] != null)
                                            env[it]!!
                                        }),
                                    acc
                                )
                            }
                        }
                    }
                    is EffectOperation, is CookedLabel -> {
                        acc.add(inst)
                    }
                }
                acc
            }
        return BasicBlock(result)
    }
}

