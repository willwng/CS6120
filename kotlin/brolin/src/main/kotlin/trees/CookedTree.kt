/**
 * A more appealing tree, easily manipulated and serializable. The main underlying representation of Bril programs.
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
sealed interface CookedInstructionOrLabel {
    /** For profiling output. */
    val count: Int?

    /** Returns if this instruction maintains control flow */
    fun isControlFlow(): Boolean {
        return (this is CookedInstruction && this.op in Operator.TERMINATORS)
    }

    fun isBranch(): Boolean {
        return (this is CookedInstruction && this.op == Operator.BR)
    }

    fun isPhi() = this is ValueOperation && this.op == Operator.PHI
}

@Serializable
data class CookedLabel(val label: String, override val count: Int? = null) : CookedInstructionOrLabel, SourcedObject() {
    override fun toString(): String {
        return "$label:"
    }
}

/** An Instruction represents a unit of computational work */
@Serializable(with = CookedInstructionSerializer::class)
sealed interface CookedInstruction : CookedInstructionOrLabel {
    val op: Operator

    fun isPure(): Boolean {
        return op !in Operator.IMPURE
    }

    fun clone(): CookedInstruction
}

/** Instructions that define a variable */
sealed interface WriteInstruction : CookedInstruction {
    val dest: String
    val type: Type

    fun withDest(dest: String): WriteInstruction =
        when (this) {
            is ConstantInstruction -> ConstantInstruction(this.op, dest, this.type, this.value, this.count)
            is ValueOperation -> ValueOperation(
                this.op,
                dest,
                this.type,
                this.args,
                this.funcs,
                this.labels,
                this.count
            )
        }
}

/** Instructions that use a variable */
sealed interface ReadInstruction : CookedInstruction {
    var args: List<String>

    fun withArgs(args: List<String>): ReadInstruction =
        when (this) {
            is EffectOperation -> EffectOperation(this.op, args, this.funcs, this.labels, this.count)
            is ValueOperation -> ValueOperation(
                this.op,
                this.dest,
                this.type,
                args,
                this.funcs,
                this.labels,
                this.count
            )
        }
}

@Serializable(with = ConstantInstructionSerializer::class)
/** A Constant is an instruction that produces a literal value */
class ConstantInstruction(
    override val op: Operator = Operator.CONST,
    override val dest: String,
    override val type: Type,
    val value: Value,
    override val count: Int? = null
) : CookedInstruction, WriteInstruction {
    override fun toString(): String {
        return "$dest: $type = const $value"
    }

    override fun clone(): CookedInstruction = this.withDest(this.dest)
}

/** An Effect Operation is like a Value Operation, but it does not produce a value. */
@Serializable(with = EffectOperationSerializer::class)
class EffectOperation(
    override val op: Operator,
    override var args: List<String> = listOf(),
    val funcs: List<String> = listOf(),
    var labels: List<String> = listOf(),
    override val count: Int? = null
) : CookedInstruction, ReadInstruction {
    override fun toString(): String {
        return "$op: $funcs $args $labels"
    }

    override fun clone(): CookedInstruction = this.withArgs(this.args)

    companion object {
        fun jump(label: String): EffectOperation {
            return EffectOperation(op = Operator.JMP, args = listOf(), funcs = listOf(), labels = listOf(label))
        }

        fun guard(arg: String, recover: String): EffectOperation {
            return EffectOperation(op = Operator.GUARD, args = listOf(arg), funcs = listOf(), labels = listOf(recover))
        }

        fun speculate(): EffectOperation {
            return EffectOperation(op = Operator.SPECULATE, args = listOf(), funcs = listOf(), labels = listOf())
        }

        fun commit(): EffectOperation {
            return EffectOperation(op = Operator.COMMIT, args = listOf(), funcs = listOf(), labels = listOf())
        }

        fun ret(): EffectOperation {
            return EffectOperation(op = Operator.RET, args = listOf(), funcs = listOf())
        }
    }
}

/** A Value Operation is an instruction that takes arguments, does some computation, and produces a value */
@Serializable(with = ValueOperationSerializer::class)
class ValueOperation(
    override val op: Operator,
    override val dest: String,
    override val type: Type,
    override var args: List<String> = listOf(),
    val funcs: List<String> = listOf(),
    var labels: List<String> = listOf(),
    override val count: Int? = null
) : CookedInstruction, ReadInstruction, WriteInstruction {
    override fun clone(): CookedInstruction = this.withArgs(this.args)

    override fun toString() = "$dest: $type = $op $funcs $args $labels"
}

@Serializable(with = OperatorSerializer::class)
enum class Operator(val commutative: Boolean = false) {
    CONST,
    ADD(true), MUL(true), EQ(true), AND(true), OR(true),
    SUB, DIV, LT, GT, LE, GE, NOT, JMP, BR, CALL, RET, ID,

    PRINT, NOP,

    // Extensions
    FADD(true), FMUL(true), FDIV, FSUB, FEQ(true), FGT, FLT, FGE, FLE,
    PHI, ALLOC, STORE, LOAD, FREE, PTRADD,
    SPECULATE, COMMIT, GUARD;

    override fun toString(): String {
        return super.toString().lowercase()
    }

    companion object {
        // Operations that have more complicated control-flow (signifies end of block)
        val TERMINATORS = listOf(JMP, RET, BR, GUARD)
        val IMPURE = TERMINATORS + listOf(CALL, ALLOC, STORE, LOAD, FREE)
    }
}


/** The types of values/literals that appear as constants */
@Serializable(with = ValueSerializer::class)
sealed interface Value

@Serializable
data class BooleanValue(val value: Boolean) : Value {
    override fun toString(): String {
        return "$value"
    }
}

data class IntValue(val value: BigInteger) : Value {
    override fun toString(): String {
        return "$value"
    }
}

@Serializable
data class FloatValue(val value: Float) : Value {
    override fun toString(): String {
        return "$value"
    }
}