package climbers

import trees.CookedInstructionOrLabel
import trees.CookedProgram

/** The beta describes how climbers complete a pass. */
interface DataflowBeta {
    class DataflowValue

    fun init(): DataflowValue
    fun merge(predecessors: Iterable<DataflowValue>): DataflowValue
    fun transfer(inEdge: DataflowValue): DataflowValue
}

class DataflowClimber(beta: DataflowBeta) : Climber {
    override fun applyToProgram(program: CookedProgram): CookedProgram {
        TODO("Not yet implemented")
    }

    private fun worklistAlg(program: CookedProgram) {
        val worklist = mutableListOf<CookedInstructionOrLabel>()
    }
}