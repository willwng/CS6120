package climbers

import trees.CookedProgram
import trees.WriteInstruction

/** A gear-loop assists climbers. This one creates fresh names for variables */
class FreshNameGearLoop(program: CookedProgram) {
    private val usedNames = mutableSetOf<String>()
    private var i = 0

    init {
        program.functions.forEach { function ->
            function.instructions.filterIsInstance<WriteInstruction>().forEach {
                usedNames.add(it.dest)
            }
        }
    }

    fun get(): String = get("v")

    fun get(base: String): String {
        do {
            val candidate = "$base$i"
            if (candidate !in usedNames) {
                usedNames.add(candidate)
                return candidate
            }
            i++
        } while (true)
    }

}