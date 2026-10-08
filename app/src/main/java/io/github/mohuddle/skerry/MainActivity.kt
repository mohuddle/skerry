package io.github.mohuddle.skerry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.mohuddle.skerry.ui.SettingsScreen
import io.github.mohuddle.skerry.ui.SkerryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SkerryTheme {
                SettingsScreen()
            }
        }
    }
}
