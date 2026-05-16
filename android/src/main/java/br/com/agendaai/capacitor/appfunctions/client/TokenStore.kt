package br.com.agendaai.capacitor.appfunctions.client

import android.content.Context
import android.content.SharedPreferences

/**
 * Armazena o JWT + apiBaseUrl em SharedPreferences (acessivel por background).
 *
 * Quando o app esta em foreground, o JS chama `setAuthToken({ token, apiBaseUrl })`
 * apos login/restore -> entra aqui. Quando o Gemini invoca uma @AppFunction no
 * background, o codigo nativo le esse token sem precisar do JS.
 */
class TokenStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_TOKEN) else putString(KEY_TOKEN, value)
                apply()
            }
        }

    var apiBaseUrl: String
        get() = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        set(value) {
            prefs.edit().putString(KEY_BASE_URL, value).apply()
        }

    fun clear() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    companion object {
        private const val PREFS_NAME = "agendaai.appfunctions"
        private const val KEY_TOKEN = "auth.token"
        private const val KEY_BASE_URL = "api.base_url"
        const val DEFAULT_BASE_URL = "https://agendaai-backend-qw6w.onrender.com"
    }
}
