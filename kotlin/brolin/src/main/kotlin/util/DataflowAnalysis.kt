package util

import trees.CookedProgram

interface DataflowValue

/** The beta describes how climbers complete a pass. */
interface DataflowBeta<T : DataflowValue> {
    val init: T
    fun merge(predecessors: List<T>): T
    fun transfer(node: CFGNode, inEdge: T): T
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

    fun applyToProgram(program: CookedProgram): Map<String, DataflowResult<T>> =
        CFGProgram.of(program).graphs.associate {
            it.function.name to forwardWorklist(it)
        }

    private fun forwardWorklist(cfg: CFG): DataflowResult<T> {
        val worklist = mutableListOf<CFGNode>()
        val inValue = mutableMapOf<CFGNode, T>()
        val outValue = mutableMapOf<CFGNode, T>()

//        inValue[cfg.entry] = beta.init
        // TODO the above line was in class pseudocode, but is anything wrong with the below instead?
        cfg.nodes.forEach { inValue[it] = beta.init }
        cfg.nodes.forEach { outValue[it] = beta.init }
        worklist.addAll(cfg.nodes)
        while (worklist.isNotEmpty()) {
            val b = worklist.first()
            worklist.removeFirst()
            inValue[b] = beta.merge(b.predecessors.mapNotNull { outValue[it] })
            val newOut = beta.transfer(b, inValue[b]!!)
            if (newOut != outValue[b]) worklist.addAll(b.successors)
            outValue[b] = newOut
        }
        return DataflowResult(cfg.nodes.fold(mutableMapOf()) { acc, node ->
            acc[node] = Pair(inValue[node]!!, outValue[node]!!); acc
        })
    }
}