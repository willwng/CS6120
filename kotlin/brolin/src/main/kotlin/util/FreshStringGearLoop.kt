package util

import trees.CookedLabel
import trees.CookedProgram
import trees.WriteInstruction
import kotlin.math.max

/** A gear-loop assists climbers. This one creates fresh strings */
abstract class FreshStringGearLoop {
    open val usedStrings = mutableSetOf<String>()

    fun addString(name: String) {
        usedStrings.add(name)
    }

    fun get(base: String): String {
        if (base !in usedStrings) {
            addString(base)
            return base
        }
        var i = 0
        var s: String
        do {
            s = "${base}${i}"
            i++
        } while (s in usedStrings)
        addString(s)
        return s
    }
}

/** Fresh labels for blocks */
class FreshLabelGearLoop(labels: List<String>) : FreshStringGearLoop() {
    init {
        labels.forEach { addString(it) }
    }

    constructor(program: CookedProgram) : this(program.functions.flatMap { function ->
        function.instructions.filterIsInstance<CookedLabel>().map { it.label }
    })

    constructor(program: CFGProgram) : this(program.graphs.flatMap { cfg ->
        cfg.nodes.flatMap { it.block.instructions }.filterIsInstance<CookedLabel>()
            .map { it.label } + cfg.nodes.map { it.name }
    })

    fun get(): String = get("l")
}


/** Fresh names for variables */
class FreshNameGearLoop(names: List<String>) : FreshStringGearLoop() {
    init {
        names.forEach { addString(it) }
    }

    constructor(program: CookedProgram) : this(program.functions.flatMap { function ->
        function.instructions.filterIsInstance<WriteInstruction>().map { it.dest }
    })

    constructor(program: CFGProgram) : this(program.graphs.flatMap { cfg ->
        cfg.nodes.flatMap { it.block.instructions.filterIsInstance<WriteInstruction>().map { it.dest } }
    })

    fun get(): String = get("v")
}
