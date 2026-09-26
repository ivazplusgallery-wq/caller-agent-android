package com.ivazplus.calleragent

import android.content.Context
import android.content.SharedPreferences

/**
 * Same idea as config.json on the Windows desktop client: host, port,
 * secure flag, api key, operator name. Stored in normal SharedPreferences
 * here instead of a JSON file.
 */
object Prefs {
    private const val FILE = "relay_prefs"

    fun get(context: Context): SharedPreferences =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isConfigured(context: Context): Boolean {
        val p = get(context)
        return !p.getString("host", "").isNullOrBlank() && !p.getString("api_key", "").isNullOrBlank()
    }
}
