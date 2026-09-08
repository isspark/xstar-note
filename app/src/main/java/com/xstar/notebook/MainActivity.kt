package com.xstar.notebook

import android.graphics.Color
import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import com.xstar.notebook.data.settings.ThemeMode
import com.xstar.notebook.data.db.entity.InboxItemEntity
import com.xstar.notebook.ui.theme.XstarTheme
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val createTodoSignal = mutableLongStateOf(0L)
    private val inboxSignal = mutableLongStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        val settings = (application as XstarApplication).container.settings
        setContent {
            val themeMode by settings.themeMode.collectAsState()
            val dynamicColor by settings.dynamicColor.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            SideEffect {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                    navigationBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                )
            }
            XstarTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
                com.xstar.notebook.ui.MainScreen(createTodoSignal.longValue, inboxSignal.longValue)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
            if (text.isNotBlank()) {
                val title = intent.getStringExtra(Intent.EXTRA_TITLE)?.trim().orEmpty()
                val isLink = text.startsWith("http://") || text.startsWith("https://")
                lifecycleScope.launch {
                    runCatching {
                        (application as XstarApplication).container.inboxRepository.add(
                            type = if (isLink) InboxItemEntity.TYPE_LINK else InboxItemEntity.TYPE_TEXT,
                            title = title,
                            content = if (isLink) "" else text,
                            url = if (isLink) text else null,
                            source = intent.type,
                        )
                    }.onSuccess { inboxSignal.longValue = System.currentTimeMillis() }
                }
            }
            return
        }
        if (intent?.getBooleanExtra(EXTRA_CREATE_TODO, false) == true) {
            createTodoSignal.longValue = System.currentTimeMillis()
            intent.removeExtra(EXTRA_CREATE_TODO)
        }
    }

    companion object {
        const val EXTRA_CREATE_TODO = "create_todo"
    }
}
