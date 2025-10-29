package me.miguelantonyortegasanta.tradumemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import me.miguelantonyortegasanta.tradumemo.ui.theme.TraduMemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TraduMemoTheme {
                AppNavigation()
            }
        }
    }
}

