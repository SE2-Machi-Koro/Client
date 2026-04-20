package com.machikoro.client.ui.start

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

fun openRulesPdf(context: Context) {
    try {
        val fileName = "rules.pdf"
        val file = File(context.cacheDir, fileName)

        // Copy from assets to cache if not already there
        if (!file.exists()) {
            context.assets.open(fileName).use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // Enable fit to width for better horizontal viewing
            putExtra("fit_to_page", true)
            putExtra("page_numbers", true)
        }

        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No PDF viewer app found", Toast.LENGTH_SHORT).show()
    }
}

