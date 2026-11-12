package com.example.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

@Serializable
data class XtreamSourceProfile(
    val id: String,
    val sourceName: String,
    val serverUrl: String,
    val username: String,
    val password: String,
    val isActive: Boolean = false,
    val lastVerifiedEpochMs: Long = 0L,
    val accountStatus: String = "Active",
    val expDate: String? = null,
    val activeConnections: String? = null,
    val maxConnections: String? = null
) {
    fun truncatedUrl(): String {
        val cleaned = serverUrl.removePrefix("https://").removePrefix("http://").trimEnd('/')
        return if (cleaned.length > 34) cleaned.take(31) + "..." else cleaned
    }
}

/**
 * Encrypts and stores Multi-Xtream credentials & parental lock PIN locally using
 * Android Keystore AES-256-GCM hardware-backed encryption.
 */
class SecureSourceStorage(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) return existingKey

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val cipherB64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
        return "$ivB64:$cipherB64"
    }

    private fun decrypt(payload: String): String? {
        return try {
            val parts = payload.split(":")
            if (parts.size != 2) return null
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val cipherBytes = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    fun loadSources(): List<XtreamSourceProfile> {
        val encrypted = prefs.getString(KEY_SOURCES_BLOB, null) ?: return emptyList()
        val decrypted = decrypt(encrypted) ?: return emptyList()
        return try {
            json.decodeFromString<List<XtreamSourceProfile>>(decrypted)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveSources(sources: List<XtreamSourceProfile>) {
        val serialized = json.encodeToString(sources)
        val encrypted = encrypt(serialized)
        prefs.edit().putString(KEY_SOURCES_BLOB, encrypted).apply()
    }

    fun clearAllSources() {
        prefs.edit().remove(KEY_SOURCES_BLOB).apply()
    }

    fun saveParentalPin(pin: String?) {
        if (pin.isNullOrBlank()) {
            prefs.edit().remove(KEY_PARENTAL_PIN).apply()
        } else {
            prefs.edit().putString(KEY_PARENTAL_PIN, encrypt(pin)).apply()
        }
    }

    fun getParentalPin(): String? {
        val encrypted = prefs.getString(KEY_PARENTAL_PIN, null) ?: return null
        return decrypt(encrypted)
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "nextvu_xtream_master_key_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val PREFS_NAME = "nextvu_secure_vault"
        private const val KEY_SOURCES_BLOB = "encrypted_sources_json"
        private const val KEY_PARENTAL_PIN = "encrypted_parental_pin"
    }
}
