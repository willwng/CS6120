/** Contains classes/objects that are shared between Cooked and RawTrees */
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

/** Type is either a primitive, or a parametrized type, which wraps a smaller one */
@Serializable(with = TypeSerializer::class)
class Type(
    val baseType: Type?,
    val type: String
) {
    fun isFinalType(): Boolean {
        return baseType == null
    }

    override fun toString(): String {
        return if (baseType == null) {
            type
        } else {
            "{$type:$baseType}"
        }
    }

    fun isFloat(): Boolean {
        return isFinalType() && type == "\"float\""
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

    private fun buildTypeObject(value: Type): JsonObject {
        assert(value.baseType != null)
        return if (value.baseType!!.isFinalType()) {
            buildJsonObject {
                put(value.type, JsonUnquotedLiteral(value.baseType.type))
            }
        } else {
            buildJsonObject {
                put(value.type, buildTypeObject(value.baseType))
            }
        }
    }

    override fun serialize(encoder: Encoder, value: Type) {
        if (value.baseType == null) {
            encoder as JsonEncoder
            encoder.encodeJsonElement(JsonUnquotedLiteral(value.type))
        } else {
            val typeObject = buildTypeObject(value = value)
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
