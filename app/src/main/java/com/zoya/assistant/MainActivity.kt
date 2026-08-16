package com.zoya.assistant

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.zoya.assistant.ui.navigation.ZoyaNavHost
import com.zoya.assistant.ui.screens.PermissionsOnboardingScreen
import com.zoya.assistant.ui.theme.ZoyaTheme
import com.zoya.assistant.util.ZoyaPermissions
import com.zoya.assistant.viewmodel.AssistantViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AssistantViewModel by viewModels()

    private fun allPermissionsGranted(): Boolean =
        ZoyaPermissions.REQUIRED.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ZoyaTheme {
                var granted by remember { mutableStateOf(allPermissionsGranted()) }

                if (granted) {
                    LaunchedEffect(Unit) { viewModel.startBackgroundService() }
                    ZoyaNavHost(viewModel = viewModel)
                } else {
                    PermissionsOnboardingScreen(onAllGranted = { granted = true })
                }
            }
        }
    }
}
