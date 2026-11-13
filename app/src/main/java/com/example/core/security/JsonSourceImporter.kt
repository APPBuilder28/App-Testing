package com.example.core.security

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.IOException

data class ImportedSourceDraft(
    val sourceName: String,
    val serverUrl: String,
    val username: String,
    val password: String,
    val expiryDate: String
)

sealed interface JsonParseResult {
    data class Success(val sources: List<ImportedSourceDraft>) : JsonParseResult
    data class Error(val message: String) : JsonParseResult
}

object JsonSourceImporter {

    private val REQUIRED_KEYS = setOf(
        "sourceName",
        "serverUrl",
        "username",
        "password",
        "expiryDate"
    )

    private const val MAX_JSON_FILE_BYTES = 2 * 1024 * 1024 // 2 MB safety cap

    private val json = Json {
        isLenient = false
        ignoreUnknownKeys = false
    }

    fun readFromUri(context: Context, uri: Uri): JsonParseResult {
        val displayName = resolveDisplayName(context, uri)
        if (displayName != null && !displayName.endsWith(".json", ignoreCase = true)) {
            return JsonParseResult.Error("Please select a valid .json file.")
        }

        val rawText = try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(8192)
                val out = java.io.ByteArrayOutputStream()
                var total = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    total += read
                    if (total > MAX_JSON_FILE_BYTES) {
                        return JsonParseResult.Error("Selected JSON file is too large (max 2 MB).")
                    }
                    out.write(buffer, 0, read)
                }
                out.toString(Charsets.UTF_8.name())
            } ?: return JsonParseResult.Error("Unable to open the selected file.")
        } catch (_: SecurityException) {
            return JsonParseResult.Error("Permission denied: Unable to read the selected file.")
        } catch (_: IOException) {
            return JsonParseResult.Error("File read error: Could not read the selected JSON file.")
        } catch (_: Exception) {
            return JsonParseResult.Error("Unexpected error reading the selected file.")
        }

        return parseAndValidateJson(rawText)
    }

    fun parseAndValidateJson(rawJson: String): JsonParseResult {
        val trimmed = rawJson.trim().removePrefix("\uFEFF")
        if (trimmed.isBlank()) {
            return JsonParseResult.Error("Selected JSON file is empty.")
        }

        val root = try {
            json.parseToJsonElement(trimmed)
        } catch (_: SerializationException) {
            return JsonParseResult.Error("Malformed JSON file. Please check the file syntax.")
        } catch (_: IllegalArgumentException) {
            return JsonParseResult.Error("Malformed JSON file. Please check the file syntax.")
        }

        val array = root as? JsonArray
            ?: return JsonParseResult.Error("Invalid schema: JSON root must be an array of source objects.")

        if (array.isEmpty()) {
            return JsonParseResult.Error("Invalid schema: JSON array contains no sources.")
        }

        val parsed = mutableListOf<ImportedSourceDraft>()
        array.forEachIndexed { index, element ->
            val obj = element as? JsonObject
                ?: return JsonParseResult.Error("Invalid schema at item #${index + 1}: expected a JSON object.")

            if (obj.keys != REQUIRED_KEYS) {
                return JsonParseResult.Error(
                    "Invalid schema at item #${index + 1}: each object must have exact keys sourceName, serverUrl, username, password, expiryDate."
                )
            }

            val sourceName = requireNonBlankString(obj, "sourceName", index)
                ?: return JsonParseResult.Error("Invalid schema at item #${index + 1}: 'sourceName' must be a non-empty string.")
            val serverUrl = requireNonBlankString(obj, "serverUrl", index)
                ?: return JsonParseResult.Error("Invalid schema at item #${index + 1}: 'serverUrl' must be a non-empty string.")
            val username = requireNonBlankString(obj, "username", index)
                ?: return JsonParseResult.Error("Invalid schema at item #${index + 1}: 'username' must be a non-empty string.")
            val password = requireNonBlankString(obj, "password", index)
                ?: return JsonParseResult.Error("Invalid schema at item #${index + 1}: 'password' must be a non-empty string.")
            val expiryDate = parseOptionalString(obj, "expiryDate")
                ?: return JsonParseResult.Error("Invalid schema at item #${index + 1}: 'expiryDate' must be a string or null.")

            parsed.add(
                ImportedSourceDraft(
                    sourceName = sourceName.trim(),
                    serverUrl = serverUrl.trim(),
                    username = username.trim(),
                    password = password,
                    expiryDate = expiryDate.trim()
                )
            )
        }

        return JsonParseResult.Success(parsed)
    }

    private fun requireNonBlankString(obj: JsonObject, key: String, index: Int): String? {
        val primitive = obj[key] as? JsonPrimitive ?: return null
        if (!primitive.isString) return null
        val content = primitive.content
        return if (content.isBlank()) null else content
    }

    private fun parseOptionalString(obj: JsonObject, key: String): String? {
        val el = obj[key] ?: return null
        if (el is JsonNull) return ""
        val primitive = el as? JsonPrimitive ?: return null
        return if (primitive.isString) primitive.content else null
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}
