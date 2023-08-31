
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Project(val name: String, val language: String)

fun main() {
    val input = generateSequence(::readLine).joinToString("\n")
    val json = Json.parseToJsonElement(input)
    print(json)
}