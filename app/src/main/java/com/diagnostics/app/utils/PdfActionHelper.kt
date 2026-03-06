package com.diagnostics.app.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.diagnostics.app.utils.language.Strings
import android.util.Log
import java.io.File
import com.diagnostics.app.R

object PdfActionHelper {

    fun openPdf(context: Context, file: File, strings: Strings) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                openPdfInBrowser(context, uri, strings)
            }
        } catch (e: Exception) {
            Log.e("PDF", "Error opening PDF: ${e.message}")
            Toast.makeText(context, strings.errorOpeningPdf, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openPdfInBrowser(context: Context, uri: android.net.Uri, strings: Strings) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, strings.shareReport))
        } catch (e: Exception) {
            Toast.makeText(context, strings.noPdfApp, Toast.LENGTH_LONG).show()
        }
    }

    // مشاركة الملف
    fun sharePdf(context: Context, file: File, strings: Strings) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, strings.reportTitle)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, strings.shareReport))
        } catch (e: Exception) {
            Log.e("PDF", "Error sharing PDF: ${e.message}")
            Toast.makeText(context, strings.errorSharingPdf, Toast.LENGTH_SHORT).show()
        }
    }
}
