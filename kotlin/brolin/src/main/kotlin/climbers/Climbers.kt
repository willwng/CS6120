package climbers

import trees.CookedProgram
import util.CFGProgram

/** A climber traverses a tree (and returns the result of the traversal). */
interface Climber {
    fun applyToProgram(program: CookedProgram): CookedProgram
}

interface CFGClimber {
    fun applyToCFG(cfgProgram: CFGProgram): CFGProgram
}
