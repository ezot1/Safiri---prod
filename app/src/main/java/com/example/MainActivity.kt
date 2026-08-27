package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
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
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.GreenAccent
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppViewModel
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val viewModel: AppViewModel = viewModel()
      val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
      val isSystemDark = isSystemInDarkTheme()
      val darkTheme = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemDark
      }

      MyApplicationTheme(darkTheme = darkTheme) {
        val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
        val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
        val subscriptionStatus by viewModel.subscriptionStatus.collectAsStateWithLifecycle()
        val showAiDialog by viewModel.showAiAssistantDialog.collectAsStateWithLifecycle()

        Scaffold(
          modifier = Modifier.fillMaxSize(),
          containerColor = BackgroundColor
        ) { innerPadding ->
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
              MovableAiAssistantFab(
                onClick = { viewModel.openAiAssistantDialog() }
              )
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

@Composable
fun MovableAiAssistantFab(
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val density = LocalDensity.current
  var offsetX by remember { mutableFloatStateOf(0f) }
  var offsetY by remember { mutableFloatStateOf(0f) }
  var isInitialized by remember { mutableStateOf(false) }
  var isDragging by remember { mutableStateOf(false) }
  var totalDragDistance by remember { mutableFloatStateOf(0f) }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val fabSize = 56.dp
    val fabSizePx = with(density) { fabSize.toPx() }
    val paddingPx = with(density) { 16.dp.toPx() }
    val bottomNavClearancePx = with(density) { 110.dp.toPx() } // Placed 110dp above bottom so it never covers the "Me" button

    if (!isInitialized && constraints.maxWidth > 0 && constraints.maxHeight > 0) {
      offsetX = (constraints.maxWidth.toFloat() - fabSizePx - paddingPx).coerceAtLeast(paddingPx)
      offsetY = (constraints.maxHeight.toFloat() - fabSizePx - bottomNavClearancePx).coerceAtLeast(paddingPx)
      isInitialized = true
    }

    FloatingActionButton(
      onClick = {
        if (totalDragDistance < 20f) {
          onClick()
        }
      },
      containerColor = GreenAccent,
      contentColor = Color.White,
      elevation = FloatingActionButtonDefaults.elevation(
        defaultElevation = if (isDragging) 12.dp else 6.dp,
        pressedElevation = 12.dp
      ),
      modifier = Modifier
        .offset {
          IntOffset(
            offsetX.roundToInt().coerceIn(0, (constraints.maxWidth - fabSizePx.toInt()).coerceAtLeast(0)),
            offsetY.roundToInt().coerceIn(0, (constraints.maxHeight - fabSizePx.toInt()).coerceAtLeast(0))
          )
        }
        .pointerInput(Unit) {
          detectDragGestures(
            onDragStart = {
              isDragging = true
              totalDragDistance = 0f
            },
            onDragEnd = {
              isDragging = false
            },
            onDragCancel = {
              isDragging = false
            },
            onDrag = { change, dragAmount ->
              change.consume()
              totalDragDistance += kotlin.math.abs(dragAmount.x) + kotlin.math.abs(dragAmount.y)
              offsetX = (offsetX + dragAmount.x).coerceIn(
                paddingPx,
                (constraints.maxWidth.toFloat() - fabSizePx - paddingPx).coerceAtLeast(paddingPx)
              )
              offsetY = (offsetY + dragAmount.y).coerceIn(
                paddingPx,
                (constraints.maxHeight.toFloat() - fabSizePx - paddingPx).coerceAtLeast(paddingPx)
              )
            }
          )
        }
        .testTag("global_gemini_ai_fab")
    ) {
      Icon(Icons.Filled.AutoAwesome, contentDescription = "Safiri AI Assistant")
    }
  }
}

