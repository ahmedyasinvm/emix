package com.emicollect.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptGenerator {

    fun generateReceipt(
        context: Context,
        customerName: String,
        amount: Double,
        transactionId: String = "TXN-${System.currentTimeMillis()}"
    ): android.net.Uri? {
        // Dimensions
        val width = 1080
        val height = 1920
        
        // Colors
        val emeraldPrimary = Color.parseColor("#064E3B")
        val goldAccent = Color.parseColor("#FFD700")
        val blackText = Color.BLACK
        val whiteBg = Color.WHITE

        // Bitmap & Canvas
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(whiteBg)

        // Paints
        val paintHeader = Paint().apply {
            color = emeraldPrimary
            style = Paint.Style.FILL
        }
        
        val paintTextTitle = Paint().apply {
            color = goldAccent
            textSize = 80f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val paintTextLabel = Paint().apply {
            color = Color.GRAY
            textSize = 40f
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
        }

        val paintTextValue = Paint().apply {
            color = blackText
            textSize = 50f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
        }

        val paintAmount = Paint().apply {
            color = emeraldPrimary
            textSize = 120f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        // --- Draw Header ---
        val headerHeight = 300f
        canvas.drawRect(0f, 0f, width.toFloat(), headerHeight, paintHeader)
        
        // App Name
        canvas.drawText("EMI Collect", width / 2f, headerHeight / 2f + 30f, paintTextTitle)

        // --- Content ---
        var yPos = headerHeight + 150f
        val margin = 100f
        val listSpacing = 120f // space between items

        // Date
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val dateStr = dateFormat.format(Date())
        
        canvas.drawText("Date", margin, yPos, paintTextLabel)
        canvas.drawText(dateStr, margin, yPos + 60f, paintTextValue)
        yPos += listSpacing

        // Customer Name
        canvas.drawText("Customer", margin, yPos, paintTextLabel)
        canvas.drawText(customerName, margin, yPos + 60f, paintTextValue)
        yPos += listSpacing

        // Transaction ID
        canvas.drawText("Transaction ID", margin, yPos, paintTextLabel)
        canvas.drawText(transactionId, margin, yPos + 60f, paintTextValue)
        yPos += listSpacing * 2

        // Amount (Centered)
        canvas.drawText("Paid Amount", width / 2f, yPos, paintTextLabel.apply { textAlign = Paint.Align.CENTER })
        yPos += 140f
        canvas.drawText("₹${String.format("%.2f", amount)}", width / 2f, yPos, paintAmount)
        yPos += 200f
        
        // Footer
        val paintFooter = Paint().apply {
            color = Color.DKGRAY
            textSize = 40f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Thank you for your payment.", width / 2f, height - 100f, paintFooter)

        // --- Save to File ---
        return try {
            val imagesFolder = File(context.cacheDir, "images")
            imagesFolder.mkdirs()
            val file = File(imagesFolder, "receipt_${System.currentTimeMillis()}.jpg")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            stream.flush()
            stream.close()
            
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
