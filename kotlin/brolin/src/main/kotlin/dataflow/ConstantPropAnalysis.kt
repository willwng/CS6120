package dataflow

import dataflow.ConstantPropAnalysis.ConstantPropBeta.Constant.Known
import dataflow.ConstantPropAnalysis.ConstantPropBeta.Constant.MultiDefined
import dataflow.ConstantPropAnalysis.ConstantPropBeta.KnownConstants
import dataflow.DataflowAnalysis.DataflowResult
import trees.ConstantInstruction
import trees.Value
import trees.WriteInstruction
import util.CFGNode
import util.CFGProgram

object ConstantPropAnalysis {

    class ConstantPropBeta : DataflowBeta<KnownConstants> {
        sealed interface Constant {
            // The value takes on multiple definitions or more complex expressions (e.g., operation/function call)
            data object MultiDefined : Constant
            data class Known(val value: Value) : Constant {
                override fun toString(): String {
                    return "$value"
                }
            }
        }

        /** The absence of a variable in this map represents uninitialized data (the top value for this analysis) */
        data class KnownConstants(val constants: Map<String, Constant>) : DataflowValue {
            override fun toString(): String {
                return "$constants"
            }
        }

        override val forward = true

        override val init: KnownConstants = KnownConstants(mutableMapOf())

        override fun merge(influencers: List<KnownConstants>): KnownConstants =
            if (influencers.isEmpty()) KnownConstants(mapOf())
            else KnownConstants(influencers.fold(mutableMapOf()) { acc, influencer ->
                influencer.constants.forEach { (k, v) ->
                    when {
                        k !in acc -> acc[k] = v
                        k in acc && acc[k] != v -> acc[k] = MultiDefined
                    }
                }
                acc
            })

        override fun transfer(node: CFGNode, inEdge: KnownConstants): KnownConstants {
            val constants = inEdge.constants.toMutableMap()
            node.block.instructions.filterIsInstance<WriteInstruction>().forEach { instr ->
                when (instr) {
                    is ConstantInstruction -> {
                        // Conflicting assignments to this variable
                        if (instr.dest in constants && constants[instr.dest] != instr.value) {
                            constants[instr.dest] = MultiDefined
                        } else {
                            constants[instr.dest] = Known(instr.value)
                        }
                    }
                    else -> constants[instr.dest] = MultiDefined
                }
            }
            return KnownConstants(constants = constants)
        }
    }

    fun analyze(program: CFGProgram): Map<String, DataflowResult<KnownConstants>> =
        DataflowAnalysis(ConstantPropBeta()).applyToProgram(program)
}