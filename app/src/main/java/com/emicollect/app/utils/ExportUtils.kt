package com.emicollect.app.utils

import android.content.Context
import android.os.Environment
import com.emicollect.app.data.model.BackupData
import com.emicollect.app.data.local.entity.Customer
import com.emicollect.app.data.local.entity.Loan
import com.emicollect.app.data.local.entity.Transaction
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportUtils {

    fun generateExcelFile(context: Context, data: BackupData): File? {
        val workbook = XSSFWorkbook()

        // Sheet 1: Customers
        val customerSheet = workbook.createSheet("Customers")
        val headerRow = customerSheet.createRow(0)
        headerRow.createCell(0).setCellValue("ID")
        headerRow.createCell(1).setCellValue("Name")
        headerRow.createCell(2).setCellValue("Phone")
        headerRow.createCell(3).setCellValue("Address")
        headerRow.createCell(4).setCellValue("Frequency")
        
        data.customers.forEachIndexed { index, customer ->
            val row = customerSheet.createRow(index + 1)
            row.createCell(0).setCellValue(customer.id.toString())
            row.createCell(1).setCellValue(customer.name)
            row.createCell(2).setCellValue(customer.phone)
            row.createCell(3).setCellValue(customer.address)
            row.createCell(4).setCellValue(customer.frequency)
        }

        // Sheet 2: Loans
        val loanSheet = workbook.createSheet("Loans")
        val loanHeader = loanSheet.createRow(0)
        loanHeader.createCell(0).setCellValue("Loan ID")
        loanHeader.createCell(1).setCellValue("Customer ID")
        loanHeader.createCell(2).setCellValue("Item")
        loanHeader.createCell(3).setCellValue("Total Principal")
        loanHeader.createCell(4).setCellValue("Down Payment")
        loanHeader.createCell(5).setCellValue("Start Date")
        loanHeader.createCell(6).setCellValue("Status")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        data.loans.forEachIndexed { index, loan ->
            val row = loanSheet.createRow(index + 1)
            row.createCell(0).setCellValue(loan.loanId.toString())
            row.createCell(1).setCellValue(loan.customerId.toString())
            row.createCell(2).setCellValue(loan.itemName)
            row.createCell(3).setCellValue(loan.totalPrincipal)
            row.createCell(4).setCellValue(loan.downPayment)
            row.createCell(5).setCellValue(dateFormat.format(Date(loan.startDate)))
            row.createCell(6).setCellValue(if (loan.isClosed) "Closed" else "Active")
        }

        // Sheet 3: Transactions
        val txnSheet = workbook.createSheet("Transactions")
        val txnHeader = txnSheet.createRow(0)
        txnHeader.createCell(0).setCellValue("Txn ID")
        txnHeader.createCell(1).setCellValue("Loan ID")
        txnHeader.createCell(2).setCellValue("Amount")
        txnHeader.createCell(3).setCellValue("Date Paid")
        txnHeader.createCell(4).setCellValue("Payment Mode")

        data.transactions.forEachIndexed { index, txn ->
            val row = txnSheet.createRow(index + 1)
            row.createCell(0).setCellValue(txn.transactionId)
            row.createCell(1).setCellValue(txn.loanId.toString())
            row.createCell(2).setCellValue(txn.amountPaid)
            row.createCell(3).setCellValue(dateFormat.format(Date(txn.datePaid)))
            row.createCell(4).setCellValue(txn.paymentMode)
        }

        // Write to file
        return try {
            val fileName = "Emix_Export_${System.currentTimeMillis()}.xlsx"
            val file = File(context.cacheDir, fileName)
            val fileOut = FileOutputStream(file)
            workbook.write(fileOut)
            fileOut.close()
            workbook.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
