package util

interface DataflowValue

/** The beta describes how climbers complete a pass. */
interface DataflowBeta<T : DataflowValue> {
    val init: T
    fun merge(predecessors: List<T>): T
    fun transfer(node: CFGNode, inEdge: T): T
}

class DataflowAnalysis<T : DataflowValue>(private val beta: DataflowBeta<T>) {
    data class Result<T>(
        val inValues: Map<CFGNode, T>,
        val outValues: Map<CFGNode, T>
    )

    fun forwardWorklist(cfgProgram: CFGProgram): List<Result<T>> = cfgProgram.graphs.map(::forwardWorklist)

    private fun forwardWorklist(cfg: CFG): Result<T> {
        val worklist = mutableListOf<CFGNode>()
        val inValue = mutableMapOf<CFGNode, T>()
        val outValue = mutableMapOf<CFGNode, T>()

        inValue[cfg.entry] = beta.init
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
        return Result(inValue, outValue)
    }
}