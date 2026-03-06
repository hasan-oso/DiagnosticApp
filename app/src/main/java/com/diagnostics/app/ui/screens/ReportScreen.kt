package com.diagnostics.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diagnostics.app.R
import com.diagnostics.app.data.models.DeviceScanData
import com.diagnostics.app.data.models.FreqReading
import com.diagnostics.app.data.models.TempReading
import com.diagnostics.app.data.models.VoltageReading
import com.diagnostics.app.data.models.auth.ShopInfo
import com.diagnostics.app.utils.language.LocalStrings
import com.diagnostics.app.viewmodel.InventoryViewModel
import kotlinx.coroutines.launch

val NeuralPrimary = Color(0xFF6200EA)
val NeuralSecondary = Color(0xFF00E5FF)
val NeuralAccent = Color(0xFF00E676)
val NeuralBackground = Color(0xFF0A0A0F)
val NeuralCard = Color(0xFF1A1A2E)
val NeuralCardBorder = Color(0xFF2D2D44)
val ChartGridColor = Color(0xFF2A2A35)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    scanData: DeviceScanData,
    shopInfo: ShopInfo?,
    onBackClick: () -> Unit,
    shopId: String? = null,
    inventoryViewModel: InventoryViewModel = viewModel()
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val coroutineScope = rememberCoroutineScope()
    val batteryScore = scanData.batteryStressResult.estimatedHealthPercent.toFloat()
    val perfScore = (100f - scanData.cpuStressResult.performanceDegradationPercent).coerceIn(0f, 100f)
    val storageScore = if (scanData.storageStressResult.sequentialReadMbps > 200f) 90f else 60f
    val ageScore = (100f - (scanData.deviceAgeYears * 10f)).coerceIn(0f, 100f)
    val overallScore = (batteryScore * 0.35f + perfScore * 0.25f + storageScore * 0.20f + ageScore * 0.20f)

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    var showPdfDialog by remember { mutableStateOf(false) }
    var savedPdfPath by remember { mutableStateOf("") }
    // حالة سؤال الشراء والـ BottomSheet
    var showPurchaseSheet by remember { mutableStateOf(false) }

    if (showPdfDialog) {
        PdfSuccessDialog(
            filePath = savedPdfPath,
            deviceName = "${scanData.deviceBrand} ${scanData.deviceModel}",
            onDismiss = { showPdfDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.technical_report),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NeuralBackground
                )
            )
        },
        containerColor = NeuralBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ReportHeader(scanData, overallScore, isVisible)
            }

            // Security Warning Card
            val isRooted = !scanData.safetyNetBasicIntegrity
            val hasSecurityRisk = isRooted || scanData.developerOptionsEnabled || scanData.adbEnabled
            if (hasSecurityRisk) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFF1744).copy(alpha = 0.12f)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFFF1744)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFF1744),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    strings.securityWarning,
                                    color = Color(0xFFFF1744),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            if (isRooted) {
                                Text("⚠️ ${strings.deviceRooted}", color = Color(0xFFFF1744), fontSize = 13.sp)
                            }
                            if (scanData.developerOptionsEnabled) {
                                Text("⚠️ ${strings.developerOptionsOn}", color = Color(0xFFFFB300), fontSize = 13.sp)
                            }
                            if (scanData.adbEnabled) {
                                Text("⚠️ ${strings.adbWarning}", color = Color(0xFFFFB300), fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            item {
                BatterySection(scanData)
            }

            item {
                PerformanceSection(scanData)
            }

            item {
                StorageSection(scanData)
            }

            item {
                DeviceSection(scanData)
            }

            item {
                SecurityConnectivitySection(scanData)
            }

            // بطاقة سؤال الشراء — تظهر فقط إذا كان المستخدم مربوطاً بمحل (shopId)
            if (shopId != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NeuralCard),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, NeuralCardBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "🛍️ ${strings.didYouBuyDevice}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                strings.addToStockSubtitle,
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { showPurchaseSheet = true },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00E676).copy(alpha = 0.2f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("✅ ${strings.yes}", color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { /* لا تفعل شيئاً */ },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, NeuralCardBorder)
                                ) {
                                    Text("❌ ${strings.no}", color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                ExportButton(onExportClick = {
                    coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val generator = com.diagnostics.app.utils.pdf.PdfReportGenerator(context)
                            val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                            val resultPath = generator.generateReport(scanData, shopInfo, currentUid)
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                if (resultPath != null) {
                                    savedPdfPath = resultPath
                                    showPdfDialog = true
                                } else {
                                    android.widget.Toast.makeText(context, context.getString(R.string.pdf_save_failed), android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        } catch(e: Exception) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                android.widget.Toast.makeText(context, context.getString(R.string.error_with_message, e.localizedMessage), android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                })
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // BottomSheet سعر الشراء
    if (showPurchaseSheet) {
        PurchasePriceBottomSheet(
            deviceName = "${scanData.deviceBrand} ${scanData.deviceModel}",
            onConfirm = { price ->
                inventoryViewModel.saveDeviceAfterScan(
                    scanData = scanData,
                    purchasePrice = price,
                    score = overallScore.toInt(),
                    shopId = shopId!!
                )
                showPurchaseSheet = false
                android.widget.Toast.makeText(
                    context, strings.savedToInventoryToast, android.widget.Toast.LENGTH_SHORT
                ).show()
            },
            onDismiss = { showPurchaseSheet = false }
        )
    }
}

@Composable
fun ReportHeader(data: DeviceScanData, score: Float, isVisible: Boolean) {
    val animatedScore by animateFloatAsState(
        targetValue = if (isVisible) score else 0f,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "score"
    )

    val scoreColor = when {
        animatedScore >= 80 -> NeuralAccent
        animatedScore >= 50 -> Color(0xFFFFC107)
        else -> Color(0xFFFF1744)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NeuralCard, RoundedCornerShape(16.dp))
            .border(1.dp, NeuralCardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${data.deviceBrand} ${data.deviceModel}",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                style = androidx.compose.ui.text.TextStyle(
                    brush = Brush.horizontalGradient(listOf(NeuralPrimary, NeuralSecondary))
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "ID: ${data.scanId.take(8).uppercase()}", color = Color.Gray, fontSize = 12.sp)
            Text(text = "OS: Android ${data.androidVersion} (API ${data.androidSdk})", color = Color.LightGray, fontSize = 14.sp)
        }

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = NeuralCardBorder,
                    startAngle = 135f, sweepAngle = 270f, useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = scoreColor,
                    startAngle = 135f, sweepAngle = 270f * (animatedScore / 100f), useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = animatedScore.toInt().toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun BatterySection(data: DeviceScanData) {
    val strings = LocalStrings.current
    val battery = data.batteryStressResult
    ReportCard(stringResource(R.string.battery_section)) {
        DetailRow("Health Status", battery.batteryHealthStatus)
        DetailRow("Estimated Health", "${battery.estimatedHealthPercent}%")
        DetailRow("Design Capacity", "${data.batteryDesignCapacityMah} mAh")
        DetailRow("Degradation", String.format("%.1f%%", battery.capacityMetrics?.degradationPercent ?: 0f))
        DetailRow("Internal Resistance", "${battery.internalResistanceMohm} mΩ")
        DetailRow("Data Quality", battery.dataQuality?.overallQuality ?: "N/A")

        Spacer(modifier = Modifier.height(16.dp))
        Text(strings.voltageDropChart, color = Color.LightGray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
        VoltageChart(battery.voltageReadings)
    }
}

@Composable
fun PerformanceSection(data: DeviceScanData) {
    val strings = LocalStrings.current
    val cpu = data.cpuStressResult
    ReportCard(stringResource(R.string.performance_section)) {
        DetailRow("Multi-Core Score", "${cpu.multiCoreScore}")
        DetailRow("Single-Core Score", "${cpu.singleCoreScore}")
        DetailRow("Peak Temp", "${cpu.maxTempCelsius} °C", if(cpu.maxTempCelsius > 45) Color.Red else Color.White)
        DetailRow("Thermal Throttling", String.format("%.1f%%", cpu.performanceDegradationPercent))
        cpu.throttlingStartSec?.let { t ->
            DetailRow("Throttling Started At", "${t}s")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(strings.cpuTempChart, color = Color.LightGray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
        TemperatureChart(cpu.tempReadings)

        Spacer(modifier = Modifier.height(16.dp))
        Text(strings.cpuFreqChart, color = Color.LightGray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
        FrequencyChart(cpu.freqReadings, cpu.throttlingStartSec)
    }
}

@Composable
fun StorageSection(data: DeviceScanData) {
    val storage = data.storageStressResult
    ReportCard(stringResource(R.string.storage_section)) {
        DetailRow("Total Storage", "${data.storageTotalGb} GB")
        DetailRow("Available", "${data.storageAvailableGb} GB")
        DetailRow("Detected Type", storage.detectedType)
        DetailRow("Seq Read Speed", String.format("%.1f MB/s", storage.sequentialReadMbps))
        DetailRow("Seq Write Speed", String.format("%.1f MB/s", storage.sequentialWriteMbps))
        DetailRow("Random Read (4K)", "${storage.random4kReadIops} IOPS")
        DetailRow("Random Write (4K)", "${storage.random4kWriteIops} IOPS")
    }
}

@Composable
fun DeviceSection(data: DeviceScanData) {
    ReportCard(stringResource(R.string.hardware_section)) {
        DetailRow("Device Age", String.format("%.1f Years", data.deviceAgeYears))
        DetailRow("Android OS", "${data.androidVersion} (API ${data.androidSdk})")
        DetailRow("RAM", "${data.ramTotalMb / 1024} GB")
        DetailRow("Screen", "${data.screenWidthPx}x${data.screenHeightPx} @ ${data.screenRefreshRateHz}Hz")
        DetailRow("CPU Cores", "${data.cpuCoreCount} Cores (Max ${data.cpuMaxFreqMhz} MHz)")
        DetailRow("GPU", "${data.gpuVendor} - ${data.gpuRenderer}")
        DetailRow("Cameras", "${data.cameraCount} Total")
        DetailRow("Sensors", "${data.sensorCountTotal} Sensors")
    }
}

@Composable
fun SecurityConnectivitySection(data: DeviceScanData) {
    ReportCard(stringResource(R.string.security_connectivity)) {
        val rootStatus = if (data.safetyNetBasicIntegrity.not()) "Compromised/Rooted" else "Intact"
        DetailRow("Root/Integrity", rootStatus, if (data.safetyNetBasicIntegrity.not()) Color.Red else NeuralAccent)
        DetailRow("Play Integrity", data.playIntegrityVerdict)
        
        Divider(color = NeuralCardBorder, modifier = Modifier.padding(vertical = 8.dp))
        
        val nfcText = if (data.nfcAvailable) "Supported" else "Not Supported"
        DetailRow("NFC", nfcText)
        val wifiText = if (data.wifiAvailable) "Working" else "Failed"
        DetailRow("Wi-Fi Module", wifiText)
        DetailRow("Bluetooth", data.bluetoothVersion)
        
        Divider(color = NeuralCardBorder, modifier = Modifier.padding(vertical = 8.dp))
        
        val speaker = if(data.speakerWorking) "Working" else "Unknown"
        val mic = if(data.microphoneWorking) "Working" else "Unknown"
        DetailRow("Speaker", speaker)
        DetailRow("Microphone", mic)
        DetailRow("Audio Test Result", if(data.audioTestPassed) "Pass" else "N/A")
    }
}

@Composable
fun ExportButton(onExportClick: () -> Unit) {
    Button(
        onClick = onExportClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(listOf(NeuralPrimary, NeuralSecondary)), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.export_pdf_report), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ======================== CHARTS ========================

@Composable
fun VoltageChart(readings: List<VoltageReading>) {
    val strings = LocalStrings.current
    if (readings.isEmpty()) {
        Text(strings.noVoltageReadings, color = Color.Gray, fontSize = 12.sp)
        return
    }

    val maxVolt = readings.maxOfOrNull { it.voltageMv } ?: 4500
    val minVolt = readings.minOfOrNull { it.voltageMv } ?: 3000
    val avgVolt = readings.map { it.voltageMv }.average().toFloat()

    Box(modifier = Modifier
        .fillMaxWidth()
        .height(150.dp)
        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
        .padding(16.dp)) {
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val voltRange = maxVolt - minVolt
            val safeRange = if (voltRange == 0) 1 else voltRange
            val path = Path()

            if (readings.size > 1) {
                readings.forEachIndexed { i, r ->
                    val x = (i.toFloat() / (readings.size - 1)) * width
                    val y = height - (((r.voltageMv - minVolt).toFloat() / safeRange) * height)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(path, color = NeuralSecondary, style = Stroke(width = 2.dp.toPx()))
                
                // Draw Avg line
                val avgY = height - (((avgVolt - minVolt) / safeRange) * height)
                drawLine(
                    color = NeuralAccent.copy(alpha = 0.5f),
                    start = Offset(0f, avgY),
                    end = Offset(width, avgY),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
        
        // Labels
        Text("$maxVolt mV", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.align(Alignment.TopStart))
        Text("$minVolt mV", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomStart))
    }
}

@Composable
fun TemperatureChart(readings: List<TempReading>) {
    val strings = LocalStrings.current
    if (readings.isEmpty()) {
        Text(strings.noTempReadings, color = Color.Gray, fontSize = 12.sp)
        return
    }

    val maxTemp = readings.maxOfOrNull { it.tempCelsius } ?: 50f
    val minTemp = readings.minOfOrNull { it.tempCelsius } ?: 20f

    Box(modifier = Modifier
        .fillMaxWidth()
        .height(150.dp)
        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
        .padding(16.dp)) {
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val tempRange = maxTemp - minTemp
            val safeRange = if (tempRange == 0f) 1f else tempRange

            if (readings.size > 1) {
                for (i in 0 until readings.size - 1) {
                    val p1 = readings[i]
                    val p2 = readings[i + 1]
                    
                    val x1 = (i.toFloat() / (readings.size - 1)) * width
                    val y1 = height - (((p1.tempCelsius - minTemp) / safeRange) * height)
                    
                    val x2 = ((i + 1).toFloat() / (readings.size - 1)) * width
                    val y2 = height - (((p2.tempCelsius - minTemp) / safeRange) * height)

                    val segColor = when {
                        p2.tempCelsius > 45f -> Color.Red
                        p2.tempCelsius > 38f -> Color(0xFFFFC107)
                        else -> NeuralAccent
                    }

                    drawLine(
                        color = segColor,
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        
        Text("${maxTemp.toInt()}°C", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.align(Alignment.TopStart))
        Text("${minTemp.toInt()}°C", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomStart))
    }
}

@Composable
fun FrequencyChart(readings: List<FreqReading>, throttlingStartSec: Int?) {
    val strings = LocalStrings.current
    if (readings.isEmpty()) {
        Text(strings.noFreqReadings, color = Color.Gray, fontSize = 12.sp)
        return
    }

    val maxFreq = readings.maxOfOrNull { it.freqMhz } ?: 3000

    Box(modifier = Modifier
        .fillMaxWidth()
        .height(150.dp)
        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
        .padding(16.dp)) {
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            val barWidth = (width / readings.size) * 0.8f
            val maxTime = readings.last().timestampSec

            readings.forEachIndexed { i, r ->
                val x = (i.toFloat() / (readings.size - 1)) * width
                val barHeight = (r.freqMhz.toFloat() / maxFreq) * height
                val y = height - barHeight
                
                val c = if (r.timestampSec >= (throttlingStartSec ?: Int.MAX_VALUE)) Color(0xFFFFC107) else NeuralPrimary
                
                drawLine(
                    color = c,
                    start = Offset(x, height),
                    end = Offset(x, y),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Square
                )
            }
            
            // Draw throttling vertical line
            throttlingStartSec?.let { t ->
                if(readings.size > 1) {
                    val xPos = (t.toFloat() / maxTime) * width
                    drawLine(
                        color = Color.Red,
                        start = Offset(xPos, 0f),
                        end = Offset(xPos, height),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }
            }
        }
        Text("$maxFreq MHz", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.align(Alignment.TopStart))
    }
}

// ======================== HELPER COMPONENTS ========================

@Composable
fun ReportCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NeuralCard),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NeuralCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NeuralSecondary)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, valueColor: Color = Color.White) {
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
