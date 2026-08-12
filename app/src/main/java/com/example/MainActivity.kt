package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.components.AppSubscriptionInactiveScreen
import com.example.components.SafiriAIAssistantDialog
import com.example.models.SubscriptionStatus
import com.example.models.UserRole
import com.example.screens.AdminDashboard
import com.example.screens.DriverDashboard
import com.example.screens.LoginScreen
import com.example.screens.ParentDashboard
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.GreenAccent
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: AppViewModel = viewModel()
        val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
        val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
        val subscriptionStatus by viewModel.subscriptionStatus.collectAsStateWithLifecycle()
        val showAiDialog by viewModel.showAiAssistantDialog.collectAsStateWithLifecycle()

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          val modifier = Modifier.padding(innerPadding)
          Box(modifier = Modifier.fillMaxSize()) {
            if (currentUser == null || currentRole == null) {
              LoginScreen(viewModel = viewModel, modifier = modifier)
            } else if (subscriptionStatus == SubscriptionStatus.INACTIVE) {
              AppSubscriptionInactiveScreen(viewModel = viewModel, modifier = modifier)
            } else {
              when (currentRole) {
                UserRole.PARENT -> ParentDashboard(viewModel = viewModel, modifier = modifier)
                UserRole.DRIVER -> DriverDashboard(viewModel = viewModel, modifier = modifier)
                UserRole.ADMIN -> AdminDashboard(viewModel = viewModel, modifier = modifier)
                else -> {}
              }
            }

            if (currentUser != null && currentRole != null) {
              FloatingActionButton(
                onClick = { viewModel.openAiAssistantDialog() },
                containerColor = GreenAccent,
                contentColor = BackgroundColor,
                modifier = Modifier
                  .align(Alignment.BottomEnd)
                  .padding(16.dp)
                  .testTag("global_gemini_ai_fab")
              ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = "Safiri AI")
              }
            }

            if (showAiDialog) {
              SafiriAIAssistantDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.dismissAiAssistantDialog() }
              )
            }
          }
        }
      }
    }
  }
}
