package com.diagnostics.app.ui.screens

// ملاحظة: هذا الملف محتفظ به للتوافق — الشاشة الرئيسية أصبحت GenerateQrScreen
// يتم الوصول إليها عبر route "generate_qr" في MainActivity

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diagnostics.app.viewmodel.InventoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratePinScreen(
    shopId: String,
    onBackClick: () -> Unit,
    inventoryViewModel: InventoryViewModel = viewModel()
) {
    // تم استبدال نظام PIN بـ QR Code
    // هذه الشاشة محتفظ بها للتوافق فقط
    GenerateQrScreen(
        shopId = shopId,
        onBackClick = onBackClick,
        inventoryViewModel = inventoryViewModel
    )
}
