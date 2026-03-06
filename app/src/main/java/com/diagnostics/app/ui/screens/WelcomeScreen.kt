/*
 * Copyright (c) 2025 Hasan Oso. All Rights Reserved.
 * 
 * PROPRIETARY SOFTWARE - DO NOT DISTRIBUTE
 * 
 * This file is part of DiagnosticApp.
 * Unauthorized use is strictly prohibited.
 * 
 * Owner: Hasan Oso
 * Student ID: 230212925
 * Institution: OSTIM Technical University
 */
package com.diagnostics.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diagnostics.app.utils.LanguageManager
import com.diagnostics.app.utils.language.LocalIsRtl
import com.diagnostics.app.utils.language.LocalStrings
import com.diagnostics.app.viewmodel.AuthViewModel
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import com.diagnostics.app.R

/**
 * WelcomeScreen — First screen shown to unauthenticated users.
 * Offers language selection, login, register, or continue as guest.
 */
@Composable
fun WelcomeScreen(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onQrScanClick: () -> Unit,
    onScanWithoutAccount: () -> Unit = {},
    onLanguageChange: (String) -> Unit = {},
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val isRtl = LocalIsRtl.current
    var currentLanguage by remember { mutableStateOf(LanguageManager.getLanguage(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
            // Brain / Network Icon - Could be an Image, using Text placeholder matching design request
            Text(
                text = "🧠",
                fontSize = 64.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Logo / Title as Gradient
            Text(
                text = strings.welcomeTitle,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                style = androidx.compose.ui.text.TextStyle(
                    brush = Brush.horizontalGradient(listOf(Color(0xFF6200EA), Color(0xFF00E5FF)))
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = strings.tagline,
                fontSize = 14.sp,
                color = Color.LightGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Language Selection Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LanguageManager.Language.values().forEach { lang ->
                        val isSelected = currentLanguage == lang
                        Text(
                            text = lang.flag,
                            fontSize = 32.sp,
                            modifier = Modifier
                                .clickable {
                                    currentLanguage = lang
                                    onLanguageChange(lang.code)
                                }
                                .padding(8.dp)
                                .background(
                                    if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Login Button (Gradient)
            Button(
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.horizontalGradient(listOf(Color(0xFF6200EA), Color(0xFF00E5FF))), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(strings.loginButton, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Register Button (Outlined)
            OutlinedButton(
                onClick = onRegisterClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF))
            ) {
                Text(strings.registerButton, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // QR Scan Button
            OutlinedButton(
                onClick = onQrScanClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6200EA)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6200EA))
            ) {
                Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(strings.scanQrCode, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Divider + "أو" ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(modifier = Modifier.weight(1f), color = Color(0xFF2D2D44))
                Text(
                    strings.orText,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 12.dp),
                    fontSize = 12.sp
                )
                Divider(modifier = Modifier.weight(1f), color = Color(0xFF2D2D44))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Scan Without Account ──
            TextButton(
                onClick = onScanWithoutAccount,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    strings.scanWithoutAccount,
                    color = Color(0xFF00E5FF).copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    textDecoration = TextDecoration.Underline
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
}
