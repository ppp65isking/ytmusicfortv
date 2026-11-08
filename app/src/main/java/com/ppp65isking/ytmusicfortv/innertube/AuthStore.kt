package com.ppp65isking.ytmusicfortv.innertube

import android.content.Context
import android.content.SharedPreferences

/**
 * Small persistent store for the InnerTube session.
 *
 * Holds visitorData (issued by the first response, required on every later
 * call or the player answers LOGIN_REQUIRED) and, later, the browser-cookie
 * headers from `browser.json` (Phase 3 — sign-in for library/playlists).
 *
 * TODO (Phase 3): move browser headers to EncryptedSharedPreferences
 * (androidx.security:security-crypto) before storing real account cookies.
 */
class AuthStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ytm_auth", Context.MODE_PRIVATE)

    var visitorData: String?
        get() = prefs.getString(KEY_VISITOR_DATA, null)
        set(value) {
            prefs.edit().putString(KEY_VISITOR_DATA, value).apply()
        }

    /** Raw `browser.json` contents pasted by the user (Phase 3). */
    var browserHeadersJson: String?
        get() = prefs.getString(KEY_BROWSER_HEADERS, null)
        set(value) {
            prefs.edit().putString(KEY_BROWSER_HEADERS, value).apply()
        }

    val hasBrowserAuth: Boolean get() = browserHeadersJson != null

    companion object {
        private const val KEY_VISITOR_DATA = "visitor_data"
        private const val KEY_BROWSER_HEADERS = "browser_headers_json"
    }
}
