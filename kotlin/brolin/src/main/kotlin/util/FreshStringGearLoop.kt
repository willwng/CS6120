package util

import trees.CookedLabel
import trees.CookedProgram
import trees.WriteInstruction
import kotlin.math.max

/** A gear-loop assists climbers. This one creates fresh strings */
abstract class FreshStringGearLoop {
    open val usedStrings = mutableMapOf<String, Int>()
    private var i = 0

    fun addString(name: String, lastIndex: Int = 0) {
        usedStrings[name] = max(lastIndex, usedStrings[name] ?: 0)
    }

    fun get(base: String): String {
        // If we have not used it already, just take the base
        if (base !in usedStrings) {
            addString(base)
            return base
        }
        // Otherwise, generate a new string
        val lastIndex = usedStrings[base]!!
        addString(base, lastIndex + 1)
        return "${base}_${lastIndex}"
    }
}

/** Fresh labels for blocks */
class FreshLabelGearLoop(program: CookedProgram) : FreshStringGearLoop() {
    init {
        program.functions.forEach { function ->
            function.instructions.filterIsInstance<CookedLabel>().forEach {
                addString(it.label)
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
                addString(it.dest)
            }
        }
    }

    fun get(): String = get("v")
}
