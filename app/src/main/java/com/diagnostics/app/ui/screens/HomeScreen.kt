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
import com.diagnostics.app.R
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import com.diagnostics.app.data.repository.DataCollectionRepository

@Composable
fun HomeScreen(
    currentLanguage: String,
    onLanguageChange: (String) -> Unit,
    onStartScan: () -> Unit
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val repository = remember { DataCollectionRepository(context) }
    val pendingCount = remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    // Update pending count on start
    LaunchedEffect(Unit) {
        pendingCount.value = repository.getPendingCount()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Pending scans indicator
        if (pendingCount.value > 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.pending_uploads),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.pending_count, pendingCount.value),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    android.widget.Toast.makeText(context, context.getString(R.string.uploading), android.widget.Toast.LENGTH_SHORT).show()
                                    repository.syncPendingScans()
                                    // Refresh count
                                    kotlinx.coroutines.delay(2000) 
                                    pendingCount.value = repository.getPendingCount()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.upload_now))
                        }
                    }
                }
            }
        }
        
        Button(
            onClick = onStartScan,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(stringResource(R.string.start_scan))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = { showLanguageDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.language))
        }
        
    }
    
    if (showLanguageDialog) {
        val scope = rememberCoroutineScope()
        
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.language)) },
            text = {
                Column {
                    LanguageOption("العربية", "ar", currentLanguage) {
                        scope.launch { onLanguageChange(it) }
                    }
                    LanguageOption("Türkçe", "tr", currentLanguage) {
                        scope.launch { onLanguageChange(it) }
                    }
                    LanguageOption("English", "en", currentLanguage) {
                        scope.launch { onLanguageChange(it) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
    
}

@Composable
fun LanguageOption(
    label: String,
    code: String,
    currentLanguage: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = currentLanguage == code,
            onClick = { onSelect(code) }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label)
    }
}
