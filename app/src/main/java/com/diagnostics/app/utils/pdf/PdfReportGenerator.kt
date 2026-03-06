package com.diagnostics.app.utils.pdf

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.diagnostics.app.data.models.DeviceScanData
import com.diagnostics.app.data.models.auth.ShopInfo
import com.diagnostics.app.utils.LanguageManager
import com.diagnostics.app.utils.language.AppStrings
import com.diagnostics.app.utils.language.Strings
import com.diagnostics.app.utils.scoring.ScoreCalculator
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfReportGenerator(
    private val context: Context
) {
    private val scoreCalculator = ScoreCalculator()

    suspend fun generateReport(
        scanData: DeviceScanData,
        shopInfo: ShopInfo? = null,
        shopId: String? = null
    ): String? {
        try {
            val fileName = "NeuralCheck_Report_${scanData.scanId.take(8)}.pdf"
            val appLanguage = LanguageManager.getLanguage(context)
            val strings = AppStrings.get(appLanguage)

            // 1. Generate local PDF file
            val cacheFile = generatePdf(context, scanData, shopInfo, strings, appLanguage)

            // 2. Upload to Firebase
            if (shopId != null) {
                uploadPdfToFirebase(cacheFile, shopId, scanData.scanId)
            }

            // 3. Save to Downloads
            return savePdfToDownloads(cacheFile, fileName)

        } catch (e: Exception) {
            Log.e("PdfReportGenerator", "Failed to generate PDF", e)
            return null
        }
    }

    private fun generatePdf(
        context: Context,
        scanData: DeviceScanData,
        shopInfo: ShopInfo?,
        strings: Strings,
        language: LanguageManager.Language
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val width = pageInfo.pageWidth.toFloat()
        val height = pageInfo.pageHeight.toFloat()

        val isRtl = language == LanguageManager.Language.ARABIC
        val alignMode = if (isRtl) Paint.Align.RIGHT else Paint.Align.LEFT
        val contentStartX = if (isRtl) width - 40f else 40f

        // Colors
        val darkBg = Color.parseColor("#0D0D1A")
        val accent = Color.parseColor("#00E5FF")
        val textWhite = Color.WHITE
        val textGray = Color.parseColor("#AAAAAA")
        val successGreen = Color.parseColor("#00C853")
        val warningRed = Color.parseColor("#FF1744")
        val neuralPrimary = Color.parseColor("#6200EA")

        // Paints
        val titlePaint = Paint().apply {
            color = neuralPrimary
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val headerPaint = Paint().apply {
            color = textWhite
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = alignMode
        }
        val labelPaint = Paint().apply {
            color = textGray
            textSize = 12f
            textAlign = alignMode
        }
        val valuePaint = Paint().apply {
            color = textWhite
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = alignMode
        }
        val sectionTitlePaint = Paint().apply {
            color = accent
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = alignMode
        }

        // --- DRAW BACKGROUND ---
        canvas.drawColor(darkBg)

        // --- HEADER ---
        var currentY = 60f
        val shopName = shopInfo?.shopName?.takeIf { it.isNotBlank() && it.length > 1 } ?: "NeuralCheck"
        
        canvas.drawText("NeuralCheck", width / 2, currentY, titlePaint)
        currentY += 40f
        
        canvas.drawText(shopName, contentStartX, currentY, headerPaint)
        val scanTimestamp = scanData.scanTimestamp
        val dateStr = if (scanTimestamp > 0) {
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(scanTimestamp))
        } else strings.notAvailable
        
        val rightAlignPaint = Paint(labelPaint).apply { textAlign = if (isRtl) Paint.Align.LEFT else Paint.Align.RIGHT }
        val rightStartX = if (isRtl) 40f else width - 40f
        
        canvas.drawText(dateStr, rightStartX, currentY, rightAlignPaint)
        currentY += 20f

        canvas.drawText("Scan ID: ${scanData.scanId.take(8).uppercase()}", rightStartX, currentY, rightAlignPaint)
        currentY += 40f

        // Device Info & Overall Score
        val deviceName = "${scanData.deviceBrand} ${scanData.deviceModel}"
        canvas.drawText(deviceName, contentStartX, currentY, headerPaint)
        currentY += 30f
        
        val overallScore = scoreCalculator.calculateOverallScore(scanData).overall.toInt()
        val scoreColor = when {
            overallScore >= 80 -> successGreen
            overallScore >= 50 -> Color.parseColor("#FFB300")
            else -> warningRed
        }
        val scorePaint = Paint(headerPaint).apply { color = scoreColor; textSize = 22f }
        canvas.drawText("${strings.overallScore}: $overallScore/100", contentStartX, currentY, scorePaint)
        currentY += 50f

        // --- 2x2 GRID SECTIONS ---
        val col1X = if (isRtl) width - 40f else 40f
        val col2X = if (isRtl) width / 2 - 20f else width / 2 + 20f
        
        var rowTempY = currentY

        // BATTERY (Col 1, Row 1)
        drawSectionTitle(canvas, strings.battery, col1X, rowTempY, sectionTitlePaint)
        var bY = rowTempY + 25f
        
        val batteryHealth = scanData.batteryStressResult.estimatedHealthPercent
        val batteryCap = scanData.batteryDesignCapacityMah
        val degradation = scanData.batteryStressResult.capacityMetrics?.degradationPercent
        val degradationText = if (degradation != null && degradation > 0) "${String.format(Locale.US, "%.1f", degradation)}%" else strings.notAvailable

        bY = drawKeyValue(canvas, "Health", "$batteryHealth%", col1X, bY, labelPaint, valuePaint, isRtl)
        bY = drawKeyValue(canvas, "Capacity", "${batteryCap}mAh", col1X, bY, labelPaint, valuePaint, isRtl)
        bY = drawKeyValue(canvas, "Degradation", degradationText, col1X, bY, labelPaint, valuePaint, isRtl)

        // PERFORMANCE (Col 2, Row 1)
        drawSectionTitle(canvas, strings.performance, col2X, rowTempY, sectionTitlePaint)
        var pY = rowTempY + 25f

        val cpuScore = scanData.cpuStressResult.multiCoreScore
        val thermalPct = scanData.cpuStressResult.performanceDegradationPercent
        val thermalText = if (thermalPct > 0) "${String.format(Locale.US, "%.1f", thermalPct)}%" else strings.notAvailable
        val peakTemp = scanData.cpuStressResult.maxTempCelsius

        pY = drawKeyValue(canvas, "CPU Score", "$cpuScore", col2X, pY, labelPaint, valuePaint, isRtl)
        pY = drawKeyValue(canvas, "Throttling", thermalText, col2X, pY, labelPaint, valuePaint, isRtl)
        pY = drawKeyValue(canvas, "Peak Temp", "${peakTemp}C", col2X, pY, labelPaint, valuePaint, isRtl)

        currentY = maxOf(bY, pY) + 40f

        // STORAGE (Col 1, Row 2)
        rowTempY = currentY
        drawSectionTitle(canvas, strings.storage, col1X, rowTempY, sectionTitlePaint)
        var sY = rowTempY + 25f

        val storageType = scanData.storageStressResult.detectedType.replace("_", " ").takeIf { it.isNotBlank() } ?: strings.notAvailable
        val seqRead = scanData.storageStressResult.sequentialReadMbps
        val seqReadText = if (seqRead > 0) "${seqRead.toInt()} MB/s" else strings.notAvailable
        val seqWrite = scanData.storageStressResult.sequentialWriteMbps
        val seqWriteText = if (seqWrite > 0) "${seqWrite.toInt()} MB/s" else strings.notAvailable

        sY = drawKeyValue(canvas, "Type", storageType, col1X, sY, labelPaint, valuePaint, isRtl)
        sY = drawKeyValue(canvas, "Read", seqReadText, col1X, sY, labelPaint, valuePaint, isRtl)
        sY = drawKeyValue(canvas, "Write", seqWriteText, col1X, sY, labelPaint, valuePaint, isRtl)

        // SECURITY (Col 2, Row 2)
        drawSectionTitle(canvas, strings.security, col2X, rowTempY, sectionTitlePaint)
        var secY = rowTempY + 25f

        val isRooted = !scanData.safetyNetBasicIntegrity
        val rootStatus = if (isRooted) "ROOTED ⚠️" else "Intact"
        val secColorPaint = Paint(valuePaint).apply { color = if (isRooted) warningRed else successGreen }
        
        secY = drawKeyValueWithCustomPaint(canvas, "Status", rootStatus, col2X, secY, labelPaint, secColorPaint, isRtl)
        secY = drawKeyValue(canvas, "Play Integrity", scanData.playIntegrityVerdict, col2X, secY, labelPaint, valuePaint, isRtl)

        currentY = maxOf(sY, secY) + 60f

        // --- HARDWARE INFO ---
        drawSectionTitle(canvas, "Device Info", contentStartX, currentY, sectionTitlePaint)
        currentY += 25f
        
        val hwInfo1 = "Android ${scanData.androidVersion} | RAM: ${scanData.ramTotalMb / 1024}GB | ${scanData.storageTotalGb}GB"
        canvas.drawText(hwInfo1, contentStartX, currentY, valuePaint)
        currentY += 25f

        val ageText = String.format(Locale.US, "%.1f years", scanData.deviceAgeYears)
        val descText = if (scanData.deviceAgeYears > 1.5) "REFURBISHED / USED" else "NEW / LIGHTLY USED"
        canvas.drawText("Age: $ageText | $descText", contentStartX, currentY, valuePaint)

        // --- FOOTER ---
        val footerPaint = Paint().apply {
            color = textGray
            textSize = 10f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Generated by NeuralCheck - neuralcheck.net", width / 2, height - 40f, footerPaint)

        document.finishPage(page)

        val file = File(context.cacheDir, "${scanData.scanId}_gen.pdf")
        document.writeTo(FileOutputStream(file))
        document.close()
        return file
    }

    private fun drawSectionTitle(canvas: Canvas, text: String, x: Float, y: Float, paint: Paint) {
        canvas.drawText(text, x, y, paint)
    }

    private fun drawKeyValue(canvas: Canvas, key: String, value: String, x: Float, y: Float, keyPaint: Paint, valuePaint: Paint, isRtl: Boolean): Float {
        return drawKeyValueWithCustomPaint(canvas, key, value, x, y, keyPaint, valuePaint, isRtl)
    }

    private fun drawKeyValueWithCustomPaint(canvas: Canvas, key: String, value: String, x: Float, y: Float, keyPaint: Paint, valuePaint: Paint, isRtl: Boolean): Float {
        canvas.drawText("$key: ", x, y, keyPaint)
        val keyWidth = keyPaint.measureText("$key: ")
        val valX = if (isRtl) x - keyWidth else x + keyWidth
        canvas.drawText(value, valX, y, valuePaint)
        return y + 20f
    }

    private fun savePdfToDownloads(sourceFile: File, fileName: String): String? {
        if (!sourceFile.exists()) return null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = resolver.insert(collection, values)

                if (itemUri != null) {
                    resolver.openOutputStream(itemUri).use { out ->
                        sourceFile.inputStream().use { input ->
                            input.copyTo(out!!)
                        }
                    }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(itemUri, values, null, null)
                    
                    sourceFile.delete()
                    return "Saved to Downloads: $fileName"
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()

                val destFile = File(downloadsDir, fileName)
                sourceFile.copyTo(destFile, overwrite = true)
                
                sourceFile.delete()
                return destFile.absolutePath
            }
        } catch (e: Exception) {
            Log.e("PdfReportGenerator", "Failed to save to downloads", e)
        }
        return null
    }

    private suspend fun uploadPdfToFirebase(pdfFile: File, shopId: String, scanId: String) {
        try {
            val pdfBytes = pdfFile.readBytes()
            val storageRef = FirebaseStorage.getInstance().reference
                .child("shops/$shopId/reports/$scanId.pdf")
            
            storageRef.putBytes(pdfBytes).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("shops").document(shopId)
                .collection("inventory").document(scanId)
                .update(
                    mapOf(
                        "pdfUrl" to downloadUrl,
                        "pdfLocalPath" to pdfFile.absolutePath
                    )
                )
                .await()
            Log.d("PdfReportGenerator", "Firestore updated with PDF URL and Local Path")
        } catch (e: Exception) {
            Log.e("PdfReportGenerator", "Failed to upload PDF to Firebase", e)
        }
    }
}
