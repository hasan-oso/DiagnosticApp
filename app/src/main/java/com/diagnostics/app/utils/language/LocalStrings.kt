package com.diagnostics.app.utils.language

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.LayoutDirection

/** CompositionLocal للـ Strings — يتيح لأي شاشة الوصول بدون تمرير parameter */
val LocalStrings = compositionLocalOf<Strings> { AppStrings.arabic }

/** CompositionLocal لاتجاه اللغة */
val LocalIsRtl = compositionLocalOf { true }

/** CompositionLocal لاتجاه التخطيط */
val LocalAppLayoutDirection = compositionLocalOf { LayoutDirection.Rtl }
