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
 * Institution: OST?M Technical University
 */
package com.diagnostics.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diagnostics.app.R
import com.diagnostics.app.data.models.DeviceScanData
import com.diagnostics.app.viewmodel.ScanViewModel
import com.diagnostics.app.viewmodel.ScanState
import com.diagnostics.app.data.repository.DataCollectionRepository
import kotlinx.coroutines.launch

import android.view.WindowManager
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import android.app.Activity

@Composable
fun ScanningScreen(
    onScanComplete: (DeviceScanData) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? Activity
    
    // Keep screen on during scan
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val viewModel: ScanViewModel = viewModel()
    val scanState by viewModel.scanState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.startScan()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (val state = scanState) {
            is ScanState.Idle -> {
                Text(stringResource(R.string.initializing))
            }
            is ScanState.PreflightCheck -> {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }
            is ScanState.Collecting -> {
                Text(
                    text = stringResource(R.string.collecting_data),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = state.progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("${(state.progress * 100).toInt()}%")
            }
            is ScanState.StressTesting -> {
                Text(
                    text = stringResource(R.string.stress_testing),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                TestProgress(
                    label = stringResource(R.string.battery_test, (state.batteryProgress * 100).toInt()),
                    progress = state.batteryProgress
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                TestProgress(
                    label = stringResource(R.string.cpu_test, (state.cpuProgress * 100).toInt()),
                    progress = state.cpuProgress
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                TestProgress(
                    label = stringResource(R.string.storage_test, (state.storageProgress * 100).toInt()),
                    progress = state.storageProgress
                )
            }
            is ScanState.Uploading -> {
                Text(
                    text = "Uploading to cloud...",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = state.progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                )
            }
            is ScanState.Complete -> {
                Text(
                    text = stringResource(R.string.complete),
                    style = MaterialTheme.typography.headlineMedium
                )
                LaunchedEffect(Unit) {
                    // Phase 1: Auto upload immediately
                    val repository = DataCollectionRepository(context)
                    val success = repository.uploadScanData(state.data)
                    
                    if (success) {
                       android.util.Log.d("ScanningScreen", "✅ Phase 1: Auto data uploaded")
                    } else {
                       android.util.Log.e("ScanningScreen", "❌ Phase 1 upload failed")
                    }
                    
                    // Proceed to Labeling (Phase 2)
                    onScanComplete(state.data)
                }
            }
            is ScanState.Error -> {
                Text(
                    text = "Error: ${state.message}",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun TestProgress(label: String, progress: Float) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label)
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        )
    }
}
