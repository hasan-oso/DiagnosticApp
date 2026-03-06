package com.diagnostics.app.utils

object PinManager {
    // Generate random 6-digit PIN
    fun generatePin(): String = (100000..999999).random().toString()

    // Valid for 1 hour
    fun isPinValid(createdAt: Long): Boolean {
        return System.currentTimeMillis() - createdAt < 3600_000L
    }
}
