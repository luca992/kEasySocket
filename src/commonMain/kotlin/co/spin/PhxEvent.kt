package co.spin

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JSON
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.parse

open class PhxEvent<T>(val name: String,
                       val serializer: DeserializationStrategy<T>?,
                       val callbacks: MutableList<OnReceive<T>>
) {
    fun parse(content: String): T? {
        try {
            if (serializer == null) return null
            return JSON.nonstrict.parse(serializer, content)
        } catch (e: Exception) {
            println("HUH??? json deserialization failed $e")
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

