package com.example.pitchmasterbeta.model

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.pitchmasterbeta.MainActivity.Companion.appContext
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoginSharedPreferences private constructor(context: Context) {

    private val mainKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    data class LoginUserFields(val username: String, val password: String)

    private val gson = Gson()
    private val sharedPreferences = EncryptedSharedPreferences.create(
        NAME,
        mainKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    suspend fun saveLoginUser(loginUserFields: LoginUserFields) {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit().putString(LAST_LOGIN, gson.toJson(loginUserFields)).apply()
        }
    }

    suspend fun forgetLoginUser() {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit().remove(LAST_LOGIN).apply()
        }
    }

    suspend fun getLoginUser(): LoginUserFields? {
        return withContext(Dispatchers.IO) {
            try {
                sharedPreferences.getString(LAST_LOGIN, null)?.let { jsonString ->
                    gson.fromJson<LoginUserFields>(
                        jsonString,
                        object : TypeToken<LoginUserFields>() {}.type
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    companion object {
        const val LAST_LOGIN = "LastLogin"
        const val NAME = "LoginSharedPreferences"

        @Volatile
        private var instance: LoginSharedPreferences? = null

        private fun getInstance(context: Context): LoginSharedPreferences =
            instance ?: synchronized(this) {
                instance ?: LoginSharedPreferences(context).also { instance = it }
            }

        suspend fun saveLastLoginUser(name: String, password: String) {
            appContext?.let { context ->
                getInstance(context).saveLoginUser(LoginUserFields(name, password))
            }
        }

        suspend fun forgetLastLoginUser() {
            appContext?.let { context ->
                getInstance(context).forgetLoginUser()
            }
        }

        suspend fun getLastLoginUser(): LoginUserFields? {
            return appContext?.let { context ->
                getInstance(context).getLoginUser()
            }
        }
    }
}