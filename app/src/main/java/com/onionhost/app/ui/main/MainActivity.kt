package com.onionhost.app.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.onionhost.app.ui.navigation.AppNavigation
import com.onionhost.app.ui.theme.OnionHostTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OnionHostTheme {
                AppNavigation()
            }
        }
    }
}
