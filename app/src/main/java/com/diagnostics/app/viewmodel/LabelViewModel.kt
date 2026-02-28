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
package com.diagnostics.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diagnostics.app.data.models.HumanLabels
import com.diagnostics.app.data.repository.DataCollectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SubmitState {
    object Idle : SubmitState()
    object Submitting : SubmitState()
    object Success : SubmitState()
    data class Error(val message: String) : SubmitState()
}

class LabelViewModel(application: Application) : AndroidViewModel(application) {
    private val _submitState = MutableStateFlow<SubmitState>(SubmitState.Idle)
    val submitState: StateFlow<SubmitState> = _submitState.asStateFlow()
    
    private val dataCollectionRepo = DataCollectionRepository(getApplication<Application>())
    
    private val json = kotlinx.serialization.json.Json { encodeDefaults = true }
    
    fun submitLabels(
        scanId: String,
        overallHealth: Int,
        authenticity: String,
        refurbishment: String,
        usageIntensity: String,
        components: List<String>,
        knownIssues: List<String>,
        technicianNotes: String
    ) {
        viewModelScope.launch {
            try {
                _submitState.value = SubmitState.Submitting
                
                val labels = HumanLabels(
                    overallHealthScore = overallHealth,
                    authenticity = authenticity,
                    refurbishmentStatus = refurbishment,
                    replacedComponents = components,
                    knownIssues = knownIssues,
                    ageCategory = "auto-calculated",
                    usageIntensity = usageIntensity,
                    technicianNotes = technicianNotes,
                    labeledBy = "technician-001", 
                    labeledAt = System.currentTimeMillis()
                )
                
                try {
                    // Serialize labels to JSON
                    val labelsJson = json.encodeToString(HumanLabels.serializer(), labels)
                    
                    val result = dataCollectionRepo.updateScanWithLabels(scanId, labelsJson)
                    if (result) {
                        _submitState.value = SubmitState.Success
                    } else {
                        throw Exception("Failed to update labels on Drive")
                    }
                } catch (e: Exception) {
                    // Schedule background work
                     val jsonData = json.encodeToString(HumanLabels.serializer(), labels)
                     
                     val uploadWork = androidx.work.OneTimeWorkRequestBuilder<com.diagnostics.app.workers.UploadWorker>()
                        .setInputData(
                            androidx.work.Data.Builder()
                                .putString("type", "LABELS")
                                .putString("data", jsonData)
                                .putString("scanId", scanId)
                                .build()
                        )
                        .setConstraints(
                            androidx.work.Constraints.Builder()
                                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                                .build()
                        )
                        .setBackoffCriteria(
                            androidx.work.BackoffPolicy.EXPONENTIAL,
                            30, java.util.concurrent.TimeUnit.SECONDS
                        )
                        .build()
                    
                    androidx.work.WorkManager.getInstance(getApplication()).enqueue(uploadWork)
                    
                    // Consider it success from UI perspective (queued)
                    _submitState.value = SubmitState.Success 
                }
                
            } catch (e: Exception) {
                _submitState.value = SubmitState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
