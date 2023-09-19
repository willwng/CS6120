import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import trees.RawProgram
import trees.TreeCooker
import util.CFGProgram
import java.io.File
import java.io.FileInputStream

object TestFixture {
    private val files = File("src/test/resources/bril").walk().filter { it.extension == "json" }
    val cfgPrograms = getCFGPrograms(files.toList())


    @OptIn(ExperimentalSerializationApi::class)
    private fun getCFGProgram(file: File): CFGProgram {
        val rawProgram = Json.decodeFromStream<RawProgram>(FileInputStream(file))
        val cookedProgram = TreeCooker.cookProgram(rawProgram)
        return CFGProgram.of(cookedProgram)
    }

    private fun getCFGPrograms(files: List<File>): List<CFGProgram> {
        return files.toList().map(::getCFGProgram)
    }
}