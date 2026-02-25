package com.digia.cleverTap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun DigiaNudgeView(content: @Composable () -> Unit) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        DigiaUIClevertapManager.init(context)
    }

    DisposableEffect(Unit) {
        val id = DigiaUIClevertapManager.registerActiveNudgeView()
        onDispose {
            DigiaUIClevertapManager.unregisterActiveNudgeView(id)
        }
    }

    content()
}
