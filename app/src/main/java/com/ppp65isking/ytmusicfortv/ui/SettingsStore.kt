package com.ppp65isking.ytmusicfortv.ui

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Where the navigation lives: the icon rail on the left, or a bar on top. */
enum class NavPosition { LEFT, TOP }

/**
 * App settings, backed by SharedPreferences (same pattern as AuthStore).
 * Only the nav position today — About and the clear-data rows read the
 * library store directly, and sign-in is still a Phase 3 placeholder.
 */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("ytm_settings", Context.MODE_PRIVATE)

    private val _navPosition = MutableStateFlow(readNavPosition())
    val navPosition: StateFlow<NavPosition> = _navPosition.asStateFlow()

    fun setNavPosition(position: NavPosition) {
        prefs.edit().putString(KEY_NAV_POSITION, position.name).apply()
        _navPosition.value = position
    }

    private fun readNavPosition(): NavPosition =
        runCatching {
            NavPosition.valueOf(
                prefs.getString(KEY_NAV_POSITION, NavPosition.LEFT.name)
                    ?: NavPosition.LEFT.name,
            )
        }.getOrDefault(NavPosition.LEFT)

    companion object {
        private const val KEY_NAV_POSITION = "nav_position"
    }
}
