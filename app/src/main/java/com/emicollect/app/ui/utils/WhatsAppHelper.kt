package com.emicollect.app.ui.utils

import android.content.Context
import android.content.Intent
import com.emicollect.app.data.local.entity.Loan

object WhatsAppHelper {
    fun shareReceipt(
        context: Context,
        customerName: String,
        amount: Double,
        itemName: String,
        balance: Double
    ) {
        val message = """
            Payment Received: ₹${String.format("%.2f", amount)}
            From: $customerName
            For: $itemName
            Balance Remaining: ₹${String.format("%.2f", balance)}
            
            Thanks!
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }

        // Try to target WhatsApp specifically
        intent.setPackage("com.whatsapp")

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // If WhatsApp is not installed, open chooser
            intent.setPackage(null)
            context.startActivity(Intent.createChooser(intent, "Share Receipt"))
        }
    }
}
