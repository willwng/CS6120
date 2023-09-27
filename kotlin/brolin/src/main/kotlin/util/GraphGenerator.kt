package util

import analysis.dataflow.DataflowAnalysis.DataflowResult
import analysis.DominatorTree
import java.io.FileOutputStream
import java.io.PrintWriter

object GraphGenerator {
    private fun String.fixStringQuotes(): String {
        return replace(Regex("\""), Regex.escapeReplacement("\\\""))
    }

    private fun BasicBlock.getContent(): String {
        val sb = StringBuilder()
        instructions.forEach { sb.append("$it \\l") }
        return sb.toString().fixStringQuotes()
    }

    private fun getNodeName(node: CFGNode, nodeMap: Map<CFGNode, Int>): String {
        return "node_${nodeMap[node]!!}"
    }

    private fun addNode(
        node: CFGNode,
        nodeMap: Map<CFGNode, Int>,
        dotWriter: DotWriter,
        prologue: String = "",
        epilogue: String = ""
    ) {
        val content = "name: ${node.name}" +
            prologue.fixStringQuotes() + "\\l\\l" + node.block.getContent() + "\\l" + epilogue.fixStringQuotes()
        dotWriter.writeNode(nodeName = getNodeName(node, nodeMap), shape = "box", label = content)
    }

    private fun addEdge(
        source: CFGNode,
        sink: CFGNode,
        nodeMap: Map<CFGNode, Int>,
        dotWriter: DotWriter,
    ) {
        dotWriter.writeEdge(
            sourceName = getNodeName(source, nodeMap),
            sinkName = getNodeName(sink, nodeMap),
        )
    }

    fun <T> createGraphOutput(cfg: CFG, dataflowResult: DataflowResult<T>? = null): DotWriter {
        val dotWriter = DotWriter()
        dotWriter.startGraph(name = cfg.fnName)

        // Keep track of the numbering of each CFG node
        val nodeMap = cfg.nodes.mapIndexed { i, node -> node to i }.toMap()
        // Add the nodes, then the edges
        cfg.nodes.forEach { node ->
            addNode(
                node = node,
                nodeMap = nodeMap,
                dotWriter = dotWriter,
                prologue = if (dataflowResult != null) "in: ${dataflowResult.result[node]?.first}" else "",
                epilogue = if (dataflowResult != null) "out: ${dataflowResult.result[node]?.second}" else ""
            )
        }
        cfg.nodes.forEach { node ->
            node.successors.forEach { sink ->
                addEdge(
                    source = node,
                    sink = sink,
                    nodeMap = nodeMap,
                    dotWriter = dotWriter
                )
            }
        }
        dotWriter.writeEntryEdge(getNodeName(node = cfg.entry, nodeMap = nodeMap))
        dotWriter.finishGraph()

        return dotWriter
    }

    fun createDominatorTreeOutput(dominatorTree: DominatorTree): DotWriter {
        val dotWriter = DotWriter()
        dotWriter.startGraph(name = dominatorTree.cfg.fnName + "_dominator")
        val nodeMap = dominatorTree.nodes.map { it.cfgNode }.mapIndexed { i, node -> node to i }.toMap()

        dominatorTree.nodes.forEach {
            addNode(
                node = it.cfgNode,
                nodeMap = nodeMap,
                dotWriter = dotWriter
            )
        }
        dominatorTree.nodes.forEach { node ->
            node.dominated.forEach { dom ->
                addEdge(
                    source = dom.cfgNode,
                    sink = node.cfgNode,
                    nodeMap = nodeMap,
                    dotWriter = dotWriter
                )
            }
        }
        dotWriter.writeEntryEdge(getNodeName(node = dominatorTree.entry.cfgNode, nodeMap = nodeMap))
        dotWriter.finishGraph()

        return dotWriter
    }

    fun <T> writeDotOutput(cfgProgram: CFGProgram, dataflowAnalysis: Map<String, DataflowResult<T>>, fileExt: String) {
        cfgProgram.graphs.forEach {
            val out = PrintWriter(FileOutputStream("${it.fnName}-$fileExt.dot"))
            createGraphOutput(it, dataflowAnalysis[it.fnName]).writeToFile(out)
            out.close()
        }
    }
}