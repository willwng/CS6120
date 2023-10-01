package util

import trees.CookedLabel
import trees.CookedProgram
import trees.WriteInstruction

/** A gear-loop assists climbers. This one creates fresh strings */
abstract class FreshStringGearLoop {
    open val usedStrings = mutableSetOf<String>()
    private var i = 0

    fun get(base: String): String {
        do {
            // If we have not used it already, just take the base
            if (base !in usedStrings) {
                usedStrings.add(base)
                return base
            }
            // Otherwise, try to generate a new string
            val candidate = "${base}_$i"
            if (candidate !in usedStrings) {
                usedStrings.add(candidate)
                return candidate
            }
            i++
        } while (true)
    }
}

/** Fresh labels for blocks */
class FreshLabelGearLoop(program: CookedProgram) : FreshStringGearLoop() {
    init {
        program.functions.forEach { function ->
            function.instructions.filterIsInstance<CookedLabel>().forEach {
                usedStrings.add(it.label)
            }
        }
    }

    fun get(): String = get("l")
}

/** Fresh names for variables */
class FreshNameGearLoop(program: CookedProgram) : FreshStringGearLoop() {
    init {
        program.functions.forEach { function ->
            function.instructions.filterIsInstance<WriteInstruction>().forEach {
                usedStrings.add(it.dest)
            }
        }
    }

    fun get(): String = get("v")
}
