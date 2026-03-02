/*
 * Copyright (c) 2025 Hasan Oso. All Rights Reserved.
 * 
 * PROPRIETARY SOFTWARE - DO NOT DISTRIBUTE
 * 
 * This file is part of DiagnosticApp.
 * Unauthorized use is strictly prohibited.
 * 
 * Owner: Hasan Oso
 * Student ID: 230212925
 * Institution: OSTIM Technical University
 */
package com.diagnostics.app.utils.pdf

import android.content.Context
import com.diagnostics.app.data.models.DeviceScanData
import com.diagnostics.app.data.models.auth.ShopInfo
import com.diagnostics.app.utils.language.Language
import com.diagnostics.app.utils.language.ReportLanguage
import com.diagnostics.app.utils.language.ReportStrings
import com.diagnostics.app.utils.pricing.PriceCalculator
import com.diagnostics.app.utils.scoring.ScoreCalculator
import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * PdfReportGenerator — Generates a professional PDF report
 * with device health scores, price estimate, and shop branding.
 *
 * Supports Arabic, Turkish, and English via ReportStrings.
 * Uses iText7 for PDF generation.
 */
class PdfReportGenerator(
    private val context: Context,
    private val language: Language = Language.ARABIC
) {
    private val strings: ReportLanguage = ReportStrings.getLanguage(language)
    private val scoreCalculator = ScoreCalculator()
    private val priceCalculator = PriceCalculator()

    /**
     * Load Arabic-compatible font from assets.
     * Falls back to Helvetica if font file is not found.
     */
    private fun getArabicFont(): PdfFont {
        return try {
            val fontStream = context.assets.open("fonts/NotoSansArabic-Regular.ttf")
            val fontBytes = fontStream.readBytes()
            fontStream.close()
            PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H,
                PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED)
        } catch (e: Exception) {
            PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA)
        }
    }

    /**
     * Generate a complete PDF report for the scanned device.
     *
     * @param scanData The collected device scan data
     * @param shopInfo Optional shop branding info for the footer
     * @return The generated PDF file
     */
    fun generateReport(
        scanData: DeviceScanData,
        shopInfo: ShopInfo? = null
    ): File {
        val outputFile = File(
            context.filesDir,
            "NeuralCheck_Report_${scanData.scanId}.pdf"
        )

        val writer = PdfWriter(FileOutputStream(outputFile))
        val pdfDoc = PdfDocument(writer)
        val document = Document(pdfDoc, PageSize.A4)

        // Set margins
        document.setMargins(40f, 40f, 40f, 40f)

        // Add content sections
        addHeader(document, shopInfo)
        addDeviceInfo(document, scanData)
        addOverallScore(document, scanData)
        addDetailedScores(document, scanData)
        addRecommendations(document, scanData)
        addPriceSuggestion(document, scanData)
        addTechnicalDetails(document, scanData)
        addFooter(document, shopInfo, scanData)

        document.close()

        return outputFile
    }

    // ══════════════════════════════════════════════
    // Section: Header
    // ══════════════════════════════════════════════

    private fun addHeader(document: Document, shopInfo: ShopInfo?) {
        val font = getArabicFont()

        // Shop logo if available
        shopInfo?.logoUrl?.let { logoUrl ->
            try {
                val logoFile = File(logoUrl)
                if (logoFile.exists()) {
                    val imageData = ImageDataFactory.create(logoUrl)
                    val logo = Image(imageData)
                    logo.setWidth(80f)
                    logo.setHorizontalAlignment(HorizontalAlignment.CENTER)
                    document.add(logo)
                }
            } catch (e: Exception) {
                // Logo loading failed, continue without it
            }
        }

        // App title
        val title = Paragraph("NeuralCheck")
            .setFont(font)
            .setFontSize(24f)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(10f)
        document.add(title)

        // Report subtitle
        val subtitle = Paragraph(strings.reportTitle)
            .setFont(font)
            .setFontSize(14f)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20f)
        document.add(subtitle)

        addSeparator(document)
    }

    // ══════════════════════════════════════════════
    // Section: Device Info
    // ══════════════════════════════════════════════

    private fun addDeviceInfo(document: Document, scanData: DeviceScanData) {
        val font = getArabicFont()

        val sectionTitle = Paragraph(strings.deviceInfo)
            .setFont(font)
            .setFontSize(16f)
            .setBold()
            .setMarginTop(20f)
        document.add(sectionTitle)

        val table = Table(2)
        table.setWidth(UnitValue.createPercentValue(100f))

        addInfoRow(table, getLocalizedLabel("model"), scanData.deviceModel, font)
        addInfoRow(table, getLocalizedLabel("brand"), scanData.deviceManufacturer, font)
        addInfoRow(table, strings.age,
            String.format("%.1f %s", scanData.deviceAgeYears, getLocalizedLabel("years")), font)
        addInfoRow(table, getLocalizedLabel("status"), getLocalizedLabel("used"), font)

        document.add(table)
        addSeparator(document)
    }

    // ══════════════════════════════════════════════
    // Section: Overall Score
    // ══════════════════════════════════════════════

    private fun addOverallScore(document: Document, scanData: DeviceScanData) {
        val font = getArabicFont()
        val score = scoreCalculator.calculateOverallScore(scanData)

        val sectionTitle = Paragraph(strings.overallScore)
            .setFont(font)
            .setFontSize(16f)
            .setBold()
            .setMarginTop(20f)
        document.add(sectionTitle)

        // Big score number
        val scoreText = Paragraph("${score.overall.toInt()}/100")
            .setFont(font)
            .setFontSize(36f)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(10f)
        document.add(scoreText)

        // Grade letter
        val gradeText = Paragraph("${strings.grade}: ${score.grade}")
            .setFont(font)
            .setFontSize(24f)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
        document.add(gradeText)

        // Star rating
        val stars = getStarsForScore(score.overall)
        val starsText = Paragraph(stars)
            .setFontSize(20f)
            .setTextAlignment(TextAlignment.CENTER)
        document.add(starsText)

        // Rating text
        val rating = getRatingText(score.overall)
        val ratingText = Paragraph(rating)
            .setFont(font)
            .setFontSize(14f)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20f)
        document.add(ratingText)

        addSeparator(document)
    }

    // ══════════════════════════════════════════════
    // Section: Detailed Scores
    // ══════════════════════════════════════════════

    private fun addDetailedScores(document: Document, scanData: DeviceScanData) {
        val font = getArabicFont()
        val score = scoreCalculator.calculateOverallScore(scanData)

        val sectionTitle = Paragraph(strings.details)
            .setFont(font)
            .setFontSize(16f)
            .setBold()
            .setMarginTop(20f)
        document.add(sectionTitle)

        addScoreRow(document, font, strings.battery, score.battery, getBatteryDetails(scanData))
        addScoreRow(document, font, strings.performance, score.performance, getPerformanceDetails(scanData))
        addScoreRow(document, font, strings.storage, score.storage, getStorageDetails(scanData))
        addScoreRow(document, font, strings.age, score.age, getAgeDetails(scanData))
        addScoreRow(document, font, strings.features, score.features, getFeaturesDetails(scanData))

        addSeparator(document)
    }

    private fun addScoreRow(
        document: Document,
        font: PdfFont,
        label: String,
        score: Double,
        details: String
    ) {
        val scoreText = Paragraph("$label    ${score.toInt()}/100")
            .setFont(font)
            .setFontSize(12f)
            .setBold()
            .setMarginTop(10f)
        document.add(scoreText)

        // Visual progress bar
        val progressBar = getProgressBar(score)
        val progressText = Paragraph(progressBar)
            .setFontSize(10f)
        document.add(progressText)

        val detailsText = Paragraph(details)
            .setFont(font)
            .setFontSize(10f)
            .setMarginBottom(5f)
        document.add(detailsText)
    }

    // ══════════════════════════════════════════════
    // Section: Recommendations
    // ══════════════════════════════════════════════

    private fun addRecommendations(document: Document, scanData: DeviceScanData) {
        val font = getArabicFont()
        val score = scoreCalculator.calculateOverallScore(scanData)

        val sectionTitle = Paragraph(strings.recommendations)
            .setFont(font)
            .setFontSize(16f)
            .setBold()
            .setMarginTop(20f)
        document.add(sectionTitle)

        // Strengths
        if (score.strengths.isNotEmpty()) {
            val strengthsTitle = Paragraph("${strings.strengths}:")
                .setFont(font)
                .setFontSize(12f)
                .setBold()
                .setFontColor(DeviceRgb(46, 125, 50))
                .setMarginTop(10f)
            document.add(strengthsTitle)

            score.strengths.forEach { strength ->
                val text = Paragraph("  + ${strings.getString(strength)}")
                    .setFont(font)
                    .setFontSize(10f)
                document.add(text)
            }
        }

        // Issues
        if (score.issues.isNotEmpty()) {
            val issuesTitle = Paragraph("${strings.warnings}:")
                .setFont(font)
                .setFontSize(12f)
                .setBold()
                .setFontColor(DeviceRgb(198, 40, 40))
                .setMarginTop(10f)
            document.add(issuesTitle)

            score.issues.forEach { issue ->
                val text = Paragraph("  - ${strings.getString(issue)}")
                    .setFont(font)
                    .setFontSize(10f)
                document.add(text)
            }
        }

        addSeparator(document)
    }

    // ══════════════════════════════════════════════
    // Section: Price Suggestion
    // ══════════════════════════════════════════════

    private fun addPriceSuggestion(document: Document, scanData: DeviceScanData) {
        val font = getArabicFont()
        val priceRange = priceCalculator.calculateSuggestedPrice(scanData)

        val sectionTitle = Paragraph(strings.suggestedPrice)
            .setFont(font)
            .setFontSize(16f)
            .setBold()
            .setMarginTop(20f)
        document.add(sectionTitle)

        val priceText = Paragraph("${priceRange.min} - ${priceRange.max} TL")
            .setFont(font)
            .setFontSize(20f)
            .setBold()
            .setFontColor(DeviceRgb(46, 125, 50))
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(10f)
        document.add(priceText)

        val conditionText = Paragraph("(${priceRange.condition})")
            .setFont(font)
            .setFontSize(10f)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(10f)
        document.add(conditionText)

        addSeparator(document)
    }

    // ══════════════════════════════════════════════
    // Section: Technical Details
    // ══════════════════════════════════════════════

    private fun addTechnicalDetails(document: Document, scanData: DeviceScanData) {
        val font = getArabicFont()

        val sectionTitle = Paragraph("Neural Processing Details")
            .setFont(font)
            .setFontSize(14f)
            .setBold()
            .setMarginTop(20f)
        document.add(sectionTitle)

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val scanDate = Date(scanData.scanTimestamp)
        val durationMin = scanData.scanDurationSeconds / 60
        val durationSec = scanData.scanDurationSeconds % 60
        val qualityStr = scanData.batteryStressResult.dataQuality?.overallQuality ?: "N/A"

        val details = buildString {
            appendLine("  Scan Date: ${dateFormat.format(scanDate)}")
            appendLine("  Duration: ${durationMin}m ${durationSec}s")
            appendLine("  Telemetry Points: 82+")
            appendLine("  Neural Processing: On-Device")
            appendLine("  Data Quality: $qualityStr")
        }

        val detailsText = Paragraph(details)
            .setFont(font)
            .setFontSize(9f)
            .setMarginTop(5f)
        document.add(detailsText)
    }

    // ══════════════════════════════════════════════
    // Section: Footer
    // ══════════════════════════════════════════════

    private fun addFooter(document: Document, shopInfo: ShopInfo?, scanData: DeviceScanData) {
        val font = getArabicFont()

        addSeparator(document)

        // Shop branding
        shopInfo?.let { shop ->
            val shopName = when (language) {
                Language.ARABIC -> shop.shopNameAr
                Language.TURKISH -> shop.shopNameTr
                Language.ENGLISH -> shop.shopNameEn
            }.ifEmpty { shop.shopName }

            val shopText = Paragraph(shopName)
                .setFont(font)
                .setFontSize(12f)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
            document.add(shopText)

            if (shop.address.isNotEmpty()) {
                val addressText = Paragraph(shop.address)
                    .setFont(font)
                    .setFontSize(9f)
                    .setTextAlignment(TextAlignment.CENTER)
                document.add(addressText)
            }

            if (shop.phoneNumber.isNotEmpty()) {
                val phoneText = Paragraph(shop.phoneNumber)
                    .setFont(font)
                    .setFontSize(9f)
                    .setTextAlignment(TextAlignment.CENTER)
                document.add(phoneText)
            }
        }

        // Powered by
        val poweredBy = Paragraph("\nPowered by NeuralCheck")
            .setFontSize(8f)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(10f)
        document.add(poweredBy)

        val website = Paragraph("www.neuralcheck.net")
            .setFontSize(8f)
            .setTextAlignment(TextAlignment.CENTER)
        document.add(website)

        val reportId = Paragraph("Report ID: ${scanData.scanId.take(8)}")
            .setFontSize(7f)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(5f)
        document.add(reportId)
    }

    // ══════════════════════════════════════════════
    // Helper Functions
    // ══════════════════════════════════════════════

    private fun addSeparator(document: Document) {
        val separator = Paragraph("________________________________________")
            .setFontSize(8f)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(10f)
            .setMarginBottom(10f)
        document.add(separator)
    }

    private fun addInfoRow(table: Table, label: String, value: String, font: PdfFont) {
        table.addCell(
            Cell().add(Paragraph(label).setFont(font).setFontSize(10f))
                .setBorder(null)
        )
        table.addCell(
            Cell().add(Paragraph(value).setFont(font).setFontSize(10f).setBold())
                .setBorder(null)
        )
    }

    private fun getStarsForScore(score: Double): String {
        val fullStars = (score / 20).toInt()
        val emptyStars = 5 - fullStars
        return "★".repeat(fullStars) + "☆".repeat(emptyStars)
    }

    private fun getRatingText(score: Double): String = when {
        score >= 85 -> strings.excellent
        score >= 70 -> strings.veryGood
        score >= 55 -> strings.good
        score >= 40 -> strings.fair
        else -> strings.poor
    }

    private fun getProgressBar(score: Double): String {
        val filled = (score / 10).toInt()
        val empty = 10 - filled
        return "█".repeat(filled) + "░".repeat(empty)
    }

    // ══════════════════════════════════════════════
    // Detail Strings per Category
    // ══════════════════════════════════════════════

    private fun getBatteryDetails(scanData: DeviceScanData): String {
        val degradation = scanData.batteryStressResult.capacityMetrics
            ?.degradationPercent?.toDouble() ?: 0.0
        return "  ${strings.degradation}: ${String.format("%.1f", degradation)}% " +
            "(${strings.degradationMessage(degradation, scanData.deviceAgeYears.toDouble())})"
    }

    private fun getPerformanceDetails(scanData: DeviceScanData): String {
        val perfDeg = scanData.cpuStressResult.performanceDegradationPercent
        return "  Degradation: ${String.format("%.1f", perfDeg)}%"
    }

    private fun getStorageDetails(scanData: DeviceScanData): String {
        val type = scanData.storageStressResult.detectedType
        return "  Type: $type"
    }

    private fun getAgeDetails(scanData: DeviceScanData): String {
        return "  ${String.format("%.1f", scanData.deviceAgeYears)} ${getLocalizedLabel("years")}"
    }

    private fun getFeaturesDetails(scanData: DeviceScanData): String {
        val features = mutableListOf<String>()
        if (scanData.nfcAvailable) features.add("NFC")
        if (scanData.wifiAvailable) features.add("WiFi")
        if (scanData.bluetoothVersion.isNotEmpty()) features.add("BT ${scanData.bluetoothVersion}")
        if (scanData.cameraCount > 0) features.add("${scanData.cameraCount} cameras")
        return "  " + features.joinToString(", ")
    }

    // ══════════════════════════════════════════════
    // Localization Helpers
    // ══════════════════════════════════════════════

    private fun getLocalizedLabel(key: String): String = when (key) {
        "model" -> when (language) {
            Language.ARABIC -> "الموديل"
            Language.TURKISH -> "Model"
            Language.ENGLISH -> "Model"
        }
        "brand" -> when (language) {
            Language.ARABIC -> "الشركة"
            Language.TURKISH -> "Marka"
            Language.ENGLISH -> "Brand"
        }
        "status" -> when (language) {
            Language.ARABIC -> "الحالة"
            Language.TURKISH -> "Durum"
            Language.ENGLISH -> "Status"
        }
        "used" -> when (language) {
            Language.ARABIC -> "مستعمل"
            Language.TURKISH -> "İkinci El"
            Language.ENGLISH -> "Used"
        }
        "years" -> when (language) {
            Language.ARABIC -> "سنة"
            Language.TURKISH -> "yıl"
            Language.ENGLISH -> "years"
        }
        else -> key
    }
}
