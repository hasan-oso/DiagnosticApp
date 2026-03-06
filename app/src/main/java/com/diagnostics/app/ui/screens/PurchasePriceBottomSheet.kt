package com.diagnostics.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diagnostics.app.utils.language.LocalStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasePriceBottomSheet(
    deviceName: String,
    onConfirm: (price: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var price by remember { mutableStateOf("") }
    val priceInt = price.toIntOrNull() ?: 0
    val isValid = priceInt > 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1A1A2E),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color(0xFF2D2D44), RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "💰 ${strings.purchasePrice}",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(deviceName, color = Color.Gray, fontSize = 14.sp)
            Spacer(Modifier.height(24.dp))

            // عرض السعر المدخل
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Color(0xFF0A0A0F), RoundedCornerShape(16.dp))
                    .border(
                        2.dp,
                        if (price.isNotEmpty()) Color(0xFF00E5FF) else Color(0xFF2D2D44),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (price.isEmpty()) "0" else price,
                        color = if (price.isEmpty()) Color.Gray else Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("TL", color = Color(0xFF00E5FF), fontSize = 20.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            // لوحة الأرقام
            NumericKeypad(
                onNumberClick = { num -> if (price.length < 7) price += num },
                onDeleteClick = { if (price.isNotEmpty()) price = price.dropLast(1) },
                onClearClick  = { price = "" }
            )

            Spacer(Modifier.height(16.dp))

            // زر الحفظ
            Button(
                onClick = { if (isValid) onConfirm(priceInt) },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isValid)
                                Brush.horizontalGradient(listOf(Color(0xFF6200EA), Color(0xFF00E5FF)))
                            else
                                Brush.horizontalGradient(listOf(Color(0xFF2D2D44), Color(0xFF2D2D44))),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(strings.save, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(strings.cancel, color = Color.Gray)
            }
        }
    }
}

@Composable
fun NumericKeypad(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onClearClick: () -> Unit
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("C", "0", "⌫")
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .background(Color(0xFF0A0A0F), RoundedCornerShape(12.dp))
                            .clickable {
                                when (key) {
                                    "⌫"  -> onDeleteClick()
                                    "C"  -> onClearClick()
                                    else -> onNumberClick(key)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            key,
                            color = when (key) {
                                "⌫"  -> Color(0xFFFF1744)
                                "C"  -> Color(0xFFFFC107)
                                else -> Color.White
                            },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
