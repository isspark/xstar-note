package com.xstar.notebook.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class AppSettings(context: Context) {

    private val sp = context.applicationContext.getSharedPreferences("xstar_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        ThemeMode.valueOf(sp.getString(KEY_THEME, ThemeMode.SYSTEM.name)!!),
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _dynamicColor = MutableStateFlow(sp.getBoolean(KEY_DYNAMIC_COLOR, false))
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private val _defaultRepoId = MutableStateFlow(sp.getLong(KEY_DEFAULT_REPO, -1L))
    val defaultRepoId: StateFlow<Long> = _defaultRepoId.asStateFlow()

    private val _taskNotificationEnabled = MutableStateFlow(sp.getBoolean(KEY_TASK_NOTIFICATION, false))
    val taskNotificationEnabled: StateFlow<Boolean> = _taskNotificationEnabled.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        sp.edit().putString(KEY_THEME, mode.name).apply()
    }

    fun setDynamicColor(enabled: Boolean) {
        _dynamicColor.value = enabled
        sp.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
    }

    fun setDefaultRepo(id: Long) {
        _defaultRepoId.value = id
        sp.edit().putLong(KEY_DEFAULT_REPO, id).apply()
    }

    fun clearDefaultRepo() {
        _defaultRepoId.value = -1L
        sp.edit().putLong(KEY_DEFAULT_REPO, -1L).apply()
    }

    fun setTaskNotificationEnabled(enabled: Boolean) {
        _taskNotificationEnabled.value = enabled
        sp.edit().putBoolean(KEY_TASK_NOTIFICATION, enabled).apply()
    }

    private companion object {
        const val KEY_THEME = "theme_mode"
        const val KEY_DYNAMIC_COLOR = "dynamic_color"
        const val KEY_DEFAULT_REPO = "default_repo_id"
        const val KEY_TASK_NOTIFICATION = "task_notification_enabled"
    }
}
