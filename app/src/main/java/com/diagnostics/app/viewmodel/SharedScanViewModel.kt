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
import com.diagnostics.app.data.models.DeviceScanData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * SharedScanViewModel — Holds scan data across navigation steps.
 * Shared between ScanningScreen → LabelingScreen → ReportScreen
 * so that DeviceScanData is available for the report.
 */
class SharedScanViewModel(application: Application) : AndroidViewModel(application) {

    private val _scanData = MutableStateFlow<DeviceScanData?>(null)
    val scanData: StateFlow<DeviceScanData?> = _scanData.asStateFlow()

    /**
     * Store scan data after scan completion.
     * Called from ScanningScreen when scan finishes.
     */
    fun setScanData(data: DeviceScanData) {
        _scanData.value = data
    }

    /**
     * Clear scan data (e.g. when returning to home).
     */
    fun clearScanData() {
        _scanData.value = null
    }
}
