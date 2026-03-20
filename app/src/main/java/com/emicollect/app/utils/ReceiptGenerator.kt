package com.emicollect.app.utils

import android.content.Context
import android.graphics.*
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

    private data class LedgerEvent(
        val timestamp: Long,
        val sortOrder: Int,
        val amount: Double,
        val description: String,
        var runningBalance: Double = 0.0
    )

    fun generateReceipt(
        context: Context,
        customerName: String,
        amount: Double,
        transactionId: String = "TXN-${System.currentTimeMillis()}",
        businessName: String = "",
        itemName: String = ""
    ): android.net.Uri? {
        val width = 1080
        val height = 1920
        
        val emeraldPrimary = Color.parseColor("#064E3B")
        val goldAccent = Color.parseColor("#FFD700")
        val blackText = Color.BLACK
        val whiteBg = Color.WHITE

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(whiteBg)

        val paintHeader = Paint().apply { color = emeraldPrimary; style = Paint.Style.FILL }
        val paintTextTitle = Paint().apply { color = goldAccent; textSize = 80f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER; isAntiAlias = true }
        val paintTextLabel = Paint().apply { color = Color.GRAY; textSize = 40f; textAlign = Paint.Align.LEFT; isAntiAlias = true }
        val paintTextValue = Paint().apply { color = blackText; textSize = 50f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.LEFT; isAntiAlias = true }
        val paintAmount = Paint().apply { color = emeraldPrimary; textSize = 120f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER; isAntiAlias = true }

        val headerHeight = 300f
        canvas.drawRect(0f, 0f, width.toFloat(), headerHeight, paintHeader)
        val receiptTitle = if (businessName.isNotBlank()) businessName.uppercase() else "EMIX OFFICIAL RECEIPT"
        canvas.drawText(receiptTitle, width / 2f, headerHeight / 2f + 30f, paintTextTitle)

        var yPos = headerHeight + 150f
        val margin = 100f
        val listSpacing = 120f
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val dateStr = dateFormat.format(Date())
        
        canvas.drawText("Date", margin, yPos, paintTextLabel)
        canvas.drawText(dateStr, margin, yPos + 60f, paintTextValue)
        yPos += listSpacing

        canvas.drawText("Customer", margin, yPos, paintTextLabel)
        canvas.drawText(customerName, margin, yPos + 60f, paintTextValue)
        yPos += listSpacing

        if (itemName.isNotBlank()) {
            canvas.drawText("For Item", margin, yPos, paintTextLabel)
            canvas.drawText(itemName, margin, yPos + 60f, paintTextValue)
            yPos += listSpacing
        }

        canvas.drawText("Transaction ID", margin, yPos, paintTextLabel)
        canvas.drawText(transactionId, margin, yPos + 60f, paintTextValue)
        yPos += listSpacing * 2

        canvas.drawText("Paid Amount", width / 2f, yPos, paintTextLabel.apply { textAlign = Paint.Align.CENTER })
        yPos += 140f
        canvas.drawText("₹${String.format("%.2f", amount)}", width / 2f, yPos, paintAmount)
        yPos += 200f
        
        val paintFooter = Paint().apply { color = Color.DKGRAY; textSize = 40f; textAlign = Paint.Align.CENTER; isAntiAlias = true }
        canvas.drawText("Thank you for your payment.", width / 2f, height - 100f, paintFooter)

        return try {
            clearCache(context)
            val imagesFolder = File(context.cacheDir, "images")
            imagesFolder.mkdirs()
            val file = File(imagesFolder, "receipt_${System.currentTimeMillis()}.jpg")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            stream.flush()
            stream.close()
            
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateStatement(
        context: Context,
        customer: com.emicollect.app.data.local.entity.Customer,
        transactions: List<com.emicollect.app.data.local.entity.Transaction>,
        loans: List<com.emicollect.app.data.local.entity.Loan>,
        isCombined: Boolean = true,
        totalPrincipal: Double = -1.0,
        remainingBalance: Double = -1.0,
        businessName: String = "",
        periodStart: Long? = null,
        periodEnd: Long? = null
    ): android.net.Uri? {
        
        // ═══ MODULE 1: Build & Sort ALL Events ═══
        val events = mutableListOf<LedgerEvent>()
        loans.forEach { loan ->
            events.add(LedgerEvent(loan.startDate, 0, loan.totalPrincipal, "New: ${loan.itemName}"))
        }
        transactions.forEach { txn ->
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

        // ═══ MODULE 2: Calculate Running Balance (Global) ═══
        var runBal = 0.0
        events.forEach { evt ->
            runBal += evt.amount
            evt.runningBalance = runBal
        }

        // ═══ MODULE 3: Filter for Display Period ═══
        val displayEvents = events.filter {
            (periodStart == null || it.timestamp >= periodStart) &&
            (periodEnd == null || it.timestamp <= periodEnd)
        }

        // ═══ Global Totals (Always show current actual status) ═══
        val finalPrincipal = if (totalPrincipal >= 0) totalPrincipal else loans.sumOf { it.totalPrincipal }
        val finalBalance = if (remainingBalance >= 0) remainingBalance else loans.sumOf { it.currentBalance }
        val finalPaid = loans.sumOf { it.totalPrincipal - it.currentBalance }

        // ═══ MODULE 4: Layout & Drawing Dimensions ═══
        val width = 1080
        val pad = 48f
        val headerH = 340f
        val tHeaderH = 90f
        val rowH = 100f
        
        // If no events in period, still draw empty table
        val displayedRows = maxOf(1, displayEvents.size)
        val tableContentH = displayedRows * rowH
        val summaryH = 480f
        val footerH = 120f
        
        val totalH = (pad + headerH + 50f + tHeaderH + tableContentH + 50f + summaryH + footerH + pad).toInt()

        // Premium Color Palette
        val cBg      = Color.parseColor("#F1F5F9") // Slate 100
        val cCard    = Color.parseColor("#FFFFFF") // White
        val cHeadTop = Color.parseColor("#064E3B") // Emerald 900
        val cHeadBot = Color.parseColor("#047857") // Emerald 700
        val cGold    = Color.parseColor("#F59E0B") // Amber 500
        val cEmerald = Color.parseColor("#059669") // Emerald 600
        val cBlue    = Color.parseColor("#2563EB") // Blue 600
        val cText    = Color.parseColor("#0F172A") // Slate 900
        val cSub     = Color.parseColor("#64748B") // Slate 500
        val cDiv     = Color.parseColor("#E2E8F0") // Slate 200
        val cRowAlt  = Color.parseColor("#F8FAFC") // Slate 50
        val cRed     = Color.parseColor("#DC2626") // Red 600
        val cRedBg   = Color.parseColor("#FEF2F2") // Red 50

        val bitmap = Bitmap.createBitmap(width, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(cBg)

        // Utility constraints & brushes
        fun p(c: Int, sz: Float, b: Boolean = false, a: Paint.Align = Paint.Align.LEFT) = Paint().apply {
            color = c; textSize = sz; isAntiAlias = true; textAlign = a
            typeface = if (b) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
        }
        val rp = Paint().apply { style = Paint.Style.FILL; isAntiAlias = true }

        // --- Draw Watermark ---
        val watermarkPaint = Paint().apply {
            color = cHeadTop
            alpha = 15 // Very faint
            textSize = 200f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.save()
        canvas.rotate(-35f, width / 2f, totalH / 2f)
        canvas.drawText("EMIX STATEMENT", width / 2f, totalH / 2f, watermarkPaint)
        if (finalBalance <= 0) {
            watermarkPaint.color = cEmerald
            watermarkPaint.alpha = 25
            canvas.drawText("SETTLED IN FULL", width / 2f, (totalH / 2f) + 250f, watermarkPaint)
        }
        canvas.restore()

        var y = pad

        // ──────── HEADER CARD ────────
        // Header Gradient Rect
        rp.shader = LinearGradient(0f, y, width.toFloat(), y + headerH, cHeadTop, cHeadBot, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(pad, y, width - pad, y + headerH, 40f, 40f, rp)
        rp.shader = null // reset
        
        // Gold accent top line
        rp.color = cGold
        canvas.drawRoundRect(pad, y, width - pad, y + 12f, 40f, 40f, rp)
        
        // Draw subtle pattern on header (Optional: just a few circles for texture)
        rp.color = Color.WHITE
        rp.alpha = 10
        canvas.drawCircle(width.toFloat(), y, 150f, rp)
        canvas.drawCircle(pad, y + headerH, 200f, rp)
        rp.alpha = 255 // reset

        val stmtBrand = if (businessName.isNotBlank()) businessName.uppercase() else "EMIX COLLECTION"
        canvas.drawText(stmtBrand, pad + 50f, y + 90f, p(cGold, 56f, true, Paint.Align.LEFT))
        
        val stmtType = if (isCombined) "Combined Statement" else "Itemized Statement"
        canvas.drawText(stmtType, width - pad - 50f, y + 90f, p(Color.parseColor("#A7F3D0"), 36f, true, Paint.Align.RIGHT))

        val periodText = if (periodStart != null && periodEnd != null) {
            val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            "Period: ${df.format(Date(periodStart))} - ${df.format(Date(periodEnd))}"
        } else {
            "Period: All Time"
        }
        canvas.drawText(periodText, width - pad - 50f, y + 140f, p(Color.WHITE, 28f, false, Paint.Align.RIGHT))

        canvas.drawText("CUSTOMER DETAILS", pad + 50f, y + 180f, p(Color.parseColor("#6EE7B7"), 24f, true))
        canvas.drawText(customer.name.uppercase(), pad + 50f, y + 240f, p(Color.WHITE, 56f, true))
        canvas.drawText(customer.phone, pad + 50f, y + 295f, p(Color.WHITE, 36f, false))

        y += headerH + 50f

        // ──────── TABLE SECTION ────────
        val tTop = y
        val tCardH = tHeaderH + tableContentH + 20f
        
        // Table Shadow (Simulated)
        rp.color = Color.parseColor("#E2E8F0")
        canvas.drawRoundRect(pad + 8f, tTop + 8f, width - pad + 8f, tTop + tCardH + 8f, 32f, 32f, rp)
        
        // Table Background
        rp.color = cCard
        canvas.drawRoundRect(pad, tTop, width - pad, tTop + tCardH, 32f, 32f, rp)
        val bdr = Paint().apply { color = cDiv; style = Paint.Style.STROKE; strokeWidth = 2f; isAntiAlias = true }
        canvas.drawRoundRect(pad, tTop, width - pad, tTop + tCardH, 32f, 32f, bdr)

        // Column Positions
        val c1 = pad + 40f        // Date
        val c2 = 250f             // Description
        val c3 = 650f             // Amount
        val c4 = width - pad - 40f // Balance (right-aligned)

        // Table Header fill
        rp.color = cRowAlt
        canvas.drawRoundRect(pad + 2f, tTop + 2f, width - pad - 2f, tTop + tHeaderH, 32f, 32f, rp)
        
        // Fix top corners rounding leaking to bottom of header
        canvas.drawRect(pad + 2f, tTop + tHeaderH - 20f, width - pad - 2f, tTop + tHeaderH, rp)
        canvas.drawLine(pad, tTop + tHeaderH, width - pad, tTop + tHeaderH, Paint().apply { color = cDiv; strokeWidth = 2f })

        val hY = tTop + 58f
        canvas.drawText("DATE", c1, hY, p(cSub, 26f, true))
        canvas.drawText("DESCRIPTION", c2, hY, p(cSub, 26f, true))
        canvas.drawText("AMOUNT", c3, hY, p(cSub, 26f, true))
        canvas.drawText("BALANCE", c4, hY, p(cSub, 26f, true, Paint.Align.RIGHT))

        // ──────── ROWS ────────
        val dfRow = SimpleDateFormat("dd MMM yy", Locale.getDefault())
        var rowY = tTop + tHeaderH
        
        if (displayEvents.isEmpty()) {
            val emptyY = rowY + 60f
            canvas.drawText("No transactions in this period.", width / 2f, emptyY, p(cSub, 32f, false, Paint.Align.CENTER))
        } else {
            displayEvents.forEachIndexed { idx, evt ->
                if (idx % 2 == 1) {
                    rp.color = cRowAlt
                    canvas.drawRect(pad + 2f, rowY, width - pad - 2f, rowY + rowH, rp)
                }
                val tY = rowY + 64f

                // Col 1: Date
                canvas.drawText(dfRow.format(Date(evt.timestamp)), c1, tY, p(cText, 28f))

                // Col 2: Description
                canvas.drawText(evt.description, c2, tY, p(cText, 26f))

                // Col 3: Amount
                val isDebit = evt.amount > 0
                val amtStr = if (isDebit) "+₹${String.format("%.0f", evt.amount)}" else "-₹${String.format("%.0f", -evt.amount)}"
                val amtColor = if (isDebit) cBlue else cEmerald
                canvas.drawText(amtStr, c3, tY, p(amtColor, 28f, true))

                // Col 4: Running Balance
                canvas.drawText("₹${String.format("%.0f", evt.runningBalance)}", c4, tY, p(cText, 30f, true, Paint.Align.RIGHT))

                if (idx < displayEvents.size - 1) {
                    canvas.drawLine(pad + 40f, rowY + rowH, width - pad - 40f, rowY + rowH, Paint().apply { color = cDiv; strokeWidth = 1f })
                }
                rowY += rowH
            }
        }

        y = tTop + tCardH + 50f

        // ──────── SUMMARY CARD ────────
        val sTop = y
        // Shadow
        rp.color = Color.parseColor("#E2E8F0")
        canvas.drawRoundRect(pad + 8f, sTop + 8f, width - pad + 8f, sTop + summaryH + 8f, 32f, 32f, rp)
        
        rp.color = cCard
        canvas.drawRoundRect(pad, sTop, width - pad, sTop + summaryH, 32f, 32f, rp)
        canvas.drawRoundRect(pad, sTop, width - pad, sTop + summaryH, 32f, 32f, bdr)

        var sY = sTop + 70f
        val sL = pad + 50f; val sR = width - pad - 50f

        canvas.drawText("Global Final Principal", sL, sY, p(cSub, 36f))
        canvas.drawText("₹${String.format("%.2f", finalPrincipal)}", sR, sY, p(cText, 40f, true, Paint.Align.RIGHT))
        sY += 60f
        canvas.drawLine(sL, sY, sR, sY, Paint().apply { color = cDiv; strokeWidth = 1f; pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f) })
        sY += 60f

        canvas.drawText("Total Collected (All Time)", sL, sY, p(cSub, 36f))
        canvas.drawText("₹${String.format("%.2f", finalPaid)}", sR, sY, p(cEmerald, 40f, true, Paint.Align.RIGHT))
        sY += 60f
        canvas.drawLine(sL, sY, sR, sY, Paint().apply { color = cDiv; strokeWidth = 1f; pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f) })
        sY += 60f

        // Balance Banner
        val bTop = sY - 10f; val bH = 130f
        if (finalBalance > 0) {
            rp.color = cRedBg
            canvas.drawRoundRect(pad + 2f, bTop, width - pad - 2f, bTop + bH, 0f, 0f, rp) // no round on sides to bleed to edge
            
            // Left thick stroke
            rp.color = cRed
            canvas.drawRect(pad + 2f, bTop, pad + 16f, bTop + bH, rp)

            canvas.drawText("Current Outstanding Balance", pad + 40f, bTop + 75f, p(cRed, 36f, true))
            canvas.drawText("₹${String.format("%.2f", finalBalance)}", sR, bTop + 80f, p(cRed, 52f, true, Paint.Align.RIGHT))
        } else {
            rp.color = Color.parseColor("#ECFDF5") // Emerald 50
            canvas.drawRoundRect(pad + 2f, bTop, width - pad - 2f, bTop + bH, 0f, 0f, rp)
            
            rp.color = cEmerald
            canvas.drawRect(pad + 2f, bTop, pad + 16f, bTop + bH, rp)

            canvas.drawText("Status", pad + 40f, bTop + 75f, p(cEmerald, 36f, true))
            canvas.drawText("SETTLED IN FULL", sR, bTop + 80f, p(cEmerald, 48f, true, Paint.Align.RIGHT))
        }

        sY = bTop + bH + 50f
        val footerDateFormatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val generatedOn = footerDateFormatter.format(Date())
        canvas.drawText("Generated on: $generatedOn", width / 2f, sY, p(cSub, 28f, false, Paint.Align.CENTER))

        // ──────── FOOTER ────────
        y = totalH - pad - 40f
        canvas.drawText("Thank you for choosing $stmtBrand.", width / 2f, y, p(cSub, 30f, false, Paint.Align.CENTER))
        
        // Bottom colored line
        rp.color = cGold
        canvas.drawRect(pad, y + 20f, width - pad, y + 26f, rp)

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
