package trees

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The internal representation of a Bril program. */

@Serializable
data class CookedProgram(val functions: List<CookedFunction>)

/** A Function object represents a (first-order) procedure consisting of a sequence of instructions */
@Serializable
data class CookedFunction(
    val name: String,
    val args: List<Argument> = listOf(),
    val type: Type? = null,
    @SerialName("instrs") val instructions: List<CookedInstructionOrLabel> = listOf(),
) : SourcedObject()

sealed interface CookedInstructionOrLabel

@Serializable
data class CookedLabel(val label: String) : CookedInstructionOrLabel, SourcedObject()

/** An Instruction represents a unit of computational work */
@Serializable
sealed interface CookedInstruction: CookedInstructionOrLabel {
    val op: Operator
}

sealed interface WriteInstruction : CookedInstruction {
    val dest: String
    val type: Type
}

sealed interface ReadInstruction : CookedInstruction {
    val args: List<String>
}

/** A Constant is an instruction that produces a literal value */
data class ConstantInstruction(
    override val op: Operator = Operator.CONST,
    override val dest: String,
    override val type: Type,
    val value: Value,
) : CookedInstruction, WriteInstruction

/** An Effect Operation is like a Value Operation, but it does not produce a value. */
data class EffectOperation(
    override val op: Operator,
    override val args: List<String> = listOf(),
    val funcs: List<String> = listOf(),
    val labels: List<String> = listOf(),
) : CookedInstruction, ReadInstruction

data class ValueOperation(
    override val op: Operator,
    override val dest: String,
    override val type: Type,
    override val args: List<String> = listOf(),
    val funcs: List<String> = listOf(),
    val labels: List<String> = listOf()
) : CookedInstruction, ReadInstruction, WriteInstruction

@Serializable
enum class Operator {
    CONST, ADD, MUL, SUB, DIV, EQ, LT, GT, LE, GE, NOT, AND, OR, JMP, BR, CALL, RET, ID, PRINT, NOP,

    // Extensions
    PHI, ALLOC, STORE, LOAD, FREE, PTRADD
}

interface Value

@Serializable
data class BooleanValue(val value: Boolean) : Value

@Serializable
data class IntValue(val value: Int) : Value

@Serializable
data class FloatValue(val value: Float) : Value
