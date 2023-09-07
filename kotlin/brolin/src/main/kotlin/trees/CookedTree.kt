/**
 * A more appealing tree, easily manipulated and serializable
 */
package trees

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigInteger

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

@Serializable(with = CookedInstructionOrLabelSerializer::class)
sealed interface CookedInstructionOrLabel

@Serializable
data class CookedLabel(val label: String) : CookedInstructionOrLabel, SourcedObject()

/** An Instruction represents a unit of computational work */
@Serializable(with = CookedInstructionSerializer::class)
sealed interface CookedInstruction : CookedInstructionOrLabel {
    val op: Operator
}

/** Instructions that define a variable */
sealed interface WriteInstruction : CookedInstruction {
    val dest: String
    val type: Type
}

/** Instructions that use a variable */
sealed interface ReadInstruction : CookedInstruction {
    val args: List<String>
}

@Serializable(with = ConstantInstructionSerializer::class)
/** A Constant is an instruction that produces a literal value */
data class ConstantInstruction(
    override val op: Operator = Operator.CONST,
    override val dest: String,
    override val type: Type,
    val value: Value,
) : CookedInstruction, WriteInstruction

/** An Effect Operation is like a Value Operation, but it does not produce a value. */
@Serializable(with = EffectOperationSerializer::class)
data class EffectOperation(
    override val op: Operator,
    override val args: List<String> = listOf(),
    val funcs: List<String> = listOf(),
    val labels: List<String> = listOf(),
) : CookedInstruction, ReadInstruction

/** A Value Operation is an instruction that takes arguments, does some computation, and produces a value */
@Serializable(with = ValueOperationSerializer::class)
data class ValueOperation(
    override val op: Operator,
    override val dest: String,
    override val type: Type,
    override val args: List<String> = listOf(),
    val funcs: List<String> = listOf(),
    val labels: List<String> = listOf()
) : CookedInstruction, ReadInstruction, WriteInstruction {
    override fun toString() = "$dest: $type = $op $args"

    fun withArgs(newArgs: List<String>) = ValueOperation(op, dest, type, newArgs, funcs, labels)
}

@Serializable(with = OperatorSerializer::class)
enum class Operator(val commutative: Boolean = false) {
    CONST,
    ADD(true), MUL(true), EQ(true), AND(true), OR(true),
    SUB, DIV, LT, GT, LE, GE, NOT, JMP, BR, CALL, RET, ID,

    PRINT, NOP,

    // Extensions
    FADD(true), FMUL(true), FDIV, FSUB, FEQ(true), FGT, FLT, FGE, FLE,
    PHI, ALLOC, STORE, LOAD, FREE, PTRADD;
}


/** The types of values/literals that appear as constants */
@Serializable(with = ValueSerializer::class)
sealed interface Value

@Serializable
data class BooleanValue(val value: Boolean) : Value

data class IntValue(val value: BigInteger) : Value

@Serializable
data class FloatValue(val value: Float) : Value