package trees

import kotlinx.serialization.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

@Serializable
abstract class SourcedObject {
    val pos: Position? = null

    @SerialName("pos_end")
    val posEnd: Position? = null
    val src: String? = null
}

@Serializable
data class Position(val row: Int, val col: Int)


/** Arguments to functions */
@Serializable
data class Argument(
    val name: String,
    val type: Type
) : SourcedObject()

@Serializable(with = TypeSerializer::class)
class Type(
    val baseType: Type?,
    val type: String
) {
    override fun toString(): String {
        return if (baseType == null) {
            type
        } else {
            "{$type:$baseType}"
        }
    }
}


@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = Type::class)
object TypeSerializer : KSerializer<Type> {
    private fun deserialize(element: JsonElement): Type {
        return if (element is JsonPrimitive) {
            Type(baseType = null, type = element.toString())
        } else {
            element as JsonObject
            val keys = element.keys
            val values = element.values
            assert(keys.size * values.size == 1)
            Type(baseType = deserialize(values.first()), type = keys.first())
        }
    }

    override fun serialize(encoder: Encoder, value: Type) {
        if (value.baseType == null) {
            encoder.encodeString(JsonPrimitive(value.type).toString())
        } else {
            val dum = buildJsonObject {
                put("stupad", "anotha") // TODO build this rec
            }
            val typeObject = buildJsonObject {
                put(value.type, dum) // TODO build this rec
            }
            encoder as JsonEncoder
            encoder.encodeJsonElement(typeObject)
        }
    }

    override fun deserialize(decoder: Decoder): Type {
        decoder as JsonDecoder
        val element = decoder.decodeJsonElement()
        return deserialize(element)
    }

}

