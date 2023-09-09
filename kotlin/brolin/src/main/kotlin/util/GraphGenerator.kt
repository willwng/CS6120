package util

object GraphGenerator {
    private fun BasicBlock.getContent(): String {
        val sb = StringBuilder()
        instructions.forEach { sb.append("$it \\l") }
        return sb.replace(Regex("\""), Regex.escapeReplacement("\\\""))
    }

    private fun addNode(node: CFGNode, nodeMap: Map<CFGNode, Int>, sb: StringBuilder) {
        sb.append("\t")
        sb.append("node_${nodeMap[node]!!} [shape = box, label = \"${node.block.getContent()}\"]")
        sb.appendLine()
    }

    fun createGraphOutput(cfg: CFG): StringBuilder {
        val sb = StringBuilder()
        sb.append("digraph ${cfg.function.name} {")
        sb.appendLine()

        val nodeMap = cfg.nodes.mapIndexed { i, node -> node to i }.toMap()
        cfg.nodes.forEach { node -> addNode(node, nodeMap, sb) }
        sb.append("}")

        println("---------------")
        println(sb.toString())
        println("---------------")
        return sb
    }
}