package com.example.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.AppViewModel

enum class AIMode {
    FAST_LITE,       // gemini-3.1-flash-lite-preview (Low latency)
    MAPS_GROUNDED,   // gemini-3.5-flash with googleMaps tool
    HIGH_THINKING,   // gemini-3.1-pro-preview with thinkingLevel = HIGH
    GENERAL_ANALYSIS // gemini-3.5-flash & gemini-3.1-pro-preview
}

@Composable
fun SafiriAIAssistantDialog(
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(AIMode.FAST_LITE) }
    var userPrompt by remember { mutableStateOf("") }
    var aiResponse by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val isFirebaseActive = viewModel.isFirebaseAuthEnabled
    val isFirestoreActive = viewModel.isFirestoreEnabled

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {},
        containerColor = BackgroundColor,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GreenAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = "AI",
                                tint = GreenAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Safiri Gemini AI",
                                color = TextPrimaryColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Powered by Google Gemini Models",
                                color = TextSecondaryColor,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = TextSecondaryColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Firebase & Firestore Sync Status Indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceColor)
                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CloudSync,
                            contentDescription = "Cloud",
                            tint = if (isFirestoreActive) GreenAccent else AmberAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isFirestoreActive) "Firestore Cloud Sync Active" else "Local Database Mode",
                            color = TextSecondaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isFirebaseActive) GreenAccent else TextTertiaryColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isFirebaseActive) "Firebase Auth" else "Local Auth",
                            color = if (isFirebaseActive) GreenAccent else TextTertiaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mode Selector Chips
                Text(
                    text = "SELECT AI MODEL MODE",
                    color = TextTertiaryColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Mode 1: Fast Lite (Low Latency)
                    FilterChip(
                        selected = selectedMode == AIMode.FAST_LITE,
                        onClick = {
                            selectedMode = AIMode.FAST_LITE
                            userPrompt = "Give me a 1-sentence fast ETA check for Bus KDE 732X in Kilimani traffic."
                        },
                        label = { Text("⚡ Low-Latency", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenAccent,
                            selectedLabelColor = BackgroundColor,
                            containerColor = SurfaceColor,
                            labelColor = TextPrimaryColor
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    // Mode 2: Maps Grounded
                    FilterChip(
                        selected = selectedMode == AIMode.MAPS_GROUNDED,
                        onClick = {
                            selectedMode = AIMode.MAPS_GROUNDED
                            userPrompt = "What is the current traffic on Ngong Road and Kilimani school bus routes in Nairobi?"
                        },
                        label = { Text("📍 Maps Grounded", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentBlue,
                            selectedLabelColor = BackgroundColor,
                            containerColor = SurfaceColor,
                            labelColor = TextPrimaryColor
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Mode 3: High Thinking Mode
                    FilterChip(
                        selected = selectedMode == AIMode.HIGH_THINKING,
                        onClick = {
                            selectedMode = AIMode.HIGH_THINKING
                            userPrompt = "Provide a comprehensive multi-factor safety risk assessment and contingency strategy for school transport during heavy rains in Upper Hill Nairobi."
                        },
                        label = { Text("🧠 High Thinking", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF8B5CF6), // Purple
                            selectedLabelColor = Color.White,
                            containerColor = SurfaceColor,
                            labelColor = TextPrimaryColor
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    // Mode 4: General Analysis
                    FilterChip(
                        selected = selectedMode == AIMode.GENERAL_ANALYSIS,
                        onClick = {
                            selectedMode = AIMode.GENERAL_ANALYSIS
                            userPrompt = "Draft a clear, reassuring update message to parents regarding a 15-minute traffic delay."
                        },
                        label = { Text("📝 Auto Draft", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberAccent,
                            selectedLabelColor = BackgroundColor,
                            containerColor = SurfaceColor,
                            labelColor = TextPrimaryColor
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mode Explanation Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Surface2Color)
                        .padding(8.dp)
                ) {
                    val description = when (selectedMode) {
                        AIMode.FAST_LITE -> "⚡ Model: gemini-3.1-flash-lite-preview — Optimized for low-latency, rapid status updates and instant queries."
                        AIMode.MAPS_GROUNDED -> "📍 Model: gemini-3.5-flash with Google Maps tool — Grounded with live location, traffic, and place data."
                        AIMode.HIGH_THINKING -> "🧠 Model: gemini-3.1-pro-preview with thinkingLevel = HIGH — Deep multi-step reasoning for complex safety analysis."
                        AIMode.GENERAL_ANALYSIS -> "📝 Model: gemini-3.5-flash / pro — General content drafting, summaries, and edits."
                    }
                    Text(
                        text = description,
                        color = TextSecondaryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Input Field
                OutlinedTextField(
                    value = userPrompt,
                    onValueChange = { userPrompt = it },
                    placeholder = { Text("Ask Gemini AI...", color = TextTertiaryColor, fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_prompt_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceColor,
                        unfocusedContainerColor = SurfaceColor,
                        focusedBorderColor = GreenAccent,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = TextPrimaryColor,
                        unfocusedTextColor = TextPrimaryColor
                    ),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Generate Button
                Button(
                    onClick = {
                        if (userPrompt.isBlank()) return@Button
                        isLoading = true
                        aiResponse = ""

                        when (selectedMode) {
                            AIMode.FAST_LITE -> {
                                viewModel.runFastAiQuery(userPrompt) {
                                    aiResponse = it
                                    isLoading = false
                                }
                            }
                            AIMode.MAPS_GROUNDED -> {
                                viewModel.runMapsGroundedQuery(userPrompt, "Nairobi, Kenya") {
                                    aiResponse = it
                                    isLoading = false
                                }
                            }
                            AIMode.HIGH_THINKING -> {
                                viewModel.runHighThinkingQuery(userPrompt) {
                                    aiResponse = it
                                    isLoading = false
                                }
                            }
                            AIMode.GENERAL_ANALYSIS -> {
                                viewModel.runGeneralAiQuery(userPrompt) {
                                    aiResponse = it
                                    isLoading = false
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("ai_submit_button"),
                    enabled = !isLoading && userPrompt.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (selectedMode) {
                            AIMode.FAST_LITE -> GreenAccent
                            AIMode.MAPS_GROUNDED -> AccentBlue
                            AIMode.HIGH_THINKING -> Color(0xFF8B5CF6)
                            AIMode.GENERAL_ANALYSIS -> AmberAccent
                        }
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = BackgroundColor,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gemini is thinking...", color = BackgroundColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "Send",
                            tint = BackgroundColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Execute AI Prompt",
                            color = BackgroundColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // AI Response Display Box
                if (aiResponse.isNotBlank() || isLoading) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "AI RESPONSE",
                        color = TextTertiaryColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceColor)
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        val scrollState = rememberScrollState()
                        Column(modifier = Modifier.verticalScroll(scrollState)) {
                            Text(
                                text = if (isLoading) "Analyzing transit query with Google Gemini..." else aiResponse,
                                color = TextPrimaryColor,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    )
}
