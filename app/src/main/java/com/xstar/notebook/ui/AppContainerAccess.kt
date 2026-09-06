package com.xstar.notebook.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.xstar.notebook.AppContainer
import com.xstar.notebook.XstarApplication

@Composable
fun appContainer(): AppContainer =
    (LocalContext.current.applicationContext as XstarApplication).container