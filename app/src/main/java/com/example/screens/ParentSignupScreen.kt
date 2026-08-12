package com.example.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentSignupScreen(
    viewModel: AppViewModel,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(1) }

    // Form states
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("+254 ") }

    var childName by remember { mutableStateOf("") }
    var childGrade by remember { mutableStateOf("Grade 3") }
    var schoolName by remember { mutableStateOf("St. Austin's Academy") }
    var pickupStop by remember { mutableStateOf("Kawangware Stop") }
    var selectedAvatarIndex by remember { mutableIntStateOf(0) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Dropdown expanded states
    var gradeDropdownExpanded by remember { mutableStateOf(false) }
    var schoolDropdownExpanded by remember { mutableStateOf(false) }
    var stopDropdownExpanded by remember { mutableStateOf(false) }

    val gradeOptions = listOf(
        "PP1", "PP2", "Grade 1", "Grade 2", "Grade 3", "Grade 4", "Grade 5",
        "Grade 6", "Grade 7", "Grade 8", "Grade 9", "Form 1", "Form 2", "Form 3", "Form 4"
    )

    val schoolOptions = listOf(
        "St. Austin's Academy",
        "St. Mary's Academy",
        "Mbagathi Road Primary",
        "Kilimani Heights School",
        "Lavington Primary School"
    )

    val stopOptions = listOf(
        "Kawangware Stop",
        "Kilimani Stop",
        "Westlands Hub",
        "Lavington Green",
        "Upper Hill Stop",
        "Ngong Road Junction"
    )

    val avatarColors = listOf(
        AccentBlue, GreenAccent, AmberAccent, PurpleAccent, RedAccent
    )

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
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Navigation Bar (Back button and Progress Indicator)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        if (step > 1) {
                            step--
                            errorMessage = null
                        } else {
                            onBackToLogin()
                        }
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SurfaceColor)
                        .border(1.dp, BorderColor, CircleShape)
                        .testTag("signup_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimaryColor
                    )
                }

                // Step Progress Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (i in 1..3) {
                        val isCurrent = step == i
                        val isDone = step > i

                        Box(
                            modifier = Modifier
                                .size(if (isCurrent) 32.dp else 24.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isCurrent -> AccentBlue
                                        isDone -> GreenAccent
                                        else -> Surface3Color
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDone) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Done",
                                    tint = BackgroundColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            } else {
                                Text(
                                    text = "$i",
                                    color = if (isCurrent) BackgroundColor else TextSecondaryColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (i < 3) {
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(2.dp)
                                    .background(if (step > i) GreenAccent else Surface3Color)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(40.dp)) // balance layout
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error banner
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
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- STEP CONTENT ---
            when (step) {
                // ==================== STEP 1: ACCOUNT DETAILS ====================
                1 -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Create Parent Account",
                            color = TextPrimaryColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Step 1 of 3 — Account & contact information",
                            color = TextSecondaryColor,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                        )

                        // Full Name
                        Text(
                            text = "FULL NAME",
                            color = TextTertiaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            placeholder = { Text("e.g. Sarah Ochieng", color = TextTertiaryColor) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = SurfaceColor,
                                unfocusedContainerColor = SurfaceColor,
                                focusedTextColor = TextPrimaryColor,
                                unfocusedTextColor = TextPrimaryColor
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("parent_fullname_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Email
                        Text(
                            text = "EMAIL ADDRESS",
                            color = TextTertiaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = { Text("e.g. sarah@safiri.co.ke", color = TextTertiaryColor) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = SurfaceColor,
                                unfocusedContainerColor = SurfaceColor,
                                focusedTextColor = TextPrimaryColor,
                                unfocusedTextColor = TextPrimaryColor
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("parent_email_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Password
                        Text(
                            text = "PASSWORD (MIN 8 CHARACTERS)",
                            color = TextTertiaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            visualTransformation = PasswordVisualTransformation(),
                            placeholder = { Text("••••••••", color = TextTertiaryColor) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = SurfaceColor,
                                unfocusedContainerColor = SurfaceColor,
                                focusedTextColor = TextPrimaryColor,
                                unfocusedTextColor = TextPrimaryColor
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("parent_password_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Phone Number
                        Text(
                            text = "PHONE NUMBER (KENYAN FORMAT +254)",
                            color = TextTertiaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            placeholder = { Text("+254 712 345 678", color = TextTertiaryColor) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = SurfaceColor,
                                unfocusedContainerColor = SurfaceColor,
                                focusedTextColor = TextPrimaryColor,
                                unfocusedTextColor = TextPrimaryColor
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("parent_phone_input")
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = {
                                if (fullName.trim().isEmpty()) {
                                    errorMessage = "Please enter your full name."
                                } else if (!email.contains("@") || email.trim().isEmpty()) {
                                    errorMessage = "Please enter a valid email address."
                                } else if (password.length < 8) {
                                    errorMessage = "Password must be at least 8 characters long."
                                } else if (phone.trim().length < 9) {
                                    errorMessage = "Please enter a valid Kenyan phone number."
                                } else {
                                    errorMessage = null
                                    step = 2
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .testTag("step1_continue_button")
                        ) {
                            Text(
                                text = "Continue to Child Details →",
                                color = BackgroundColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // ==================== STEP 2: CHILD DETAILS ====================
                2 -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Tell Us About Your Child",
                            color = TextPrimaryColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Step 2 of 3 — Student details, grade & bus stop",
                            color = TextSecondaryColor,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                        )

                        // Child's Full Name
                        Text(
                            text = "CHILD'S FULL NAME",
                            color = TextTertiaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = childName,
                            onValueChange = { childName = it },
                            placeholder = { Text("e.g. Liam Ochieng", color = TextTertiaryColor) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = SurfaceColor,
                                unfocusedContainerColor = SurfaceColor,
                                focusedTextColor = TextPrimaryColor,
                                unfocusedTextColor = TextPrimaryColor
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("child_fullname_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Class / Grade Dropdown
                        Text(
                            text = "CLASS / GRADE",
                            color = TextTertiaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ExposedDropdownMenuBox(
                            expanded = gradeDropdownExpanded,
                            onExpandedChange = { gradeDropdownExpanded = !gradeDropdownExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = childGrade,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gradeDropdownExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentBlue,
                                    unfocusedBorderColor = BorderColor,
                                    focusedContainerColor = SurfaceColor,
                                    unfocusedContainerColor = SurfaceColor,
                                    focusedTextColor = TextPrimaryColor,
                                    unfocusedTextColor = TextPrimaryColor
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("child_grade_dropdown")
                            )
                            ExposedDropdownMenu(
                                expanded = gradeDropdownExpanded,
                                onDismissRequest = { gradeDropdownExpanded = false },
                                modifier = Modifier.background(SurfaceColor)
                            ) {
                                gradeOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option, color = TextPrimaryColor) },
                                        onClick = {
                                            childGrade = option
                                            gradeDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // School Name Dropdown / Input
                        Text(
                            text = "SCHOOL NAME",
                            color = TextTertiaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ExposedDropdownMenuBox(
                            expanded = schoolDropdownExpanded,
                            onExpandedChange = { schoolDropdownExpanded = !schoolDropdownExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = schoolName,
                                onValueChange = { schoolName = it },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = schoolDropdownExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentBlue,
                                    unfocusedBorderColor = BorderColor,
                                    focusedContainerColor = SurfaceColor,
                                    unfocusedContainerColor = SurfaceColor,
                                    focusedTextColor = TextPrimaryColor,
                                    unfocusedTextColor = TextPrimaryColor
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("school_name_dropdown")
                            )
                            ExposedDropdownMenu(
                                expanded = schoolDropdownExpanded,
                                onDismissRequest = { schoolDropdownExpanded = false },
                                modifier = Modifier.background(SurfaceColor)
                            ) {
                                schoolOptions.forEach { school ->
                                    DropdownMenuItem(
                                        text = { Text(school, color = TextPrimaryColor) },
                                        onClick = {
                                            schoolName = school
                                            schoolDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Pickup Stop Dropdown
                        Text(
                            text = "PICKUP STOP",
                            color = TextTertiaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ExposedDropdownMenuBox(
                            expanded = stopDropdownExpanded,
                            onExpandedChange = { stopDropdownExpanded = !stopDropdownExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = pickupStop,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stopDropdownExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentBlue,
                                    unfocusedBorderColor = BorderColor,
                                    focusedContainerColor = SurfaceColor,
                                    unfocusedContainerColor = SurfaceColor,
                                    focusedTextColor = TextPrimaryColor,
                                    unfocusedTextColor = TextPrimaryColor
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("pickup_stop_dropdown")
                            )
                            ExposedDropdownMenu(
                                expanded = stopDropdownExpanded,
                                onDismissRequest = { stopDropdownExpanded = false },
                                modifier = Modifier.background(SurfaceColor)
                            ) {
                                stopOptions.forEach { stop ->
                                    DropdownMenuItem(
                                        text = { Text(stop, color = TextPrimaryColor) },
                                        onClick = {
                                            pickupStop = stop
                                            stopDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Child Avatar/Profile Color Selection
                        Text(
                            text = "PROFILE AVATAR COLOR (OPTIONAL)",
                            color = TextTertiaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            avatarColors.forEachIndexed { idx, color ->
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(color.copy(alpha = 0.2f))
                                        .border(
                                            width = if (selectedAvatarIndex == idx) 3.dp else 1.dp,
                                            color = if (selectedAvatarIndex == idx) color else BorderColor,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedAvatarIndex = idx },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Face,
                                        contentDescription = "Avatar",
                                        tint = color,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = {
                                if (childName.trim().isEmpty()) {
                                    errorMessage = "Please enter your child's full name."
                                } else if (schoolName.trim().isEmpty()) {
                                    errorMessage = "Please enter or select a school."
                                } else {
                                    errorMessage = null
                                    step = 3
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .testTag("step2_continue_button")
                        ) {
                            Text(
                                text = "Continue to Confirmation →",
                                color = BackgroundColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // ==================== STEP 3: CONFIRMATION ====================
                3 -> {
                    val parentFirstName = fullName.trim().split(" ").firstOrNull() ?: fullName
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "All Set, $parentFirstName!",
                            color = TextPrimaryColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Step 3 of 3 — Confirm details to start live tracking",
                            color = TextSecondaryColor,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                        )

                        // Confirmation Summary Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(avatarColors[selectedAvatarIndex].copy(alpha = 0.2f))
                                            .border(2.dp, avatarColors[selectedAvatarIndex], CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Face,
                                            contentDescription = "Child Avatar",
                                            tint = avatarColors[selectedAvatarIndex],
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = childName,
                                            color = TextPrimaryColor,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "$childGrade • $schoolName",
                                            color = AccentBlue,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Divider(
                                    modifier = Modifier.padding(vertical = 16.dp),
                                    color = BorderColor
                                )

                                SummaryDetailRow(label = "Parent", value = "$fullName ($phone)")
                                SummaryDetailRow(label = "Email", value = email)
                                SummaryDetailRow(label = "Assigned Stop", value = pickupStop)
                                SummaryDetailRow(label = "Assigned Bus", value = if (pickupStop.contains("Kawangware")) "KBZ 441G" else "KDE 732X")
                                SummaryDetailRow(label = "Data Sync", value = "Supabase Realtime Enabled ✓")
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = {
                                isLoading = true
                                errorMessage = null
                                viewModel.registerParentWithChild(
                                    parentFullName = fullName,
                                    parentEmail = email,
                                    parentPassword = password,
                                    parentPhone = phone,
                                    childFullName = childName,
                                    childGrade = childGrade,
                                    schoolName = schoolName,
                                    pickupStop = pickupStop,
                                    photoUrl = null
                                ) { success, msg ->
                                    isLoading = false
                                    if (!success) {
                                        errorMessage = msg
                                    }
                                }
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .testTag("start_tracking_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = BackgroundColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.DirectionsBus,
                                        contentDescription = "Start",
                                        tint = BackgroundColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Start Live Tracking",
                                        color = BackgroundColor,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextTertiaryColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(text = value, color = TextPrimaryColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
