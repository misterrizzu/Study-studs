package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

object FileParserUtil {

    suspend fun readContentFromUri(context: Context, uri: Uri): String {
        val mimeType = context.contentResolver.getType(uri) ?: ""
        val fileName = getFileName(context, uri)

        return when {
            mimeType.contains("pdf", ignoreCase = true) || fileName.endsWith(".pdf", ignoreCase = true) -> {
                readPdfText(context, uri)
            }
            else -> {
                // Default to plain text reader
                readPlainText(context, uri)
            }
        }
    }

    private fun readPlainText(context: Context, uri: Uri): String {
        val stringBuilder = StringBuilder()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line).append("\n")
                        line = reader.readLine()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FileParserUtil", "Error reading plain text file", e)
            return "Error reading file: ${e.localizedMessage}"
        }
        return stringBuilder.toString().trim()
    }

    private fun readPdfText(context: Context, uri: Uri): String {
        val stringBuilder = StringBuilder()
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                renderer = PdfRenderer(pfd)
                val pageCount = renderer.pageCount
                stringBuilder.append("--- PDF Document (${pageCount} Pages) ---\n\n")

                // First attempt text stream reading if possible
                val textFromStream = readPlainText(context, uri)
                if (textFromStream.isNotBlank() && !textFromStream.startsWith("Error")) {
                    return textFromStream
                }

                // Fallback note
                stringBuilder.append("Note: PDF attached with $pageCount pages. Please review and edit the content below if necessary.\n")
            }
        } catch (e: Exception) {
            Log.e("FileParserUtil", "Error reading PDF", e)
            return readPlainText(context, uri)
        } finally {
            try {
                renderer?.close()
                pfd?.close()
            } catch (_: Exception) {}
        }
        return stringBuilder.toString()
    }

    fun getFileName(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex("_display_name")
                    if (index != -1) {
                        result = it.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "document.txt"
    }
}
