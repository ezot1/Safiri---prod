package com.example.screens

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.models.UserRole
import com.example.ui.theme.*
import com.example.viewmodel.AppViewModel

@Composable
fun LoginScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isSignUpMode by remember { mutableStateOf(false) }
    var showParentSignupWizard by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(UserRole.PARENT) }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("parent@safiri.co.ke") }
    var password by remember { mutableStateOf("demo1234") }
    var showGoogleDialog by remember { mutableStateOf(false) }
    var isGoogleSigningIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    if (showParentSignupWizard) {
        ParentSignupScreen(
            viewModel = viewModel,
            onBackToLogin = { showParentSignupWizard = false },
            modifier = modifier
        )
        return
    }

    // Pre-fill email based on selected role ONLY in sign in mode
    LaunchedEffect(selectedRole, isSignUpMode) {
        if (!isSignUpMode) {
            email = when (selectedRole) {
                UserRole.PARENT -> "parent@safiri.co.ke"
                UserRole.DRIVER -> "driver@safiri.co.ke"
                UserRole.ADMIN -> "admin@safiri.co.ke"
            }
            errorMessage = null
        } else {
            email = ""
            password = ""
            fullName = ""
            errorMessage = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundColor),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
        Spacer(modifier = Modifier.height(24.dp))

        // 1. Beautiful Hero Card containing Custom Generated Onboarding Art
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
        ) {
            Image(
                painter = painterResource(id = com.example.R.drawable.img_login_hero),
                contentDescription = "Safiri Onboarding Art",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Bottom shadow overlay gradient to integrate nicely with dark layout
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                BackgroundColor.copy(alpha = 0.85f)
                            )
                        )
                    )
            )
            
            // Neon glassmorphic Safiri badge floating centrally
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceColor.copy(alpha = 0.90f))
                    .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = com.example.R.drawable.img_app_logo),
                        contentDescription = "Safiri Logo",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Safiri",
                            color = TextPrimaryColor,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "School bus tracker • Nairobi",
            color = TextSecondaryColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(if (viewModel.isFirebaseAuthEnabled) GreenAccent.copy(alpha = 0.1f) else BorderColor.copy(alpha = 0.5f))
                .border(1.dp, if (viewModel.isFirebaseAuthEnabled) GreenAccent else BorderColor, RoundedCornerShape(100.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (viewModel.isFirebaseAuthEnabled) GreenAccent else TextTertiaryColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (viewModel.isFirebaseAuthEnabled) "Firebase Auth Engaged" else "Local Auth Engine Active",
                color = if (viewModel.isFirebaseAuthEnabled) GreenAccent else TextSecondaryColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- MODERN TABS FOR SIGN IN / SIGN UP ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceColor)
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (!isSignUpMode) AccentBlue else Color.Transparent)
                    .clickable { isSignUpMode = false }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sign In",
                    color = if (!isSignUpMode) BackgroundColor else TextSecondaryColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSignUpMode) AccentBlue else Color.Transparent)
                    .clickable { isSignUpMode = true }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sign Up",
                    color = if (isSignUpMode) BackgroundColor else TextSecondaryColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Error message banner
        if (errorMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(RedAccent.copy(alpha = 0.15f))
                    .border(1.dp, RedAccent, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Error",
                    tint = RedAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = errorMessage ?: "",
                    color = RedAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Section label
        Text(
            text = "CHOOSE YOUR ROLE",
            color = TextTertiaryColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Role Selector Cards
        RoleCard(
            title = "Parent",
            description = "Track your child's bus live and get ETA updates.",
            icon = Icons.Filled.FamilyRestroom,
            selected = selectedRole == UserRole.PARENT,
            accentColor = AccentBlue,
            onClick = { selectedRole = UserRole.PARENT },
            testTag = "role_parent_card"
        )

        Spacer(modifier = Modifier.height(10.dp))

        RoleCard(
            title = "Driver",
            description = "Log routes, track boarding students & send SOS.",
            icon = Icons.Filled.DriveEta,
            selected = selectedRole == UserRole.DRIVER,
            accentColor = GreenAccent,
            onClick = { selectedRole = UserRole.DRIVER },
            testTag = "role_driver_card"
        )

        Spacer(modifier = Modifier.height(10.dp))

        RoleCard(
            title = "School Administrator",
            description = "Monitor the full fleet, manage rosters, send alerts.",
            icon = Icons.Filled.School,
            selected = selectedRole == UserRole.ADMIN,
            accentColor = PurpleAccent,
            onClick = { selectedRole = UserRole.ADMIN },
            testTag = "role_admin_card"
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- FULL NAME (Only shown in Sign Up Mode) ---
        if (isSignUpMode) {
            Text(
                text = "FULL NAME",
                color = TextTertiaryColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(6.dp))
            TextField(
                value = fullName,
                onValueChange = { fullName = it },
                placeholder = { Text("e.g. Ezra Ochieng", color = TextTertiaryColor) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SurfaceColor,
                    unfocusedContainerColor = SurfaceColor,
                    disabledContainerColor = SurfaceColor,
                    focusedTextColor = TextPrimaryColor,
                    unfocusedTextColor = TextPrimaryColor,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .testTag("fullname_input"),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Input Fields with elegant dark styling
        Text(
            text = "EMAIL ADDRESS",
            color = TextTertiaryColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )
        Spacer(modifier = Modifier.height(6.dp))
        TextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("email@safiri.co.ke", color = TextTertiaryColor) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = SurfaceColor,
                unfocusedContainerColor = SurfaceColor,
                disabledContainerColor = SurfaceColor,
                focusedTextColor = TextPrimaryColor,
                unfocusedTextColor = TextPrimaryColor,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .testTag("email_input"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "PASSWORD",
            color = TextTertiaryColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )
        Spacer(modifier = Modifier.height(6.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            visualTransformation = PasswordVisualTransformation(),
            placeholder = { Text("••••••••", color = TextTertiaryColor) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = SurfaceColor,
                unfocusedContainerColor = SurfaceColor,
                disabledContainerColor = SurfaceColor,
                focusedTextColor = TextPrimaryColor,
                unfocusedTextColor = TextPrimaryColor,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .testTag("password_input"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Submit Button (Sign In / Sign Up)
        Button(
            onClick = {
                if (isSignUpMode) {
                    if (selectedRole == UserRole.PARENT) {
                        showParentSignupWizard = true
                    } else {
                        if (fullName.trim().isEmpty() || email.trim().isEmpty() || password.trim().isEmpty()) {
                            errorMessage = "Please fill in all fields."
                        } else {
                            viewModel.signUp(fullName, email, selectedRole, "email", password) { success, message ->
                                if (!success) errorMessage = message
                            }
                        }
                    }
                } else {
                    if (email.trim().isEmpty() || password.trim().isEmpty()) {
                        errorMessage = "Please fill in all fields."
                    } else {
                        viewModel.signIn(email, password, selectedRole) { success, message ->
                            if (!success) errorMessage = message
                        }
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .testTag("submit_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (isSignUpMode) {
                    if (selectedRole == UserRole.PARENT) "Start 3-Step Parent Sign Up →" else "Create Safiri Account"
                } else "Sign In to Safiri",
                color = BackgroundColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- DIVIDER OR ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = BorderColor)
            Text(
                text = "OR",
                color = TextTertiaryColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = BorderColor)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- GOOGLE AUTHENTICATION BUTTON ---
        OutlinedButton(
            onClick = { showGoogleDialog = true },
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = SurfaceColor,
                contentColor = TextPrimaryColor
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .testTag("google_auth_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // A simulated Google 'G' icon colored cleanly
                Text(
                    text = "G",
                    color = AccentBlue,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Text(
                    text = if (isSignUpMode) "Sign Up with Google" else "Sign In with Google",
                    color = TextPrimaryColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
    }

    // --- GOOGLE ACCOUNT CHOOSER DIALOG ---
    if (showGoogleDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleDialog = false },
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("G", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text("o", color = RedAccent, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text("o", color = AmberAccent, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text("g", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text("l", color = GreenAccent, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text("e", color = RedAccent, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Choose an account",
                        color = TextPrimaryColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "to continue to Safiri",
                        color = TextSecondaryColor,
                        fontSize = 12.sp
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Account 1: Ezra Ochieng (Dynamic personalization!)
                    val userEmail = "EzraO652@gmail.com"
                    val userName = "Ezra Ochieng"
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showGoogleDialog = false
                                isGoogleSigningIn = true
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(1200)
                                    if (isSignUpMode) {
                                        viewModel.signUp(userName, userEmail, selectedRole, "google") { success, message ->
                                            isGoogleSigningIn = false
                                            if (!success) errorMessage = message
                                        }
                                    } else {
                                        viewModel.signIn(userEmail, "", selectedRole) { success, message ->
                                            isGoogleSigningIn = false
                                            if (!success) errorMessage = message
                                        }
                                    }
                                }
                            }
                            .background(Surface2Color)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AccentBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "E",
                                color = BackgroundColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = userName,
                                color = TextPrimaryColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = userEmail,
                                color = TextSecondaryColor,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Account 2: Simulated Guest Account
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showGoogleDialog = false
                                isGoogleSigningIn = true
                                val guestEmail = "guest.user@gmail.com"
                                val guestName = "Guest User"
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(1200)
                                    if (isSignUpMode) {
                                        viewModel.signUp(guestName, guestEmail, selectedRole, "google") { success, message ->
                                            isGoogleSigningIn = false
                                            if (!success) errorMessage = message
                                        }
                                    } else {
                                        viewModel.signIn(guestEmail, "", selectedRole) { success, message ->
                                            isGoogleSigningIn = false
                                            if (!success) errorMessage = message
                                        }
                                    }
                                }
                            }
                            .background(Surface2Color)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PurpleAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "G",
                                color = BackgroundColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Guest User",
                                color = TextPrimaryColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "guest.user@gmail.com",
                                color = TextSecondaryColor,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "To continue, Google will share your name, email address, and profile picture with Safiri. See Privacy Policy and Terms.",
                        color = TextTertiaryColor,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showGoogleDialog = false }) {
                    Text("Cancel", color = AccentBlue)
                }
            },
            containerColor = SurfaceColor,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // --- GOOGLE SIGNING IN LOADING OVERLAY ---
    if (isGoogleSigningIn) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            containerColor = SurfaceColor,
            shape = RoundedCornerShape(24.dp),
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = AccentBlue,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Connecting to Google...",
                        color = TextPrimaryColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Securing credential handshakes",
                        color = TextSecondaryColor,
                        fontSize = 11.sp
                    )
                }
            }
        )
    }
}

@Composable
fun RoleCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    val cardBg = if (selected) Surface2Color else SurfaceColor
    val cardBorderColor = if (selected) accentColor else BorderColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(1.dp, cardBorderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) accentColor.copy(alpha = 0.2f) else Surface2Color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "$title Icon",
                tint = if (selected) accentColor else TextSecondaryColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = if (selected) accentColor else TextPrimaryColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Selected",
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = description,
                color = TextSecondaryColor,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
