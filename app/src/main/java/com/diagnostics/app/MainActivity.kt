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
package com.diagnostics.app

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.diagnostics.app.ui.screens.*
import com.diagnostics.app.ui.screens.ReportScreen
import com.diagnostics.app.ui.theme.DiagnosticTheme
import com.diagnostics.app.utils.LanguageManager
import com.diagnostics.app.utils.PermissionHandler
import com.diagnostics.app.utils.language.LocalIsRtl
import com.diagnostics.app.utils.language.LocalStrings
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diagnostics.app.viewmodel.AuthViewModel
import com.diagnostics.app.viewmodel.AuthState
import com.diagnostics.app.viewmodel.InventoryViewModel
import com.diagnostics.app.viewmodel.LanguageViewModel
import com.diagnostics.app.viewmodel.SharedScanViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var permissionHandler: PermissionHandler
    
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.wrapContext(newBase))
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        permissionHandler = PermissionHandler(this)
        
        setContent {
            val authViewModel: AuthViewModel = viewModel()
            val authState by authViewModel.authState.collectAsState()
            val sharedScanViewModel: SharedScanViewModel = viewModel()
            val inventoryViewModel: InventoryViewModel = viewModel()

            // ViewModel اللغة المشترك على مستوى Activity
            val languageViewModel: LanguageViewModel = viewModel()
            val strings by languageViewModel.strings.collectAsState()
            val isRtl by languageViewModel.isRtl.collectAsState()

            // ← startDestination مُحسوب مرة واحدة فقط - لا يتغير عند تغيّر authState
            val startDestination = remember {
                when (authViewModel.authState.value) {
                    is AuthState.Authenticated -> "dashboard"
                    is AuthState.Guest         -> "home"
                    is AuthState.Unauthenticated -> "welcome"
                }
            }

            // تطبيق CompositionLocals على كامل التطبيق
            CompositionLocalProvider(
                LocalStrings provides strings,
                LocalIsRtl provides isRtl,
                LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
            ) {
            DiagnosticTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // تنقّل تلقائي عند تغيّر حالة المصادقة
                    LaunchedEffect(authState) {
                        when (authState) {
                            is AuthState.Unauthenticated -> {
                                navController.navigate("welcome") {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                            else -> {}
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
                        // ══════ Auth Flow ══════
                        composable("welcome") {
                            WelcomeScreen(
                                onLoginClick = { navController.navigate("login") },
                                onRegisterClick = { navController.navigate("register") },
                                onQrScanClick = {
                                    navController.navigate("qr_scan")
                                },
                                onScanWithoutAccount = {
                                    authViewModel.continueAsGuest()
                                    navController.navigate("scanning") {
                                        popUpTo("welcome") { inclusive = true }
                                    }
                                },
                                onLanguageChange = { code ->
                                    val lang = LanguageManager.Language.values()
                                        .find { it.code == code }
                                        ?: LanguageManager.Language.ARABIC
                                    languageViewModel.setLanguage(lang)
                                    recreate()
                                }
                            )
                        }
                        
                        composable("qr_scan") {
                            QrScanScreen(
                                onQrScanned = { shopId ->
                                    inventoryViewModel.loadDashboard(shopId)
                                    navController.navigate("scanning") {
                                        popUpTo("qr_scan") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("generate_qr") {
                            val account = (authState as? AuthState.Authenticated)?.account
                            if (account != null) {
                                GenerateQrScreen(
                                    shopId = account.id,
                                    onBackClick = { navController.popBackStack() },
                                    inventoryViewModel = inventoryViewModel
                                )
                            }
                        }
                        
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate("dashboard") {
                                        popUpTo("welcome") { inclusive = true }
                                    }
                                },
                                onBackClick = { navController.popBackStack() },
                                authViewModel = authViewModel
                            )
                        }
                        
                        composable("register") {
                            RegisterScreen(
                                onRegisterSuccess = {
                                    navController.navigate("dashboard") {
                                        popUpTo("welcome") { inclusive = true }
                                    }
                                },
                                onBackClick = { navController.popBackStack() },
                                authViewModel = authViewModel
                            )
                        }
                        
                        composable("dashboard") {
                            val account = (authState as? AuthState.Authenticated)?.account
                            if (account != null) {
                                DashboardScreen(
                                    shopId = account.id,
                                    shopName = account.shopInfo.shopName,
                                    shopInfo = account.shopInfo,
                                    onGeneratePinClick = { navController.navigate("generate_qr") },
                                    onLogoutClick = {
                                        authViewModel.logout()
                                        // LaunchedEffect(authState) يتولى التنقل تلقائياً
                                    },
                                    onDeviceClick = { /* TODO DeviceDetailScreen */ },
                                    onScanClick = {
                                        if (permissionHandler.hasAllPermissions()) {
                                            navController.navigate("scanning")
                                        } else {
                                            permissionHandler.setOnPermissionsGrantedCallback {
                                                navController.navigate("scanning")
                                            }
                                            permissionHandler.checkAndRequestPermissions()
                                        }
                                    },
                                    onSettingsClick = { navController.navigate("settings") },
                                    inventoryViewModel = inventoryViewModel
                                )
                            }
                        }
                        

                        composable("settings") {
                            val account = (authState as? AuthState.Authenticated)?.account
                            SettingsScreen(
                                shopName = account?.shopInfo?.shopName ?: "",
                                email = account?.email ?: "",
                                onBackClick = { navController.popBackStack() },
                                onLanguageChange = { lang ->
                                    languageViewModel.setLanguage(lang)
                                    recreate()
                                }
                            )
                        }
                        
                        // ══════ Main App Flow ══════
                        composable("home") {
                            HomeScreen(
                                currentLanguage = languageViewModel.language.value.code,
                                onLanguageChange = { langCode ->
                                    val lang = LanguageManager.Language.values()
                                        .find { it.code == langCode } ?: LanguageManager.Language.ARABIC
                                    languageViewModel.setLanguage(lang)
                                    recreate()
                                },
                                onStartScan = {
                                    if (permissionHandler.hasAllPermissions()) {
                                        try {
                                            navController.navigate("scanning")
                                        } catch (e: Exception) {
                                            // Handle error
                                        }
                                    } else {
                                        permissionHandler.setOnPermissionsGrantedCallback {
                                            navController.navigate("scanning")
                                        }
                                        permissionHandler.checkAndRequestPermissions()
                                    }
                                }
                            )
                        }
                        
                        composable("scanning") {
                            ScanningScreen(
                                onScanComplete = { scanData ->
                                    // Store scan data in shared ViewModel
                                    sharedScanViewModel.setScanData(scanData)
                                    navController.navigate("labeling/${scanData.scanId}")
                                }
                            )
                        }
                        
                        composable("labeling/{scanId}") { backStackEntry ->
                            val scanId = backStackEntry.arguments?.getString("scanId") ?: ""
                            LabelingScreen(
                                scanId = scanId,
                                onComplete = {
                                    val shopId = inventoryViewModel.currentShopId.value
                                    if (shopId != null) {
                                        navController.navigate("purchase_price") {
                                            popUpTo("home") { inclusive = false }
                                        }
                                    } else {
                                        navController.navigate("report") {
                                            popUpTo("home") { inclusive = false }
                                        }
                                    }
                                }
                            )
                        }
                        
                        composable("purchase_price") {
                            val scanData by sharedScanViewModel.scanData.collectAsState()
                            
                            scanData?.let { data ->
                                val realScore = calculateOverallScore(data).coerceIn(0f, 100f).toInt()
                                PurchasePriceScreen(
                                    deviceModel = data.deviceModel,
                                    overallScore = realScore,
                                    onSaveClick = { purchasePrice ->
                                        val shopId = inventoryViewModel.currentShopId.value
                                        if (shopId != null) {
                                            inventoryViewModel.saveDeviceAfterScan(data, purchasePrice, realScore, shopId)
                                        }
                                        navController.navigate("report") {
                                            popUpTo("dashboard") { inclusive = false }
                                        }
                                    },
                                    onSkipClick = {
                                        val shopId = inventoryViewModel.currentShopId.value
                                        if (shopId != null) {
                                            inventoryViewModel.saveDeviceAfterScan(data, 0, realScore, shopId)
                                        }
                                        navController.navigate("report") {
                                            popUpTo("dashboard") { inclusive = false }
                                        }
                                    },
                                    onBackClick = { navController.popBackStack() }
                                )
                            } ?: run {
                                navController.navigate("home")
                            }
                        }
                        
                        // ══════ Report Screen (NEW) ══════
                        composable("report") {
                            val scanData by sharedScanViewModel.scanData.collectAsState()
                            
                            scanData?.let { data ->
                                ReportScreen(
                                    scanData = data,
                                    shopInfo = (authState as? AuthState.Authenticated)?.account?.shopInfo,
                                    onBackClick = {
                                        sharedScanViewModel.clearScanData()
                                        if (authState is AuthState.Authenticated || inventoryViewModel.currentShopId.value != null) {
                                            navController.navigate("dashboard") {
                                                popUpTo("dashboard") { inclusive = true }
                                            }
                                        } else {
                                            navController.navigate("welcome") {
                                                popUpTo("welcome") { inclusive = true }
                                            }
                                        }
                                    }
                                )
                            } ?: run {
                                // Fallback: no scan data available
                                ResultScreen(
                                    onBackToHome = {
                                        navController.navigate("welcome") {
                                            popUpTo("welcome") { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }
                        
                        // Keep old result route for backward compat
                        composable("result") {
                            ResultScreen(
                                onBackToHome = {
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
            } // CompositionLocalProvider
        }
    }
}
