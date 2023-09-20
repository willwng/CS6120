package dataflow

import util.CFGNode
import util.CFG
import util.CFGProgram

interface DataflowValue

/** The beta describes how climbers complete a pass. */
interface DataflowBeta<T : DataflowValue> {
    val init: T
    fun merge(influencers: List<T>): T
    fun transfer(node: CFGNode, inEdge: T): T
    val forward: Boolean
}

class DataflowAnalysis<T : DataflowValue>(private val beta: DataflowBeta<T>) {
    /** A mapping from each CFG node to its in edge values and out edge values. */
    data class DataflowResult<T>(val result: Map<CFGNode, Pair<T, T>>) {
        override fun toString(): String {
            val builder = StringBuilder()
            result.forEach { node, (inValue, outValue) ->
                builder.append("\n${node.block}\n\tIn value: $inValue\n\tOut value: $outValue")
            }
            return builder.toString()
        }
    }

    fun applyToProgram(program: CFGProgram): Map<String, DataflowResult<T>> =
        program.graphs.associate { cfg ->
            cfg.function.name to worklist(cfg)
        }

    /** A generic worklist algorithm used for solving both forward and backward data flow problems */
    private fun worklist(cfg: CFG): DataflowResult<T> {
        val worklist = cfg.nodes.toMutableList()
        // Information stored on in/out edges of each node (initialized to init)
        val inValue = mutableMapOf<CFGNode, T>()
        val outValue = mutableMapOf<CFGNode, T>()
        cfg.nodes.forEach { inValue[it] = beta.init; outValue[it] = beta.init }

        // Data that influences this node's output and the data that represents this node's output
        val influencerData = if (beta.forward) outValue else inValue
        val followerData = if (beta.forward) inValue else outValue

        while (worklist.isNotEmpty()) {
            val b = worklist.removeFirst()
            // The set of nodes which influences/is influenced by this one
            val influencers = if (beta.forward) b.predecessors else b.successors
            val followers = if (beta.forward) b.successors else b.predecessors

            val mergedData = beta.merge(influencers.mapNotNull { influencerData[it] })
            followerData[b] = mergedData

            val newData = beta.transfer(b, mergedData)
            if (newData != followerData[b]) worklist.addAll(followers)
            influencerData[b] = newData
        }
        return DataflowResult(cfg.nodes.fold(mutableMapOf()) { acc, node ->
            acc[node] = Pair(inValue[node]!!, outValue[node]!!); acc
        })
    }
}