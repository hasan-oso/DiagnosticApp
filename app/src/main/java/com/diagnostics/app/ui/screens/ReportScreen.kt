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
package com.diagnostics.app.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.diagnostics.app.data.models.DeviceScanData
import com.diagnostics.app.data.models.auth.ShopInfo
import com.diagnostics.app.utils.language.Language
import com.diagnostics.app.utils.pdf.PdfReportGenerator
import com.diagnostics.app.utils.scoring.ScoreCalculator
import com.diagnostics.app.utils.pricing.PriceCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ReportScreen — Displays device health report with score, price,
 * and option to generate & share PDF.
 */
@Composable
fun ReportScreen(
    scanData: DeviceScanData,
    shopInfo: ShopInfo? = null,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scoreCalculator = remember { ScoreCalculator() }
    val priceCalculator = remember { PriceCalculator() }
    val score = remember { scoreCalculator.calculateOverallScore(scanData) }
    val priceRange = remember { priceCalculator.calculateSuggestedPrice(scanData) }
    var isGeneratingPdf by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text(
            text = "NeuralCheck Report",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ══════ Overall Score Card ══════
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${score.overall.toInt()}",
                    fontSize = 60.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "/ 100",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Grade: ${score.grade}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                // Stars
                val stars = "★".repeat((score.overall / 20).toInt()) +
                    "☆".repeat(5 - (score.overall / 20).toInt())
                Text(text = stars, fontSize = 24.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ══════ Device Info ══════
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Device", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow("Model", scanData.deviceModel)
                InfoRow("Brand", scanData.deviceManufacturer)
                InfoRow("Age", String.format("%.1f years", scanData.deviceAgeYears))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ══════ Detailed Scores ══════
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Scores", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                ScoreBar("Battery", score.battery)
                ScoreBar("Performance", score.performance)
                ScoreBar("Storage", score.storage)
                ScoreBar("Age", score.age)
                ScoreBar("Features", score.features)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ══════ Price Suggestion ══════
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Suggested Price",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${priceRange.min} - ${priceRange.max} TL",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = priceRange.condition,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ══════ Strengths & Issues ══════
        if (score.strengths.isNotEmpty() || score.issues.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (score.strengths.isNotEmpty()) {
                        Text("✅ Strengths", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        score.strengths.forEach { s ->
                            Text("  • $s", fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (score.issues.isNotEmpty()) {
                        Text("⚠️ Issues", fontWeight = FontWeight.Bold, fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error)
                        score.issues.forEach { issue ->
                            Text("  • $issue", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ══════ Generate PDF Button ══════
        Button(
            onClick = {
                isGeneratingPdf = true
                scope.launch {
                    try {
                        val pdfFile = withContext(Dispatchers.IO) {
                            val generator = PdfReportGenerator(context, Language.ARABIC)
                            generator.generateReport(scanData, shopInfo)
                        }

                        // Share the PDF
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            pdfFile
                        )
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(
                            Intent.createChooser(shareIntent, "Share Report / مشاركة التقرير")
                        )
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        isGeneratingPdf = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = !isGeneratingPdf
        ) {
            if (isGeneratingPdf) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generating...")
            } else {
                Text("📄 Generate & Share PDF", fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Back button
        OutlinedButton(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Home")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ══════════════════════════════════════════════
// Helper Composables
// ══════════════════════════════════════════════

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ScoreBar(label: String, score: Double) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 13.sp)
            Text("${score.toInt()}/100", fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = (score / 100.0).toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
