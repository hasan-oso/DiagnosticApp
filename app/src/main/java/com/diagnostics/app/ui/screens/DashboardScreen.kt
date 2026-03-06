package com.diagnostics.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diagnostics.app.R
import com.diagnostics.app.data.models.auth.DeviceStatus
import com.diagnostics.app.data.models.auth.InventoryDevice
import com.diagnostics.app.data.models.auth.ShopStats
import com.diagnostics.app.utils.language.LocalStrings
import com.diagnostics.app.viewmodel.AuthViewModel
import com.diagnostics.app.viewmodel.InventoryViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    shopId: String,
    shopName: String,
    shopInfo: com.diagnostics.app.data.models.auth.ShopInfo? = null,
    onGeneratePinClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onDeviceClick: (InventoryDevice) -> Unit,
    onScanClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    inventoryViewModel: InventoryViewModel = viewModel()
) {
    val devices by inventoryViewModel.devices.collectAsState()
    val stats by inventoryViewModel.stats.collectAsState()
    val pdfState by inventoryViewModel.pdfState.collectAsState()
    val context = LocalContext.current
    val strings = LocalStrings.current
    var selectedTab by remember { mutableIntStateOf(0) }

    var showPdfDialog by remember { mutableStateOf(false) }
    var savedPdfPath by remember { mutableStateOf("") }
    var downloadingDevice by remember { mutableStateOf<InventoryDevice?>(null) }

    // حالة dialog البيع
    var showSellDialog by remember { mutableStateOf(false) }
    var selectedDevice by remember { mutableStateOf<InventoryDevice?>(null) }
    
    LaunchedEffect(shopId) {
        inventoryViewModel.loadDashboard(shopId)
    }

    when (val state = pdfState) {
        is InventoryViewModel.PdfState.Success -> {
            LaunchedEffect(state) {
                savedPdfPath = state.filePath
                showPdfDialog = true
                inventoryViewModel.resetPdfState()
            }
        }
        is InventoryViewModel.PdfState.Error -> {
            LaunchedEffect(state) {
                Toast.makeText(context, context.getString(R.string.error_toast, state.message), Toast.LENGTH_LONG).show()
                inventoryViewModel.resetPdfState()
            }
        }
        else -> {}
    }

    if (showPdfDialog && downloadingDevice != null) {
        PdfSuccessDialog(
            filePath = savedPdfPath,
            deviceName = "${downloadingDevice!!.deviceBrand} ${downloadingDevice!!.deviceModel}",
            onDismiss = { showPdfDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🏪 $shopName", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onGeneratePinClick) {
                        Icon(
                            Icons.Default.QrCode2,
                            contentDescription = "Generate QR",
                            tint = Color(0xFF00E5FF)
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.LightGray)
                    }
                    IconButton(onClick = onLogoutClick) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color(0xFFFF1744))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0A0F))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onScanClick,
                containerColor = Color(0xFF00E5FF),
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Scan")
            }
        },
        containerColor = Color(0xFF0A0A0F)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(modifier = Modifier.weight(1f), title = stringResource(R.string.total), value = stats.totalDevices.toString(), color = Color(0xFF6200EA))
                StatCard(modifier = Modifier.weight(1f), title = stringResource(R.string.in_stock), value = stats.inStock.toString(), color = Color(0xFF00E5FF))
                StatCard(modifier = Modifier.weight(1f), title = stringResource(R.string.sold), value = stats.sold.toString(), color = Color(0xFF00E676))
            }

            // Profit Summary
            ProfitCard(stats)
            
            // QR Button — لربط جهاز العميل
            Button(
                onClick = onGeneratePinClick,
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
                            Brush.horizontalGradient(listOf(Color(0xFF6200EA), Color(0xFF00E5FF))),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = strings.generateQrForCustomer,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color(0xFF00E5FF),
                divider = { Divider(color = Color(0xFF2D2D44)) },
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFF00E5FF)
                    )
                }
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text(stringResource(R.string.all_tab), modifier = Modifier.padding(vertical = 12.dp), color = if (selectedTab == 0) Color.White else Color.Gray)
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text(stringResource(R.string.stock_tab), modifier = Modifier.padding(vertical = 12.dp), color = if (selectedTab == 1) Color.White else Color.Gray)
                }
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                    Text(stringResource(R.string.sold_tab), modifier = Modifier.padding(vertical = 12.dp), color = if (selectedTab == 2) Color.White else Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Devices List
            val filteredList = devices.filter {
                when (selectedTab) {
                    1 -> it.status == DeviceStatus.IN_STOCK
                    2 -> it.status == DeviceStatus.SOLD
                    else -> true
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredList) { device ->
                    DeviceCard(
                        device = device,
                        strings = strings,
                        onSellClick = {
                            selectedDevice = device
                            showSellDialog = true
                        },
                        onReturnClick = { inventoryViewModel.markAsInStock(device) },
                        onDownloadClick = {
                            downloadingDevice = device
                            inventoryViewModel.downloadReport(context, device, shopInfo)
                        },
                        isDownloading = pdfState is InventoryViewModel.PdfState.Loading
                    )
                }
            }
        }
    }

    // Dialog البيع
    if (showSellDialog && selectedDevice != null) {
        SellDeviceDialog(
            device = selectedDevice!!,
            onConfirm = { soldPrice, note ->
                inventoryViewModel.markAsSold(selectedDevice!!, soldPrice, note)
                showSellDialog = false
                selectedDevice = null
            },
            onDismiss = {
                showSellDialog = false
                selectedDevice = null
            }
        )
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, title: String, value: String, color: Color) {
    Column(
        modifier = modifier
            .background(Color(0xFF1A1A2E), RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(title, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun ProfitCard(stats: ShopStats) {
    val NumberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A2E), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF2D2D44), RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Text(stringResource(R.string.total_revenue), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.purchase), color = Color.Gray, fontSize = 14.sp)
            Text("${NumberFormat.format(stats.totalPurchaseCost)} TL", color = Color.LightGray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.sold_label), color = Color.Gray, fontSize = 14.sp)
            Text("${NumberFormat.format(stats.totalSoldRevenue)} TL", color = Color.LightGray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Divider(color = Color(0xFF2D2D44), modifier = Modifier.padding(vertical = 12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.profit), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            val profitColor = if (stats.netProfit >= 0) Color(0xFF00E676) else Color(0xFFFF1744)
            Text("${if (stats.netProfit >= 0) "✅ +" else "❌"}${NumberFormat.format(stats.netProfit)} TL", color = profitColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DeviceCard(
    device: InventoryDevice,
    strings: com.diagnostics.app.utils.language.Strings = LocalStrings.current,
    context: android.content.Context = LocalContext.current,
    onSellClick: () -> Unit = {},
    onReturnClick: () -> Unit = {},
    onDownloadClick: (InventoryDevice) -> Unit = {},
    isDownloading: Boolean = false
) {
    val nf = NumberFormat.getNumberInstance(Locale.getDefault())
    val profit = device.profit

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF2D2D44))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Header: اسم الجهاز + الحالة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "📱 ${device.deviceBrand} ${device.deviceModel}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        "Score: ${device.overallScore}/100",
                        color = Color(0xFF00E5FF),
                        fontSize = 12.sp
                    )
                }

                val statusColor = when (device.status) {
                    DeviceStatus.IN_STOCK -> Color(0xFF00E676)
                    DeviceStatus.SOLD     -> Color(0xFFFF1744)
                    else                  -> Color(0xFFFFC107)
                }
                val statusText = when (device.status) {
                    DeviceStatus.IN_STOCK -> strings.inStock
                    DeviceStatus.SOLD     -> strings.sold
                    else                  -> device.status.name
                }
                Surface(
                    color = statusColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        statusText,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Divider(color = Color(0xFF2D2D44))
            Spacer(Modifier.height(12.dp))

            // ── الأسعار والربح
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PriceColumn(strings.purchasePrice, "${nf.format(device.purchasePrice)} TL", Color.Gray)
                PriceColumn(
                    strings.sellingPrice,
                    if (device.actualSoldPrice > 0) "${nf.format(device.actualSoldPrice)} TL" else "---",
                    Color.White
                )
                val profitColor = when {
                    profit > 0 -> Color(0xFF00E676)
                    profit < 0 -> Color(0xFFFF1744)
                    else       -> Color.Gray
                }
                PriceColumn(
                    strings.totalProfit,
                    if (profit != 0) "${if (profit > 0) "+" else ""}${nf.format(profit)} TL" else "---",
                    profitColor
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── أزرار الإجراءات
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // زر البيع / إعادة للمخزون
                if (device.status == DeviceStatus.IN_STOCK) {
                    Button(
                        onClick = onSellClick,
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E676).copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(strings.markAsSold, color = Color(0xFF00E676), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = onReturnClick,
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF2D2D44))
                    ) {
                        Text(strings.inStock, color = Color.Gray, fontSize = 12.sp)
                    }
                }

                // زر التقرير PDF
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = Color(0xFF6200EA),
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(
                        onClick = {
                            val pdfUrl = device.pdfUrl
                            val pdfLocalPath = device.pdfLocalPath

                            when {
                                pdfLocalPath != null && java.io.File(pdfLocalPath).exists() -> {
                                    com.diagnostics.app.utils.PdfActionHelper.openPdf(context, java.io.File(pdfLocalPath), strings)
                                }
                                pdfUrl != null -> {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(pdfUrl))
                                    context.startActivity(intent)
                                }
                                else -> {
                                    android.widget.Toast.makeText(context, strings.noPdfApp, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF6200EA).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = strings.downloadPdf,
                            tint = Color(0xFF6200EA),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceColumn(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 11.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
