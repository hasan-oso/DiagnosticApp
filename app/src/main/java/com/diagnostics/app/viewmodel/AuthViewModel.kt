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
package com.diagnostics.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diagnostics.app.data.models.auth.RegistrationData
import com.diagnostics.app.data.models.auth.ShopAccount
import com.diagnostics.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * AuthViewModel — Business logic for authentication flows.
 * Manages login, registration, and auth state.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(application.applicationContext)

    private val _authState = MutableStateFlow<AuthState>(
        if (authRepository.isLoggedIn) AuthState.Authenticated(null)
        else AuthState.Unauthenticated
    )
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState.asStateFlow()

    init {
        // Load shop profile if already logged in
        if (authRepository.isLoggedIn) {
            viewModelScope.launch {
                val profile = authRepository.getShopProfile()
                _authState.value = AuthState.Authenticated(profile)
            }
        }
    }

    // ══════════════════════════════════════════════
    // Login
    // ══════════════════════════════════════════════

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginState.value = LoginState.Error("Email and password required")
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginState.Loading

            val result = authRepository.login(email, password)
            result.fold(
                onSuccess = { account ->
                    _loginState.value = LoginState.Success(account)
                    _authState.value = AuthState.Authenticated(account)
                },
                onFailure = { error ->
                    _loginState.value = LoginState.Error(
                        error.message ?: "Login failed"
                    )
                }
            )
        }
    }

    // ══════════════════════════════════════════════
    // Register
    // ══════════════════════════════════════════════

    fun register(data: RegistrationData) {
        if (data.email.isBlank() || data.password.isBlank()) {
            _registerState.value = RegisterState.Error("Email and password required")
            return
        }

        if (data.password.length < 6) {
            _registerState.value = RegisterState.Error("Password must be at least 6 characters")
            return
        }

        viewModelScope.launch {
            _registerState.value = RegisterState.Loading

            val result = authRepository.register(data)
            result.fold(
                onSuccess = { account ->
                    _registerState.value = RegisterState.Success(account)
                    _authState.value = AuthState.Authenticated(account)
                },
                onFailure = { error ->
                    _registerState.value = RegisterState.Error(
                        error.message ?: "Registration failed"
                    )
                }
            )
        }
    }

    // ══════════════════════════════════════════════
    // Logout & Guest
    // ══════════════════════════════════════════════

    fun logout() {
        authRepository.logout()
        _authState.value = AuthState.Unauthenticated
        _loginState.value = LoginState.Idle
        _registerState.value = RegisterState.Idle
    }

    fun continueAsGuest() {
        _authState.value = AuthState.Guest
    }

    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }

    fun resetRegisterState() {
        _registerState.value = RegisterState.Idle
    }
}

// ══════════════════════════════════════════════
// State Classes
// ══════════════════════════════════════════════

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Guest : AuthState()
    data class Authenticated(val account: ShopAccount?) : AuthState()
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val account: ShopAccount) : LoginState()
    data class Error(val message: String) : LoginState()
}

sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    data class Success(val account: ShopAccount) : RegisterState()
    data class Error(val message: String) : RegisterState()
}
