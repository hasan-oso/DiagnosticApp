package com.diagnostics.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diagnostics.app.utils.QrManager
import com.diagnostics.app.utils.language.LocalStrings
import com.diagnostics.app.viewmodel.InventoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateQrScreen(
    shopId: String,
    onBackClick: () -> Unit,
    inventoryViewModel: InventoryViewModel = viewModel()
) {
    val qrToken by inventoryViewModel.qrToken.collectAsState()
    val qrExpiry by inventoryViewModel.qrExpiry.collectAsState()
    val qrTimeLeft by inventoryViewModel.qrTimeLeft.collectAsState()
    val isLoading by inventoryViewModel.isLoading.collectAsState()
    val context = LocalContext.current
    val strings = LocalStrings.current

    LaunchedEffect(Unit) {
        // ولّد دائماً عند دخول الشاشة — يُلغي القديم تلقائياً
        inventoryViewModel.generateQrToken(shopId)
    }

    // توليد QR Bitmap من الـ token الحالي
    val qrBitmap = remember(qrToken) {
        qrToken?.let { token ->
            val content = QrManager.buildQrContent(shopId, token, qrExpiry)
            QrManager.generateQrBitmap(content, 512)
        }
    }

    // Countdown display
    val minutes = qrTimeLeft / 60
    val seconds = qrTimeLeft % 60
    val timeString = String.format("%02d:%02d", minutes, seconds)
    val timerColor = when {
        qrTimeLeft > 1800 -> Color(0xFF00E676)  // أخضر > 30 دقيقة
        qrTimeLeft > 600  -> Color(0xFFFFC107)  // أصفر > 10 دقائق
        else              -> Color(0xFFFF1744)  // أحمر < 10 دقائق
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.generateQrScreenTitle, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0A0F))
            )
        },
        containerColor = Color(0xFF0A0A0F)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = strings.scanQrToStart,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // QR Code Image
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        3.dp,
                        Brush.linearGradient(listOf(Color(0xFF6200EA), Color(0xFF00E5FF))),
                        RoundedCornerShape(16.dp)
                    )
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> CircularProgressIndicator(color = Color(0xFF6200EA))
                    qrBitmap != null -> {
                        androidx.compose.foundation.Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier
                                .size(260.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                    else -> {
                        Text(
                            text = strings.qrExpiredLabel,
                            color = Color(0xFFFF1744),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Countdown Timer
            if (qrToken != null) {
                Text(
                    text = "${strings.qrExpiresIn} $timeString",
                    color = timerColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = strings.qrValidForOneDevice,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // زر توليد رمز جديد
            Button(
                onClick = { inventoryViewModel.generateQrToken(shopId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A2E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF00E5FF))
                Spacer(modifier = Modifier.width(8.dp))
                Text("🔄 ${strings.generateNewQr}", color = Color(0xFF00E5FF), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // زر مشاركة
            OutlinedButton(
                onClick = {
                    qrToken?.let { token ->
                        val content = QrManager.buildQrContent(shopId, token, qrExpiry)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "${strings.qrShareTextPrefix}\n$content")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, strings.shareQrCode))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2D2D44)),
                shape = RoundedCornerShape(12.dp),
                enabled = qrToken != null
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = Color.LightGray)
                Spacer(modifier = Modifier.width(8.dp))
                Text("📤 ${strings.shareQr}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
