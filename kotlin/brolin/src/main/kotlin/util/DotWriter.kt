package util

import java.io.PrintWriter

class DotWriter {
    private val content = StringBuilder()
    fun startGraph(name: String) {
        content.append("digraph $name {")
        content.appendLine()
        content.append("rankdir=TD; ordering=out;")
        content.append("START [shape = none, label = entry]")
        content.appendLine()
    }

    fun writeNode(nodeName: String, shape: String, label: String) {
        content.append("\t")
        content.append("$nodeName [shape = $shape, label = \"$label\"]")
        content.appendLine()
    }

    fun writeEntryEdge(entryNodeName: String) {
        content.append("\t")
        content.append("START -> $entryNodeName")
        content.appendLine()
    }

    fun writeEdge(sourceName: String, sinkName: String) {
        content.append("\t")
        content.append("$sourceName -> $sinkName")
        content.appendLine()
    }

    fun finishGraph() {
        content.append("}")
    }

    fun writeToFile(out: PrintWriter) {
        out.println(content.toString())
    }
}