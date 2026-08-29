package com.boikhata

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.boikhata.core.designsystem.theme.BoiKhataTheme
import com.boikhata.feature.home.HomeScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BoiKhataTheme {
                // P1: login → home flow. For now, the home screen is shown directly
                // with the seed tenant id; the login screen will gate this in the
                // full UX build. The seed (t_1) is the single-tenant offline default.
                HomeScreen(tenantId = "t_1")
            }
        }
    }
}
