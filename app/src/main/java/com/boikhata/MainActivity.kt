package com.boikhata

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.boikhata.core.designsystem.theme.BoiKhataTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BoiKhataTheme {
                // P2a: bottom-nav navigation (Home/Catalog/Khata).
                // Seed tenant t_1 — single-tenant offline default (P1 seeder).
                BoiKhataMainScreen(tenantId = "t_1", shopName = "দোকান ১")
            }
        }
    }
}
