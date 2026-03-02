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

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.diagnostics.app.ui.screens.*
import com.diagnostics.app.ui.theme.DiagnosticTheme
import com.diagnostics.app.utils.LanguageManager
import com.diagnostics.app.utils.PermissionHandler
import kotlinx.coroutines.runBlocking
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diagnostics.app.viewmodel.AuthViewModel
import com.diagnostics.app.viewmodel.AuthState

class MainActivity : AppCompatActivity() {
    private lateinit var languageManager: LanguageManager
    private lateinit var permissionHandler: PermissionHandler
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        languageManager = LanguageManager(this)
        permissionHandler = PermissionHandler(this)
        
        setContent {
            val currentLanguage by languageManager.currentLanguage.collectAsState(initial = "en")
            val authViewModel: AuthViewModel = viewModel()
            val authState by authViewModel.authState.collectAsState()
            
            // Determine start destination based on auth state
            val startDestination = when (authState) {
                is AuthState.Authenticated -> "home"
                is AuthState.Guest -> "home"
                is AuthState.Unauthenticated -> "welcome"
            }
            
            DiagnosticTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
                        // ══════ Auth Flow ══════
                        composable("welcome") {
                            WelcomeScreen(
                                onLoginClick = { navController.navigate("login") },
                                onRegisterClick = { navController.navigate("register") },
                                onGuestClick = {
                                    authViewModel.continueAsGuest()
                                    navController.navigate("home") {
                                        popUpTo("welcome") { inclusive = true }
                                    }
                                }
                            )
                        }
                        
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate("home") {
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
                                    navController.navigate("home") {
                                        popUpTo("welcome") { inclusive = true }
                                    }
                                },
                                onBackClick = { navController.popBackStack() },
                                authViewModel = authViewModel
                            )
                        }
                        
                        // ══════ Main App Flow ══════
                        composable("home") {
                            LaunchedEffect(navController.currentBackStackEntry) {
                                val route = navController.currentBackStackEntry?.destination?.route
                            }

                            HomeScreen(
                                currentLanguage = currentLanguage,
                                onLanguageChange = { lang ->
                                    runBlocking {
                                        languageManager.setLanguage(lang)
                                    }
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
                                    navController.navigate("labeling/${scanData.scanId}")
                                }
                            )
                        }
                        
                        composable("labeling/{scanId}") { backStackEntry ->
                            val scanId = backStackEntry.arguments?.getString("scanId") ?: ""
                            LabelingScreen(
                                scanId = scanId,
                                onComplete = {
                                    navController.navigate("result") {
                                        popUpTo("home") { inclusive = false }
                                    }
                                }
                            )
                        }
                        
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
        }
    }
}
