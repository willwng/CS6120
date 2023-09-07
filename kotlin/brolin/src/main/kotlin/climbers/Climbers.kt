package climbers

import trees.CookedProgram

interface Climber {
    fun applyToProgram(program: CookedProgram): CookedProgram
}