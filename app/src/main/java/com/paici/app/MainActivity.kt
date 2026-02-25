package com.paici.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.paici.app.navigation.AppNavigation
import com.paici.app.ui.theme.拍词Theme

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            拍词Theme {
                AppNavigation(viewModel = viewModel)
            }
        }
    }
}
