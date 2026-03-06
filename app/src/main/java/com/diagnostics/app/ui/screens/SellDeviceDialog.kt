package com.diagnostics.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diagnostics.app.data.models.auth.InventoryDevice
import com.diagnostics.app.utils.language.LocalStrings

@Composable
fun SellDeviceDialog(
    device: InventoryDevice,
    onConfirm: (soldPrice: Int, buyerNote: String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    var soldPrice by remember { mutableStateOf("") }
    var buyerNote by remember { mutableStateOf("") }

    val priceInt = soldPrice.toIntOrNull() ?: 0
    val profit = priceInt - device.purchasePrice
    val profitColor = when {
        profit > 0 -> Color(0xFF00E676)
        profit < 0 -> Color(0xFFFF1744)
        else       -> Color.Gray
    }
    val isValid = priceInt > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A2E),
        shape = RoundedCornerShape(20.dp),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📱", fontSize = 32.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "${device.deviceBrand} ${device.deviceModel}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // سعر الشراء للمرجع
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(strings.purchasePrice, color = Color.Gray, fontSize = 14.sp)
                    Text(
                        "${device.purchasePrice} TL",
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }

                Divider(color = Color(0xFF2D2D44))

                // حقل سعر البيع
                OutlinedTextField(
                    value = soldPrice,
                    onValueChange = { soldPrice = it.filter { c -> c.isDigit() } },
                    label = { Text(strings.sellingPrice, color = Color.Gray) },
                    placeholder = { Text("0 ${strings.currencySymbol}", color = Color.Gray) },
                    suffix = { Text(strings.currencySymbol, color = Color(0xFF00E5FF)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF2D2D44),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF00E5FF)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // حقل ملاحظة اختياري
                OutlinedTextField(
                    value = buyerNote,
                    onValueChange = { buyerNote = it },
                    label = { Text(strings.noteOptional, color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6200EA),
                        unfocusedBorderColor = Color(0xFF2D2D44),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF6200EA)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                // بطاقة الربح المتوقع
                if (soldPrice.isNotEmpty() && priceInt > 0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0F)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                strings.profit,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                            Text(
                                "${if (profit > 0) "+" else ""}$profit ${strings.currencySymbol}",
                                color = profitColor,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // زر تأكيد البيع
                Button(
                    onClick = { if (isValid) onConfirm(priceInt, buyerNote) },
                    enabled = isValid,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
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
                        Text(
                            strings.markAsSold,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                // زر الإلغاء
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings.cancel, color = Color.Gray)
                }
            }
        }
    )
}
