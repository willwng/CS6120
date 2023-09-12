package util

interface DataflowValue

/** The beta describes how climbers complete a pass. */
interface DataflowBeta<T : DataflowValue> {
    val init: T
    fun merge(predecessors: List<T>): T
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
        program.graphs.associate {
            it.function.name to forwardWorklist(it)
        }

    /** A generic worklist algorithm used for solving forward data flow problems */
    private fun forwardWorklist(cfg: CFG): DataflowResult<T> {
        assert(beta.forward)
        val worklist = cfg.nodes.toMutableList()
        // Information stored on in/out edges of each node (initialized to init)
        val inValue = mutableMapOf<CFGNode, T>()
        val outValue = mutableMapOf<CFGNode, T>()
        cfg.nodes.forEach { inValue[it] = beta.init; outValue[it] = beta.init }

        while (worklist.isNotEmpty()) {
            val b = worklist.removeFirst()
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