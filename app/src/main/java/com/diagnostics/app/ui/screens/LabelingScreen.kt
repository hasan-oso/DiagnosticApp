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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diagnostics.app.R
import com.diagnostics.app.viewmodel.LabelViewModel

/**
 * Helper functions to map internal values to translated strings
 */
@Composable
private fun getAuthenticityLabel(value: String): String {
    return when(value) {
        "GENUINE" -> stringResource(R.string.auth_genuine)
        "SUSPICIOUS" -> stringResource(R.string.auth_suspicious)
        "FAKE" -> stringResource(R.string.auth_fake)
        else -> value
    }
}

@Composable
private fun getRefurbishmentLabel(value: String): String {
    return when(value) {
        "ORIGINAL" -> stringResource(R.string.refurb_original)
        "MINOR_REPAIR" -> stringResource(R.string.refurb_minor)
        "REFURBISHED" -> stringResource(R.string.refurb_refurbished)
        else -> value
    }
}

@Composable
private fun getUsageLabel(value: String): String {
    return when(value) {
        "LIGHT" -> stringResource(R.string.usage_light)
        "MODERATE" -> stringResource(R.string.usage_moderate)
        "HEAVY" -> stringResource(R.string.usage_heavy)
        else -> value
    }
}

@Composable
private fun getComponentLabel(value: String): String {
    return when(value) {
        "BATTERY" -> stringResource(R.string.component_battery)
        "SCREEN" -> stringResource(R.string.component_screen)
        "CAMERA" -> stringResource(R.string.component_camera)
        else -> value
    }
}

@Composable
fun LabelingScreen(
    scanId: String,
    onComplete: () -> Unit,
    viewModel: LabelViewModel = viewModel()
) {
    var overallHealth by remember { mutableStateOf(50) }
    var authenticity by remember { mutableStateOf("GENUINE") }
    var refurbishment by remember { mutableStateOf("ORIGINAL") }
    var usageIntensity by remember { mutableStateOf("MODERATE") }
    val replacedComponents = remember { mutableStateListOf<String>() }
    val knownIssues = remember { mutableStateListOf<String>() }
    var technicianNotes by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    
    val submitState by viewModel.submitState.collectAsState()
    
    LaunchedEffect(submitState) {
        if (submitState is com.diagnostics.app.viewmodel.SubmitState.Success) {
            onComplete()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.overall_health),
            style = MaterialTheme.typography.titleMedium
        )
        Slider(
            value = overallHealth.toFloat(),
            onValueChange = { overallHealth = it.toInt() },
            valueRange = 0f..100f,
            modifier = Modifier.fillMaxWidth()
        )
        Text("$overallHealth/100")
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(stringResource(R.string.authenticity))
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("GENUINE", "SUSPICIOUS", "FAKE").forEach { option ->
                RadioOption(
                    option = getAuthenticityLabel(option),
                    selected = authenticity == option,
                    onClick = { authenticity = option }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(stringResource(R.string.refurbishment))
        Column {
            listOf("ORIGINAL", "MINOR_REPAIR", "REFURBISHED").forEach { option ->
                RadioOption(
                    option = getRefurbishmentLabel(option),
                    selected = refurbishment == option,
                    onClick = { refurbishment = option }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(stringResource(R.string.usage_intensity))
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("LIGHT", "MODERATE", "HEAVY").forEach { option ->
                RadioOption(
                    option = getUsageLabel(option),
                    selected = usageIntensity == option,
                    onClick = { usageIntensity = option }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // ...
        
        Text(stringResource(R.string.replaced_components))
        listOf("BATTERY", "SCREEN", "CAMERA").forEach { component ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = replacedComponents.contains(component),
                    onCheckedChange = {
                        if (it) {
                            replacedComponents.add(component)
                        } else {
                            replacedComponents.remove(component)
                        }
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(getComponentLabel(component))
            }
        }

        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = technicianNotes,
            onValueChange = { technicianNotes = it },
            label = { Text(stringResource(R.string.technician_notes)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                isSubmitting = true
                viewModel.submitLabels(
                    scanId = scanId,
                    overallHealth = overallHealth,
                    authenticity = authenticity,
                    refurbishment = refurbishment,
                    usageIntensity = usageIntensity,
                    components = replacedComponents.toList(),
                    knownIssues = knownIssues.toList(),
                    technicianNotes = technicianNotes
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSubmitting
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(stringResource(R.string.submit))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Skip Button (Phase 2 Optional)
        OutlinedButton(
            onClick = {
                // Just navigate away, data already uploaded in Phase 1
                onComplete()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSubmitting
        ) {
             Text("تخطي")
        }
        
        if (submitState is com.diagnostics.app.viewmodel.SubmitState.Error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.error_upload),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun RadioOption(option: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 16.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(option, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
fun ComponentCheckbox(component: String, selected: MutableList<String>) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Checkbox(
            checked = selected.contains(component),
            onCheckedChange = {
                if (it) selected.add(component) else selected.remove(component)
            }
        )
        Text(
            stringResource(when(component) {
                "BATTERY" -> R.string.battery
                "SCREEN" -> R.string.screen
                "CAMERA" -> R.string.camera
                else -> R.string.app_name
            })
        )
    }
}
