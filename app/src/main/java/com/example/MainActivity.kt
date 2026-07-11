package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.models.UserRole
import com.example.screens.AdminDashboard
import com.example.screens.DriverDashboard
import com.example.screens.LoginScreen
import com.example.screens.ParentDashboard
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: AppViewModel = viewModel()
        val currentUser by viewModel.currentUser.collectAsState()
        val currentRole by viewModel.currentRole.collectAsState()

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          val modifier = Modifier.padding(innerPadding)
          if (currentUser == null || currentRole == null) {
            LoginScreen(viewModel = viewModel, modifier = modifier)
          } else {
            when (currentRole) {
              UserRole.PARENT -> ParentDashboard(viewModel = viewModel, modifier = modifier)
              UserRole.DRIVER -> DriverDashboard(viewModel = viewModel, modifier = modifier)
              UserRole.ADMIN -> AdminDashboard(viewModel = viewModel, modifier = modifier)
              null -> LoginScreen(viewModel = viewModel, modifier = modifier)
            }
          }
        }
      }
    }
  }
}
