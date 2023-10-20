package climbers

import analysis.dataflow.DataflowAnalysis.DataflowResult
import analysis.dataflow.LiveVariablesAnalysis
import analysis.dataflow.LiveVariablesAnalysis.LiveVariablesBeta.LiveVars
import trees.ReadInstruction
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
            // Since granularity is at the node-level, we need to check possible live-vars in this node
            val liveVarsNode = node.block.instructions.filterIsInstance<ReadInstruction>().flatMap { it.args }.toSet()
            // Variables that are live out of this node
            val liveVarsOut = dataflowResult.result[node]!!.second.live
            // Keep only WriteInstructions that define something that is live out
//            println("---")
//            println(node.name)
//            println(liveVarsOut)
//            println(dataflowResult.result[node]!!.first.live)
            node.replaceInsns(
                node.block.instructions.filter {
                    (it !is WriteInstruction) || (it.dest in liveVarsOut) || (it.dest in liveVarsNode)
                }
            )
        }
    }

}