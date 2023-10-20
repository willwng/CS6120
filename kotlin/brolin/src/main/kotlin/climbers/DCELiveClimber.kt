package climbers

import analysis.dataflow.DataflowAnalysis.DataflowResult
import analysis.dataflow.LiveVariablesAnalysis
import analysis.dataflow.LiveVariablesAnalysis.LiveVariablesBeta.LiveVars
import trees.CookedInstructionOrLabel
import trees.WriteInstruction
import util.CFG
import util.CFGProgram

/** DCE, but stronger (uses live variable analysis) */
object DCELiveClimber : CFGClimber {
    override fun applyToCFG(cfgProgram: CFGProgram): CFGProgram {
        val liveVarAnalysis = LiveVariablesAnalysis.analyze(program = cfgProgram)
        cfgProgram.graphs.forEach { cfg ->
            applyToCFG(cfg = cfg, dataflowResult = liveVarAnalysis[cfg.fnName]!!)
        }
        return cfgProgram
    }

    private fun applyToCFG(cfg: CFG, dataflowResult: DataflowResult<LiveVars>) {
        cfg.nodes.forEach { node ->
            val liveVarsOut = dataflowResult.result[node]!!.second
            val newInsns = mutableListOf<CookedInstructionOrLabel>()
            node.block.instructions.filterIsInstance<WriteInstruction>().filter { insn ->
                insn.dest in liveVarsOut.live
            }

        }
    }

}