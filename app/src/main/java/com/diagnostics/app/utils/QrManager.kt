package com.diagnostics.app.utils

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.UUID

object QrManager {

    /** توليد QR كـ Bitmap */
    fun generateQrBitmap(content: String, size: Int = 512): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(
                    x, y,
                    if (bitMatrix[x, y]) android.graphics.Color.BLACK
                    else android.graphics.Color.WHITE
                )
            }
        }
        return bitmap
    }

    /** محتوى QR: JSON بسيط */
    fun buildQrContent(shopId: String, token: String, expiresAt: Long): String {
        return """{"shopId":"$shopId","token":"$token","exp":$expiresAt}"""
    }

    /** توليد token عشوائي (16 حرف) */
    fun generateToken(): String = UUID.randomUUID().toString().replace("-", "").take(16)

    /** التحقق من الصلاحية */
    fun isTokenValid(expiresAt: Long): Boolean {
        return System.currentTimeMillis() < expiresAt
    }

    /** وقت انتهاء الصلاحية (ساعة واحدة) */
    fun getExpiryTime(): Long = System.currentTimeMillis() + 3_600_000L
}
