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
    private fun clearCache(context: Context) {
        try {
            val imagesFolder = File(context.cacheDir, "images")
            if (imagesFolder.exists()) {
                imagesFolder.listFiles()?.forEach { it.delete() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun generateReceipt(
        context: Context,
        customerName: String,
        amount: Double,
        transactionId: String = "TXN-${System.currentTimeMillis()}",
        businessName: String = ""
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
        
        // App Name / Business Name
        val receiptTitle = if (businessName.isNotBlank()) businessName.uppercase() else "EMIX OFFICIAL RECEIPT"
        canvas.drawText(receiptTitle, width / 2f, headerHeight / 2f + 30f, paintTextTitle)

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
            clearCache(context)
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

    // Event type for sort-order tie-breaking: Loan=0 (first), DownPayment=1, Payment=2
    private data class LedgerEvent(
        val timestamp: Long,
        val sortOrder: Int,
        val amount: Double,
        val description: String,
        var runningBalance: Double = 0.0
    )

    fun generateStatement(
        context: Context,
        customer: com.emicollect.app.data.local.entity.Customer,
        transactions: List<com.emicollect.app.data.local.entity.Transaction>,
        loans: List<com.emicollect.app.data.local.entity.Loan>,
        isCombined: Boolean = true,
        totalPrincipal: Double = -1.0,
        remainingBalance: Double = -1.0,
        businessName: String = ""
    ): android.net.Uri? {

        // ═══ MODULE 2: Build & Sort Events with Tie-Breaking ═══
        val events = mutableListOf<LedgerEvent>()
        loans.forEach { loan ->
            events.add(LedgerEvent(loan.startDate, 0, loan.totalPrincipal, "New Loan: ${loan.itemName}"))
        }
        transactions.forEach { txn ->
            val loanName = loans.find { it.loanId == txn.loanId }?.itemName ?: "Unknown"
            when (txn.type) {
                com.emicollect.app.data.local.entity.TransactionType.DOWN_PAYMENT -> {
                    events.add(LedgerEvent(txn.datePaid, 1, -txn.amountPaid, "Down Pmt (${txn.paymentMode})"))
                }
                else -> {
                    events.add(LedgerEvent(txn.datePaid, 2, -txn.amountPaid, "Payment (${txn.paymentMode})"))
                }
            }
        }
        // Sort: by timestamp, then Loan before DownPayment before Payment at same time
        events.sortWith(compareBy<LedgerEvent> { it.timestamp }.thenBy { it.sortOrder })

        // ═══ Calculate Running Balance ═══
        var runBal = 0.0
        events.forEach { evt ->
            runBal += evt.amount
            evt.runningBalance = runBal
        }

        // ═══ Totals ═══
        val finalPrincipal = if (totalPrincipal >= 0) totalPrincipal else loans.sumOf { it.totalPrincipal }
        val finalBalance = if (remainingBalance >= 0) remainingBalance else loans.sumOf { it.currentBalance }
        val finalPaid = loans.sumOf { it.totalPrincipal - it.currentBalance }

        // ═══ MODULE 3: Layout & Drawing ═══
        val width = 1080
        val pad = 60f
        val headerH = 320f
        val tHeaderH = 80f
        val rowH = 95f
        val tableContentH = maxOf(rowH, events.size * rowH)
        val summaryH = 460f
        val footerH = 100f
        val totalH = (pad + headerH + 40f + tHeaderH + tableContentH + 40f + summaryH + footerH + pad).toInt()

        // Colors
        val cBg      = Color.parseColor("#F8FAFC")
        val cCard    = Color.WHITE
        val cHead    = Color.parseColor("#064E3B")
        val cHeadBot = Color.parseColor("#065F46")
        val cGold    = Color.parseColor("#FFD700")
        val cEmerald = Color.parseColor("#059669")
        val cBlue    = Color.parseColor("#2563EB")
        val cText    = Color.parseColor("#1E293B")
        val cSub     = Color.parseColor("#64748B")
        val cDiv     = Color.parseColor("#E2E8F0")
        val cRowAlt  = Color.parseColor("#F1F5F9")
        val cRed     = Color.parseColor("#DC2626")
        val cRedBg   = Color.parseColor("#FEF2F2")

        val bitmap = Bitmap.createBitmap(width, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(cBg)

        fun p(c: Int, sz: Float, b: Boolean = false, a: Paint.Align = Paint.Align.LEFT) = Paint().apply {
            color = c; textSize = sz; isAntiAlias = true; textAlign = a
            typeface = if (b) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
        }
        val rp = Paint().apply { style = Paint.Style.FILL; isAntiAlias = true }

        var y = pad

        // ──────── HEADER ────────
        rp.color = cHead
        canvas.drawRoundRect(pad, y, width - pad, y + headerH, 32f, 32f, rp)
        rp.color = cHeadBot
        canvas.drawRoundRect(pad, y + headerH / 2, width - pad, y + headerH, 0f, 0f, rp)
        canvas.drawRoundRect(pad, y + headerH - 64f, width - pad, y + headerH, 32f, 32f, rp)
        rp.color = cGold
        canvas.drawRoundRect(pad, y, width - pad, y + 8f, 32f, 32f, rp)

        val stmtBrand = if (businessName.isNotBlank()) businessName.uppercase() else "EMIX"
        canvas.drawText(stmtBrand, width / 2f, y + 90f, p(cGold, 72f, true, Paint.Align.CENTER))
        canvas.drawText("Combined Statement", width / 2f, y + 145f, p(Color.WHITE, 36f, false, Paint.Align.CENTER))
        canvas.drawText(customer.name.uppercase(), width / 2f, y + 220f, p(Color.WHITE, 48f, true, Paint.Align.CENTER))
        canvas.drawText(customer.phone, width / 2f, y + 270f, p(Color.parseColor("#A7F3D0"), 32f, false, Paint.Align.CENTER))
        val todayStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
        canvas.drawText("Generated: $todayStr", width / 2f, y + 308f, p(Color.parseColor("#6EE7B7"), 26f, false, Paint.Align.CENTER))

        y += headerH + 40f

        // ──────── TABLE CARD ────────
        val tTop = y
        val tCardH = tHeaderH + tableContentH + 20f
        rp.color = cCard
        canvas.drawRoundRect(pad, tTop, width - pad, tTop + tCardH, 24f, 24f, rp)
        val bdr = Paint().apply { color = cDiv; style = Paint.Style.STROKE; strokeWidth = 2f; isAntiAlias = true }
        canvas.drawRoundRect(pad, tTop, width - pad, tTop + tCardH, 24f, 24f, bdr)

        // 4 Column positions
        val c1 = pad + 24f        // Date
        val c2 = 230f             // Description
        val c3 = 650f             // Amount
        val c4 = width - pad - 24f // Balance (right-aligned)

        // Table header bg
        rp.color = Color.parseColor("#F8FAFC")
        canvas.drawRoundRect(pad + 2f, tTop + 2f, width - pad - 2f, tTop + tHeaderH, 24f, 24f, rp)

        val hY = tTop + 52f
        canvas.drawText("DATE", c1, hY, p(cSub, 24f, true))
        canvas.drawText("DESCRIPTION", c2, hY, p(cSub, 24f, true))
        canvas.drawText("AMOUNT", c3, hY, p(cSub, 24f, true))
        canvas.drawText("BALANCE", c4, hY, p(cSub, 24f, true, Paint.Align.RIGHT))

        canvas.drawLine(pad + 20f, tTop + tHeaderH, width - pad - 20f, tTop + tHeaderH,
            Paint().apply { color = cDiv; strokeWidth = 2f })

        // ──────── ROWS ────────
        val df = SimpleDateFormat("dd MMM", Locale.getDefault())
        var rowY = tTop + tHeaderH
        events.forEachIndexed { idx, evt ->
            if (idx % 2 == 1) {
                rp.color = cRowAlt
                canvas.drawRect(pad + 2f, rowY, width - pad - 2f, rowY + rowH, rp)
            }
            val tY = rowY + 58f

            // Col 1: Date
            canvas.drawText(df.format(Date(evt.timestamp)), c1, tY, p(cText, 26f))

            // Col 2: Description
            canvas.drawText(evt.description, c2, tY, p(cText, 24f))

            // Col 3: Amount — Debit(Loan)=Blue, Credit(Payment)=Emerald
            val isDebit = evt.amount > 0
            val amtStr = if (isDebit) "+₹${String.format("%.0f", evt.amount)}" else "-₹${String.format("%.0f", -evt.amount)}"
            val amtColor = if (isDebit) cBlue else cEmerald
            canvas.drawText(amtStr, c3, tY, p(amtColor, 26f, true))

            // Col 4: Running Balance — Bold
            canvas.drawText("₹${String.format("%.0f", evt.runningBalance)}", c4, tY, p(cText, 28f, true, Paint.Align.RIGHT))

            if (idx < events.size - 1) {
                canvas.drawLine(pad + 20f, rowY + rowH, width - pad - 20f, rowY + rowH,
                    Paint().apply { color = cDiv; strokeWidth = 1f })
            }
            rowY += rowH
        }

        y = tTop + tCardH + 40f

        // ──────── SUMMARY CARD ────────
        val sTop = y
        rp.color = cCard
        canvas.drawRoundRect(pad, sTop, width - pad, sTop + summaryH, 24f, 24f, rp)
        canvas.drawRoundRect(pad, sTop, width - pad, sTop + summaryH, 24f, 24f, bdr)

        var sY = sTop + 60f
        val sL = pad + 40f; val sR = width - pad - 40f

        canvas.drawText("Total Principal", sL, sY, p(cSub, 34f))
        canvas.drawText("₹${String.format("%.2f", finalPrincipal)}", sR, sY, p(cText, 36f, true, Paint.Align.RIGHT))
        sY += 60f
        canvas.drawLine(sL, sY, sR, sY, Paint().apply { color = cDiv; strokeWidth = 1f })
        sY += 50f

        canvas.drawText("Total Collected", sL, sY, p(cSub, 34f))
        canvas.drawText("₹${String.format("%.2f", finalPaid)}", sR, sY, p(cEmerald, 36f, true, Paint.Align.RIGHT))
        sY += 60f
        canvas.drawLine(sL, sY, sR, sY, Paint().apply { color = cDiv; strokeWidth = 1f })
        sY += 50f

        // Red balance banner
        val bTop = sY - 10f; val bH = 110f
        rp.color = cRedBg
        canvas.drawRoundRect(sL - 10f, bTop, sR + 10f, bTop + bH, 16f, 16f, rp)
        rp.color = cRed
        canvas.drawRoundRect(sL - 10f, bTop, sL + 8f, bTop + bH, 16f, 16f, rp)

        canvas.drawText("Remaining Balance", sL + 30f, bTop + 50f, p(cRed, 36f, true))
        canvas.drawText("₹${String.format("%.2f", finalBalance)}", sR - 10f, bTop + 50f, p(cRed, 44f, true, Paint.Align.RIGHT))

        sY = bTop + bH + 40f
        canvas.drawText("Thank you for choosing EMIX  •  ${customer.phone}", width / 2f, sY, p(cSub, 26f, false, Paint.Align.CENTER))

        // ──────── FOOTER ────────
        y = totalH - pad - 30f
        rp.color = cGold
        canvas.drawRect(pad, y, width - pad, y + 4f, rp)
        y += 30f
        canvas.drawText("EMIX Collection App", width / 2f, y, p(cSub, 24f, false, Paint.Align.CENTER))

        // ──────── SAVE ────────
        return try {
            clearCache(context)
            val file = File(context.cacheDir, "images/statement_${customer.id}_${System.currentTimeMillis()}.png")
            file.parentFile?.mkdirs()
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush(); out.close()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) { e.printStackTrace(); null }
    }
}
