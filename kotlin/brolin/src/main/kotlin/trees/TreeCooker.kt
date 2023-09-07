package trees

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.float
import kotlinx.serialization.json.floatOrNull

/** The TreeCooker RawTrees into CookedTrees */
object TreeCooker {

    fun cookProgram(rawProgram: RawProgram): CookedProgram =
        CookedProgram(functions = rawProgram.rawFunctions.map { func -> cookFunction(func) })

    private fun cookFunction(rawFunction: RawFunction): CookedFunction =
        CookedFunction(
            rawFunction.name,
            rawFunction.args,
            rawFunction.type,
            rawFunction.instructions.map { cookInstructionOrLabel(it) })

    private fun cookInstructionOrLabel(instructionOrLabel: RawInstructionOrLabel): CookedInstructionOrLabel =
        when (instructionOrLabel) {
            is RawLabel -> CookedLabel(instructionOrLabel.label)
            is RawInstruction -> cookInstruction(instructionOrLabel)
            else -> throw (Error("Unsupported raw type"))
        }

    private fun cookInstruction(rawInstruction: RawInstruction): CookedInstruction {
        when {
            // Constant instructions
            rawInstruction.op == "const" -> {
                assert(rawInstruction.dest != null && rawInstruction.type != null && rawInstruction.jsonValue != null)
                return ConstantInstruction(
                    dest = rawInstruction.dest!!,
                    type = rawInstruction.type!!,
                    value = cookValue(rawInstruction.type, rawInstruction.jsonValue!!)
                )
            }
            // Effect operation
            rawInstruction.dest == null -> {
                return EffectOperation(
                    op = cookOp(rawInstruction.op),
                    args = rawInstruction.args,
                    funcs = rawInstruction.funcs,
                    labels = rawInstruction.labels
                )

            }
            // Value operation
            else -> {
                assert(rawInstruction.type != null)
                return ValueOperation(
                    op = cookOp(rawInstruction.op),
                    dest = rawInstruction.dest,
                    type = rawInstruction.type!!,
                    args = rawInstruction.args,
                    funcs = rawInstruction.funcs,
                    labels = rawInstruction.labels
                )
            }
        }
    }

    private fun cookOp(op: String): Operator =
        when (op) {
            "const" -> Operator.CONST
            "add" -> Operator.ADD
            "sub" -> Operator.SUB
            "mul" -> Operator.MUL
            "div" -> Operator.DIV
            "eq" -> Operator.EQ
            "lt" -> Operator.LT
            "gt" -> Operator.GT
            "le" -> Operator.LE
            "ge" -> Operator.GE
            "not" -> Operator.NOT
            "and" -> Operator.AND
            "or" -> Operator.OR
            "jmp" -> Operator.JMP
            "br" -> Operator.BR
            "call" -> Operator.CALL
            "ret" -> Operator.RET
            "id" -> Operator.ID
            "print" -> Operator.PRINT
            "nop" -> Operator.NOP
            // Extensions
            "fadd" -> Operator.FADD
            "fmul" -> Operator.FMUL
            "fdiv" -> Operator.FDIV
            "fsub" -> Operator.FSUB
            "feq" -> Operator.FEQ
            "flt" -> Operator.FLT
            "fgt" -> Operator.FGT
            "fle" -> Operator.FLE
            "fge" -> Operator.FGE
            "alloc" -> Operator.ALLOC
            "free" -> Operator.FREE
            "ptradd" -> Operator.PTRADD
            "load" -> Operator.LOAD
            "store" -> Operator.STORE
            "phi" -> Operator.PHI
            else -> throw (Error("Unsupported operator: $op"))
        }

    private fun cookValue(type: Type, jsonValue: JsonPrimitive): Value {
        return when {
            jsonValue.content == "true" -> BooleanValue(value = true)
            jsonValue.content == "false" -> BooleanValue(value = false)
            jsonValue.content.toBigIntegerOrNull() != null && type.type == "\"int\"" -> IntValue(value = jsonValue.content.toBigInteger())
            jsonValue.floatOrNull != null -> FloatValue(value = jsonValue.float)
            else -> throw (Error("Unsupported value: $jsonValue"))
        }
    }
}