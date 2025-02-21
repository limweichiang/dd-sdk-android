package com.example.model

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import java.lang.IllegalStateException
import java.lang.NumberFormatException
import kotlin.Boolean
import kotlin.String
import kotlin.collections.ArrayList
import kotlin.collections.List
import kotlin.jvm.JvmStatic
import kotlin.jvm.Throws

public data class Message(
    public val destination: List<String>,
    public val origin: String,
    public val subject: String? = null,
    public val message: String? = null,
    public var labels: List<String>? = null,
    public var read: Boolean? = null,
    public var important: Boolean? = null
) {
    public fun toJson(): JsonElement {
        val json = JsonObject()
        val destinationArray = JsonArray(destination.size)
        destination.forEach { destinationArray.add(it) }
        json.add("destination", destinationArray)
        json.addProperty("origin", origin)
        subject?.let { json.addProperty("subject", it) }
        message?.let { json.addProperty("message", it) }
        labels?.let { temp ->
            val labelsArray = JsonArray(temp.size)
            temp.forEach { labelsArray.add(it) }
            json.add("labels", labelsArray)
        }
        read?.let { json.addProperty("read", it) }
        important?.let { json.addProperty("important", it) }
        return json
    }

    public companion object {
        @JvmStatic
        @Throws(JsonParseException::class)
        public fun fromJson(serializedObject: String): Message {
            try {
                val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                val destination = jsonObject.get("destination").asJsonArray.let { jsonArray ->
                    val collection = ArrayList<String>(jsonArray.size())
                    jsonArray.forEach {
                        collection.add(it.asString)
                    }
                    collection
                }
                val origin = jsonObject.get("origin").asString
                val subject = jsonObject.get("subject")?.asString
                val message = jsonObject.get("message")?.asString
                val labels = jsonObject.get("labels")?.asJsonArray?.let { jsonArray ->
                    val collection = ArrayList<String>(jsonArray.size())
                    jsonArray.forEach {
                        collection.add(it.asString)
                    }
                    collection
                }
                val read = jsonObject.get("read")?.asBoolean
                val important = jsonObject.get("important")?.asBoolean
                return Message(destination, origin, subject, message, labels, read, important)
            } catch (e: IllegalStateException) {
                throw JsonParseException(e.message)
            } catch (e: NumberFormatException) {
                throw JsonParseException(e.message)
            }
        }
    }
}
