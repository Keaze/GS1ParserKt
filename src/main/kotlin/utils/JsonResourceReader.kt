package org.app.utils

import kotlinx.serialization.json.Json
object JsonResourceReader {

    // Private JSON configuration - only accessible within this object
    val json = Json {
        ignoreUnknownKeys = true  // Ignore extra fields in JSON
        isLenient = true          // Allow relaxed JSON parsing
        prettyPrint = true        // For debugging
        encodeDefaults = true     // Include default values when serializing
    }

    // Inline function can access private members within the same object
    inline fun <reified T> readJsonFromResource(fileName: String): T? {
        return try {
            val jsonString = readResourceAsString(fileName)
            jsonString?.let { json.decodeFromString<T>(it) }
        } catch (e: Exception) {
            println("Error parsing JSON: ${e.message}")
            null
        }
    }

    // Alternative method that reads and parses in one step
    inline fun <reified T> parseJsonResource(fileName: String): ResultKt<T, Exception> {
        return try {
            val jsonString = readResourceAsString(fileName)
                ?: return ResultKt.failure(Exception("File not found: $fileName"))

            val parsedObject = json.decodeFromString<T>(jsonString)
            ResultKt.success(parsedObject)
        } catch (e: Exception) {
            ResultKt.failure(e)
        }
    }

    // Helper method to read file as string - private within the object
    fun readResourceAsString(fileName: String): String? {
        return try {
            this::class.java.classLoader.getResourceAsStream(fileName)?.use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            }
        } catch (e: Exception) {
            println("Error reading file: ${e.message}")
            null
        }
    }

    // Utility method to check if resource exists
    fun resourceExists(fileName: String): Boolean {
        return this::class.java.classLoader.getResource(fileName) != null
    }

    // Method to get raw JSON string if needed
    fun getResourceAsString(fileName: String): String? = readResourceAsString(fileName)
}
