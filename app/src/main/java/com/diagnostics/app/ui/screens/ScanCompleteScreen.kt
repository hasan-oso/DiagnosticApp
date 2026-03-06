package com.diagnostics.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diagnostics.app.data.models.DeviceScanData
import com.diagnostics.app.utils.scoring.ScoreCalculator
import androidx.compose.ui.res.stringResource
import com.diagnostics.app.R

val AppPrimary = Color(0xFF6200EA)
val AppSecondary = Color(0xFF00E5FF)
val AppAccent = Color(0xFF00E676)
val AppBackground = Color(0xFF0A0A0F)
val CardBackground = Color(0xFF1E1E29)

@Composable
fun ScanCompleteScreen(
    deviceData: DeviceScanData,
    onCreateReportClick: () -> Unit,
    onNewScanClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Scaffold(
        containerColor = AppBackground,
        bottomBar = {
            BottomActions(
                isVisible = isVisible,
                onCreateReportClick = onCreateReportClick,
                onNewScanClick = onNewScanClick
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppBackground),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HeaderSection(deviceData, isVisible)
            }

            item {
                OverallScoreSection(deviceData, isVisible)
            }

            item {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(500, delayMillis = 300)) + slideInVertically(
                        initialOffsetY = { 50 }, animationSpec = tween(500, delayMillis = 300)
                    )
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        BatteryCard(deviceData)
                        PerformanceCard(deviceData)
                        StorageCard(deviceData)
                        DeviceCard(deviceData)
                        SecurityCard(deviceData)
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderSection(data: DeviceScanData, isVisible: Boolean) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(500)) + slideInVertically(initialOffsetY = { -50 })
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "NeuralCheck | نيورال تشيك",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${data.deviceBrand} ${data.deviceModel}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Transparent,
                style = androidx.compose.ui.text.TextStyle(
                    brush = Brush.horizontalGradient(
                        colors = listOf(AppPrimary, AppSecondary)
                    )
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Scan ID: ${data.scanId.take(8).uppercase()}",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun OverallScoreSection(data: DeviceScanData, isVisible: Boolean) {
    // حساب تقريبي للنتيجة إذا لم تكن موجودة. يمكنك تعديل الحسابات أو أخذها من ViewModel.
    val score = remember { calculateOverallScore(data).coerceIn(0f, 100f) }
    
    val animatedScore by animateFloatAsState(
        targetValue = if (isVisible) score else 0f,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "scoreAnimation"
    )

    val scoreColor = when {
        animatedScore >= 80 -> AppAccent
        animatedScore >= 50 -> Color(0xFFFFC107) // Yellow
        else -> Color(0xFFFF1744) // Red
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(180.dp)) {
            val strokeWidth = 12.dp.toPx()
            
            // Background Circle
            drawArc(
                color = CardBackground,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size(size.width, size.height)
            )

            // Progress Circle
            drawArc(
                color = scoreColor,
                startAngle = 135f,
                sweepAngle = 270f * (animatedScore / 100f),
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size(size.width, size.height)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = animatedScore.toInt().toString(),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = stringResource(R.string.overall_score_label),
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

// Cards
@Composable
fun InfoCard(title: String, icon: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = icon, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title, 
                    fontSize = 16.sp, 
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun CardRow(label: String, value: String, valueColor: Color = Color.White) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 14.sp)
        Text(text = value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun BatteryCard(data: DeviceScanData) {
    val health = data.batteryStressResult.estimatedHealthPercent
    val degradation = data.batteryStressResult.capacityMetrics?.degradationPercent ?: 0f

    InfoCard(title = stringResource(R.string.battery_label), icon = "🔋") {
        CardRow("Health", if (health >= 0) "$health%" else "N/A", if (health > 80) AppAccent else Color.Yellow)
        CardRow("Design Capacity", "${data.batteryDesignCapacityMah} mAh")
        CardRow("Degradation", String.format("%.1f%%", degradation))
    }
}

@Composable
fun PerformanceCard(data: DeviceScanData) {
    val multiCore = data.cpuStressResult.multiCoreScore
    val temp = data.cpuStressResult.maxTempCelsius
    val throttling = data.cpuStressResult.performanceDegradationPercent

    InfoCard(title = stringResource(R.string.performance_label), icon = "⚡") {
        CardRow("Multi-Core Score", "$multiCore")
        CardRow("Peak Temperature", "${temp}°C", if (temp > 45) Color.Red else Color.White)
        CardRow("Thermal Throttling", String.format("%.1f%%", throttling))
    }
}

@Composable
fun StorageCard(data: DeviceScanData) {
    val type = data.storageStressResult.detectedType
    val read = data.storageStressResult.sequentialReadMbps
    val write = data.storageStressResult.sequentialWriteMbps

    InfoCard(title = stringResource(R.string.storage_label), icon = "💾") {
        CardRow("Storage Type", type)
        CardRow("Read Speed", String.format("%.0f MB/s", read))
        CardRow("Write Speed", String.format("%.0f MB/s", write))
    }
}

@Composable
fun DeviceCard(data: DeviceScanData) {
    val ramGb = data.ramTotalMb / 1024
    
    InfoCard(title = stringResource(R.string.device_label), icon = "📱") {
        CardRow("Age (Years)", String.format("%.1f", data.deviceAgeYears))
        CardRow("Android Version", data.androidVersion)
        CardRow("RAM", "$ramGb GB")
        CardRow("Sensors", "${data.sensorCountTotal} available")
        CardRow("Cameras", "${data.cameraCount}")
    }
}

@Composable
fun SecurityCard(data: DeviceScanData) {
    val isRooted = data.safetyNetBasicIntegrity.not() || data.unknownSourcesCount > 10 // Simplified logic
    val rootText = if (isRooted) "Warning" else "Secure"
    val rootColor = if (isRooted) Color.Red else AppAccent

    InfoCard(title = stringResource(R.string.security_label), icon = "🔒") {
        CardRow("System Status", rootText, rootColor)
        CardRow("Play Integrity", data.playIntegrityVerdict)
        CardRow("Play Services", if (data.playServicesAvailable) "Available" else "Missing")
        CardRow("Connectivity", if (data.nfcAvailable) "NFC Supported" else "No NFC")
    }
}

@Composable
fun BottomActions(
    isVisible: Boolean,
    onCreateReportClick: () -> Unit,
    onNewScanClick: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { 100 }) + fadeIn()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppBackground.copy(alpha = 0.95f))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onCreateReportClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(listOf(AppPrimary, AppSecondary)),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.create_pdf_report),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onNewScanClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.new_scan), fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

fun calculateOverallScore(data: DeviceScanData): Float {
    return ScoreCalculator().calculateOverallScore(data).overall.toFloat()
}
