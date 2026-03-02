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
package com.diagnostics.app.utils.language

object ReportStrings {
    val AR = ArabicStrings()
    val TR = TurkishStrings()
    val EN = EnglishStrings()

    fun getLanguage(language: Language): ReportLanguage = when (language) {
        Language.ARABIC -> AR
        Language.TURKISH -> TR
        Language.ENGLISH -> EN
    }
}
