package com.xstar.notebook.data.secure

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * 用 Android Keystore 加密存储 PAT。security-crypto 已废弃但仍可用；
 * Keystore 不可用时降级到明文 SharedPreferences（返回 [fallbackUsed] = true 供 UI 提示风险）。
 */
class SecureTokenStore(context: Context) {

    private val prefs: SharedPreferences
    private var fallbackUsed = false

    init {
        prefs = try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: Exception) {
            fallbackUsed = true
            context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        }
    }

    fun putToken(repoId: Long, token: String) {
        prefs.edit().putString(key(repoId), token).apply()
    }

    fun getToken(repoId: Long): String? = prefs.getString(key(repoId), null)

    fun removeToken(repoId: Long) {
        prefs.edit().remove(key(repoId)).apply()
    }

    fun isFallbackUsed(): Boolean = fallbackUsed

    private fun key(repoId: Long) = "pat_$repoId"

    private companion object {
        const val FILE_NAME = "secure_tokens"
    }
}