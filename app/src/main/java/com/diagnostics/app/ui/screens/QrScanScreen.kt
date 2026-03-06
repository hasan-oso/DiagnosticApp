package com.diagnostics.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diagnostics.app.utils.QrManager
import com.diagnostics.app.utils.language.LocalStrings
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import org.json.JSONObject

@Composable
fun QrScanScreen(
    onQrScanned: (shopId: String) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalStrings.current

    // ── ZXing scanner launcher ──
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        try {
            val raw = result.contents ?: return@rememberLauncherForActivityResult
            android.util.Log.d("QR_DEBUG", "Raw QR: $raw")

            val json   = JSONObject(raw)
            val shopId = json.optString("shopId", "")
            val token  = json.optString("token", "")
            val exp    = json.optLong("exp", 0L)

            android.util.Log.d("QR_DEBUG", "Parsed → shopId=$shopId  token=$token  exp=$exp")

            when {
                shopId.isEmpty() || token.isEmpty() -> {
                    Toast.makeText(context, strings.qrInvalid, Toast.LENGTH_SHORT).show()
                }
                !QrManager.isTokenValid(exp) -> {
                    android.util.Log.d("QR_DEBUG", "Token expired locally")
                    Toast.makeText(context, strings.qrExpired, Toast.LENGTH_LONG).show()
                }
                else -> {
                    // انتقل فوراً بدون Firestore — التحقق المحلي كافي
                    onQrScanned(shopId)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("QR_DEBUG", "Scan parse error: ${e.message}")
            Toast.makeText(context, "${strings.qrScanError}: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    // ── Camera permission launcher ──
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                scanLauncher.launch(
                    ScanOptions().apply {
                        setPrompt(strings.scanShopQr)
                        setBeepEnabled(true)
                        setOrientationLocked(false)
                        setBarcodeImageEnabled(false)
                        setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(context, strings.error, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, strings.cameraPermissionRequired, Toast.LENGTH_LONG).show()
        }
    }

    // ── دالة تشغيل الكاميرا مع فحص Permission ──
    fun launchScanner() {
        try {
            when {
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> {
                    scanLauncher.launch(
                        ScanOptions().apply {
                            setPrompt(strings.scanShopQr)
                            setBeepEnabled(true)
                            setOrientationLocked(false)
                            setBarcodeImageEnabled(false)
                            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                        }
                    )
                }
                else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        } catch (e: Exception) {
            Toast.makeText(context, strings.error, Toast.LENGTH_SHORT).show()
        }
    }

    // ── UI ──
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = strings.scanShopQr,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = strings.scanQrSubtitle,
            color = Color.Gray,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // زر مسح QR
        Button(
            onClick = { launchScanner() },
            modifier = Modifier.size(200.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF6200EA), Color(0xFF00E5FF))),
                        RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📷", fontSize = 56.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        strings.scanQrCode,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }       // close Column
            }               // close Box
        }                   // close Button

    }                       // close outer Column
}
