package com.example.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.SubscriptionStatus
import com.example.ui.theme.*
import com.example.viewmodel.AppViewModel

// ==========================================================
// 1. FULL APP INACTIVE / SUSPENDED LOCKOUT SCREEN
// ==========================================================
@Composable
fun AppSubscriptionInactiveScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val agreedAmount by viewModel.agreedMonthlyAmount.collectAsState()
    val paybillAcc by viewModel.paybillAccount.collectAsState()
    val isProcessing by viewModel.paymentProcessing.collectAsState()
    val subscriptionToast by viewModel.subscriptionToast.collectAsState()
    
    var showPaySheet by remember { mutableStateOf(false) }
    var phoneNumber by remember { mutableStateOf("0712345678") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Lock Badge Header
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(RedAccent.copy(alpha = 0.15f))
                    .border(2.dp, RedAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Subscription Inactive",
                    tint = RedAccent,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "SYSTEM SUBSCRIPTION INACTIVE",
                color = RedAccent,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "St. Mary's Academy Fleet Account #SAF-8832",
                color = TextSecondaryColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Explanation Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceColor)
                    .border(1.dp, RedAccent.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Warning",
                        tint = RedAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Agreed Payment Overdue",
                        color = TextPrimaryColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "The monthly SaaS subscription fee of $agreedAmount for this school bus fleet management platform has not been settled. Real-time GPS tracking, parent alerts, and driver dispatch are temporarily suspended.",
                    color = TextSecondaryColor,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                HorizontalDivider(color = BorderColor)

                Spacer(modifier = Modifier.height(12.dp))

                // Billing breakdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Agreed Fee:", color = TextTertiaryColor, fontSize = 11.sp)
                    Text(text = agreedAmount, color = TextPrimaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Payment Method:", color = TextTertiaryColor, fontSize = 11.sp)
                    Text(text = paybillAcc, color = TextPrimaryColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action: Pay Now Button
            Button(
                onClick = { showPaySheet = true },
                colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("inactive_pay_now_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Payment,
                    contentDescription = "Pay",
                    tint = BackgroundColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Settle Payment Now & Reactivate",
                    color = BackgroundColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Simulation Quick Controls for Testing
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface2Color)
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ADMIN TEST CONTROLS (QUICK SWITCH):",
                    color = TextTertiaryColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.setSubscriptionStatus(SubscriptionStatus.ACTIVE) },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("inactive_reactivate_test_btn")
                    ) {
                        Text("Reactivate (Set Active)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BackgroundColor)
                    }

                    OutlinedButton(
                        onClick = { viewModel.signOut() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedAccent),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("inactive_sign_out_btn")
                    ) {
                        Text("Admin Logout", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (subscriptionToast != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = subscriptionToast ?: "",
                    color = GreenAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // Payment Dialog Sheet
    if (showPaySheet) {
        SubscriptionPaymentModal(
            viewModel = viewModel,
            onDismiss = { showPaySheet = false }
        )
    }
}

// ==========================================================
// 2. ADMIN SUBSCRIPTION MANAGEMENT CARD
// ==========================================================
@Composable
fun AdminSubscriptionManagementCard(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val status by viewModel.subscriptionStatus.collectAsState()
    val agreedAmount by viewModel.agreedMonthlyAmount.collectAsState()
    val planName by viewModel.subscriptionPlanName.collectAsState()
    val dueDate by viewModel.nextDueDate.collectAsState()
    val paybillAcc by viewModel.paybillAccount.collectAsState()
    val invoices by viewModel.invoiceHistory.collectAsState()
    val toastMsg by viewModel.subscriptionToast.collectAsState()

    var showPayDialog by remember { mutableStateOf(false) }

    val statusColor = when (status) {
        SubscriptionStatus.ACTIVE -> GreenAccent
        SubscriptionStatus.PAYMENT_DUE -> AmberAccent
        SubscriptionStatus.INACTIVE -> RedAccent
    }

    val statusLabel = when (status) {
        SubscriptionStatus.ACTIVE -> "ACTIVE (OPERATIONAL)"
        SubscriptionStatus.PAYMENT_DUE -> "PAYMENT DUE SOON"
        SubscriptionStatus.INACTIVE -> "INACTIVE (APP LOCKED)"
    }

    SafiriCard(
        testTag = "admin_subscription_management_card",
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Payment,
                    contentDescription = "Subscription",
                    tint = PurpleAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ADMIN FLEET SUBSCRIPTION & LICENSING",
                    color = PurpleAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
            }

            // Status Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(statusColor.copy(alpha = 0.15f))
                    .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = statusLabel,
                    color = statusColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Details Panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceColor)
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Text(
                text = planName,
                color = TextPrimaryColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Agreed Amount: $agreedAmount",
                color = AccentBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Next Billing Date:", color = TextTertiaryColor, fontSize = 11.sp)
                Text(text = dueDate, color = TextPrimaryColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Paybill / Bank:", color = TextTertiaryColor, fontSize = 11.sp)
                Text(text = paybillAcc, color = TextSecondaryColor, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Pay / Renew Button
        Button(
            onClick = { showPayDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_pay_subscription_btn"),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.CreditCard,
                contentDescription = "Pay Invoice",
                tint = BackgroundColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Settle Invoice / Make Payment",
                color = BackgroundColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SIMULATION TESTER BUTTONS
        Text(
            text = "TEST SIMULATION (TOGGLE APP SUBSCRIPTION STATE):",
            color = TextTertiaryColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.setSubscriptionStatus(SubscriptionStatus.ACTIVE) },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (status == SubscriptionStatus.ACTIVE) GreenAccent.copy(alpha = 0.2f) else SurfaceColor,
                    contentColor = if (status == SubscriptionStatus.ACTIVE) GreenAccent else TextSecondaryColor
                ),
                border = BorderStroke(1.dp, if (status == SubscriptionStatus.ACTIVE) GreenAccent else BorderColor),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .testTag("set_status_active_btn")
            ) {
                Text("Active", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { viewModel.setSubscriptionStatus(SubscriptionStatus.PAYMENT_DUE) },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (status == SubscriptionStatus.PAYMENT_DUE) AmberAccent.copy(alpha = 0.2f) else SurfaceColor,
                    contentColor = if (status == SubscriptionStatus.PAYMENT_DUE) AmberAccent else TextSecondaryColor
                ),
                border = BorderStroke(1.dp, if (status == SubscriptionStatus.PAYMENT_DUE) AmberAccent else BorderColor),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .testTag("set_status_due_btn")
            ) {
                Text("Due", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { viewModel.setSubscriptionStatus(SubscriptionStatus.INACTIVE) },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (status == SubscriptionStatus.INACTIVE) RedAccent.copy(alpha = 0.2f) else SurfaceColor,
                    contentColor = if (status == SubscriptionStatus.INACTIVE) RedAccent else TextSecondaryColor
                ),
                border = BorderStroke(1.dp, if (status == SubscriptionStatus.INACTIVE) RedAccent else BorderColor),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .testTag("set_status_inactive_btn")
            ) {
                Text("Lock (Inactive)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Invoice History Table
        Text(
            text = "INVOICE & RECEIPT HISTORY",
            color = TextTertiaryColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            invoices.forEach { inv ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceColor)
                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = inv.id,
                                color = TextPrimaryColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(GreenAccent.copy(alpha = 0.15f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = inv.status,
                                    color = GreenAccent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "${inv.date} • ${inv.paymentMethod}",
                            color = TextSecondaryColor,
                            fontSize = 10.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = inv.amount,
                            color = TextPrimaryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "#${inv.receiptNumber}",
                            color = TextTertiaryColor,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }

        if (toastMsg != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(PurpleAccent.copy(alpha = 0.15f))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = toastMsg ?: "",
                    color = TextPrimaryColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                IconButton(
                    onClick = { viewModel.clearSubscriptionToast() },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Dismiss",
                        tint = TextSecondaryColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }

    if (showPayDialog) {
        SubscriptionPaymentModal(
            viewModel = viewModel,
            onDismiss = { showPayDialog = false }
        )
    }
}

// ==========================================================
// 3. PAYMENT MODAL (M-PESA / CARD)
// ==========================================================
@Composable
fun SubscriptionPaymentModal(
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    var phoneNumber by remember { mutableStateOf("0712345678") }
    var amount by remember { mutableStateOf("15000") }
    val isProcessing by viewModel.paymentProcessing.collectAsState()

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        containerColor = SurfaceColor,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Payment,
                    contentDescription = "Payment",
                    tint = GreenAccent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SETTLE SUBSCRIPTION INVOICE",
                    color = TextPrimaryColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "M-Pesa Express / Bank Card STK Push",
                    color = TextSecondaryColor,
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("M-Pesa Phone Number") },
                    placeholder = { Text("e.g. 0712345678") },
                    leadingIcon = {
                        Icon(Icons.Filled.Phone, contentDescription = "Phone", tint = GreenAccent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_phone_input")
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Agreed Amount (KES)") },
                    leadingIcon = {
                        Icon(Icons.Filled.AttachMoney, contentDescription = "Amount", tint = AmberAccent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_amount_input")
                )

                if (isProcessing) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(GreenAccent.copy(alpha = 0.15f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = GreenAccent,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Processing STK Push...",
                                color = TextPrimaryColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Please enter M-Pesa PIN on your phone",
                                color = TextSecondaryColor,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.paySubscriptionInvoice(phoneNumber, amount)
                    onDismiss()
                },
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                modifier = Modifier.testTag("confirm_pay_btn")
            ) {
                Text("Confirm & Pay KES $amount", color = BackgroundColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            if (!isProcessing) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextSecondaryColor)
                }
            }
        }
    )
}
