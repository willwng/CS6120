package climbers

import trees.CookedProgram

/** A climber traverses a tree (and returns the result of the traversal). */
interface Climber {
    fun applyToProgram(program: CookedProgram): CookedProgram
}