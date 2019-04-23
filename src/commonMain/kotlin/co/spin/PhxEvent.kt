package co.spin

import co.spin.utils.Log
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

open class PhxEvent<T>(val name: String,
                       val serializer: DeserializationStrategy<T>?,
                       val callbacks: MutableList<OnReceive<T>>,
                       val json: Json = Json.nonstrict
) {
    fun parse(content: String): T? {
        try {
            if (serializer == null) return null
            return json.parse(serializer, content)
        } catch (e: Exception) {
            Log.error{"HUH??? json deserialization failed: $e"}
            return null
        }
    }

    constructor(name: String, serializer: DeserializationStrategy<T>?, callback: OnReceive<T>)
            : this(name, serializer, mutableListOf(callback))

}

class PhxEventJson(name: String,
                   callbacks: MutableList<OnReceiveJson>)
    : PhxEvent<JsonElement>(name, null, callbacks) {

    constructor(name: String, callback: OnReceiveJson)
            : this(name, mutableListOf(callback))
}

