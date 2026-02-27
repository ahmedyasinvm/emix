package com.emicollect.app.ui.details

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.emicollect.app.data.local.entity.Loan
import com.emicollect.app.data.local.entity.Transaction
import com.emicollect.app.ui.components.GlassCard
import com.emicollect.app.ui.components.SmartPaymentDialog
import com.emicollect.app.ui.theme.EmeraldPrimary
import com.emicollect.app.ui.theme.GoldAccent
import com.emicollect.app.ui.theme.TextWhite
import com.emicollect.app.utils.ReceiptGenerator
import com.emicollect.app.utils.ShareUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    onBackClick: () -> Unit,
    viewModel: CustomerDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ─── Dialog state ─────────────────────────────────────────────────────────
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showAddLoanDialog by remember { mutableStateOf(false) }
    var selectedLoanId by remember { mutableStateOf<Long?>(null) }
    var selectedLoanBalance by remember { mutableStateOf(0.0) }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showEditCustomerDialog by remember { mutableStateOf(false) }
    var showDeleteCustomerDialog by remember { mutableStateOf(false) }
    var loanToDelete by remember { mutableStateOf<Loan?>(null) }

    // ─── Handle navigation events (e.g. after delete) ─────────────────────────
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is CustomerDetailUiEvent.NavigateBack -> onBackClick()
            }
        }
    }

    // ─── Snackbar ─────────────────────────────────────────────────────────────
    state.snackbarMessage?.let { message ->
        LaunchedEffect(message) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearSnackbar()
        }
    }

    // ─── Add Loan Dialog ──────────────────────────────────────────────────────
    if (showAddLoanDialog) {
        com.emicollect.app.ui.components.AddLoanDialog(
            onDismiss = { showAddLoanDialog = false },
            onConfirm = { itemName, price, downPayment, startDate, paymentMode ->
                viewModel.addLoan(itemName, price, downPayment, startDate, paymentMode)
                showAddLoanDialog = false
            }
        )
    }

    // ─── Payment Dialog ────────────────────────────────────────────────────────
    if (showPaymentDialog && selectedLoanId != null) {
        SmartPaymentDialog(
            defaultAmount = state.defaultCollectionAmount,
            currentBalance = selectedLoanBalance,
            onDismiss = { showPaymentDialog = false },
            onConfirm = { amount, mode, date ->
                if (selectedLoanId == -1L) {
                    viewModel.processTotalPayment(amount, mode, date)
                } else {
                    viewModel.processPayment(selectedLoanId!!, amount, mode, date)
                }
                showPaymentDialog = false
            }
        )
    }

    // ─── Edit Transaction Dialog ───────────────────────────────────────────────
    if (showEditDialog && selectedTransaction != null) {
        SmartPaymentDialog(
            defaultAmount = selectedTransaction!!.amountPaid,
            currentBalance = 0.0,
            initialDate = selectedTransaction!!.datePaid,
            initialMode = selectedTransaction!!.paymentMode,
            isEdit = true,
            onDismiss = { showEditDialog = false },
            onConfirm = { amount, mode, date ->
                viewModel.updateTransaction(selectedTransaction!!, amount, mode, date)
                showEditDialog = false
            }
        )
    }

    // ─── Edit Customer Dialog ─────────────────────────────────────────────────
    if (showEditCustomerDialog) {
        val customer = state.customerWithLoans?.customer
        EditCustomerDialog(
            initialName = customer?.name ?: "",
            initialPhone = customer?.phone ?: "",
            initialAddress = customer?.address ?: "",
            onDismiss = { showEditCustomerDialog = false },
            onConfirm = { name, phone, address ->
                viewModel.editCustomer(name, phone, address)
                showEditCustomerDialog = false
            }
        )
    }

    // ─── Delete Customer Confirmation ─────────────────────────────────────────
    if (showDeleteCustomerDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteCustomerDialog = false },
            title = { Text("Delete Customer", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will permanently delete this customer and all associated loans and payment history. This action cannot be undone.",
                    color = TextWhite.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteCustomerDialog = false
                        viewModel.deleteCustomer()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete Permanently", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCustomerDialog = false }) {
                    Text("Cancel", color = TextWhite.copy(alpha = 0.7f))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // ─── Delete Loan Confirmation ─────────────────────────────────────────────
    loanToDelete?.let { loan ->
        AlertDialog(
            onDismissRequest = { loanToDelete = null },
            title = { Text("Delete Loan", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Delete '${loan.itemName}'? All transaction history for this loan will also be removed.",
                    color = TextWhite.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteLoan(loan)
                        loanToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { loanToDelete = null }) {
                    Text("Cancel", color = TextWhite.copy(alpha = 0.7f))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // ─── Main Scaffold ────────────────────────────────────────────────────────
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(state.customerWithLoans?.customer?.name ?: "Customer Details", color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                actions = {
                    // Edit Customer
                    IconButton(onClick = { showEditCustomerDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Customer", tint = GoldAccent)
                    }
                    // Delete Customer
                    IconButton(onClick = { showDeleteCustomerDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Customer", tint = MaterialTheme.colorScheme.error)
                    }
                    // Share Statement
                    var showShareOptions by remember { mutableStateOf(false) }
                    IconButton(onClick = { showShareOptions = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Share Statement", tint = EmeraldPrimary)
                    }
                    if (showShareOptions) {
                        AlertDialog(
                            onDismissRequest = { showShareOptions = false },
                            title = { Text("Share Statement", color = TextWhite) },
                            text = { Text("Choose statement format:", color = TextWhite.copy(alpha = 0.8f)) },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        viewModel.shareStatement(context, isCombined = true)
                                        showShareOptions = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                ) { Text("Combined Statement", color = TextWhite) }
                            },
                            dismissButton = {
                                Button(
                                    onClick = {
                                        viewModel.shareStatement(context, isCombined = false)
                                        showShareOptions = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                                ) { Text("Itemized Receipt", color = MaterialTheme.colorScheme.onSecondary) }
                            },
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddLoanDialog = true },
                containerColor = EmeraldPrimary,
                contentColor = TextWhite
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Loan")
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GoldAccent)
            }
        } else {
            val customer = state.customerWithLoans?.customer
            val loans = state.customerWithLoans?.loans ?: emptyList()

            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ─ Customer Header & Total Balance ─
                item {
                    val totalOwed = loans.sumOf { it.currentBalance }
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Contact Info", style = MaterialTheme.typography.labelMedium, color = TextWhite.copy(alpha = 0.7f))
                                    Text(customer?.phone ?: "No Phone", style = MaterialTheme.typography.bodyLarge, color = TextWhite)
                                    Text(customer?.address ?: "No Address", style = MaterialTheme.typography.bodyMedium, color = TextWhite.copy(alpha = 0.8f))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Total Owed", style = MaterialTheme.typography.labelMedium, color = GoldAccent)
                                    Text(
                                        "₹${String.format("%.2f", totalOwed)}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldAccent
                                    )
                                }
                            }

                            if (totalOwed > 0) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        selectedLoanId = -1L
                                        selectedLoanBalance = totalOwed
                                        showPaymentDialog = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                ) {
                                    Text("Pay Toward Total (FIFO)", color = TextWhite, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // ─ Master History ─
                item {
                    var isExpanded by remember { mutableStateOf(false) }
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Master History", style = MaterialTheme.typography.titleMedium, color = GoldAccent, fontWeight = FontWeight.Bold)
                                TextButton(onClick = { isExpanded = !isExpanded }) {
                                    Text(if (isExpanded) "Hide" else "View All", color = GoldAccent)
                                }
                            }

                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (state.masterLedger.isEmpty()) {
                                        Text("No activity yet", color = TextWhite.copy(alpha = 0.5f), style = MaterialTheme.typography.bodyMedium)
                                    } else {
                                        val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
                                        state.masterLedger.forEach { item ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    val (text, color) = when (item) {
                                                        is LedgerItem.LoanCreated -> "New Loan: ${item.loan.itemName}" to com.emicollect.app.ui.theme.InfoBlue
                                                        is LedgerItem.TransactionItem -> {
                                                            val typeText = when (item.transaction.type) {
                                                                com.emicollect.app.data.local.entity.TransactionType.DOWN_PAYMENT -> "Down Payment"
                                                                com.emicollect.app.data.local.entity.TransactionType.PAYMENT -> "Payment"
                                                                com.emicollect.app.data.local.entity.TransactionType.LOAN_CREATED -> "Loan Created"
                                                            }
                                                            "$typeText (${item.itemName})" to if (item.transaction.type == com.emicollect.app.data.local.entity.TransactionType.PAYMENT) com.emicollect.app.ui.theme.EmeraldLight else androidx.compose.ui.graphics.Color.Gray
                                                        }
                                                    }
                                                    Text(text, color = color, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                    Text(dateFormat.format(Date(item.timestamp)), color = TextWhite.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                                                }
                                                val amountStr = when (item) {
                                                    is LedgerItem.LoanCreated -> "+₹${String.format("%.0f", item.loan.totalPrincipal)}"
                                                    is LedgerItem.TransactionItem -> "-₹${String.format("%.0f", item.transaction.amountPaid)}"
                                                }
                                                Text(amountStr, color = TextWhite, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                            }
                                            Divider(color = TextWhite.copy(alpha = 0.05f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ─ Active Loans Section Header ─
                item {
                    Text("Loans", style = MaterialTheme.typography.titleMedium, color = GoldAccent, fontWeight = FontWeight.Bold)
                }

                if (loans.isEmpty()) {
                    item {
                        Text("No loans yet. Tap + to add a loan.", color = TextWhite.copy(alpha = 0.5f))
                    }
                } else {
                    items(loans, key = { it.loanId }) { loan ->
                        LoanCard(
                            loan = loan,
                            customerName = customer?.name ?: "",
                            viewModel = viewModel,
                            onPayClick = {
                                selectedLoanId = loan.loanId
                                selectedLoanBalance = loan.currentBalance
                                showPaymentDialog = true
                            },
                            onDeleteClick = { loanToDelete = loan },
                            onEditTransaction = { txn ->
                                selectedTransaction = txn
                                showEditDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─── Loan Card ─────────────────────────────────────────────────────────────────
@Composable
private fun LoanCard(
    loan: Loan,
    customerName: String,
    viewModel: CustomerDetailViewModel,
    onPayClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditTransaction: (Transaction) -> Unit
) {
    val progress = if (loan.totalPrincipal > 0) {
        ((loan.totalPrincipal - loan.currentBalance) / loan.totalPrincipal).toFloat().coerceIn(0f, 1f)
    } else 1f

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ─ Header row: title + settle badge + delete ─
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(loan.itemName, style = MaterialTheme.typography.titleMedium, color = TextWhite, fontWeight = FontWeight.Bold)
                    if (loan.isClosed) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = EmeraldPrimary.copy(alpha = 0.2f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                "Settled",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!loan.isClosed) {
                        Text(
                            "Due: ₹${String.format("%.2f", loan.currentBalance)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Loan",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // ─ Progress Bar ─
            Spacer(modifier = Modifier.height(10.dp))
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Repaid", style = MaterialTheme.typography.labelSmall, color = TextWhite.copy(alpha = 0.5f))
                    Text(
                        "₹${String.format("%.0f", loan.totalPrincipal - loan.currentBalance)} / ₹${String.format("%.0f", loan.totalPrincipal)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextWhite.copy(alpha = 0.5f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = if (loan.isClosed) EmeraldPrimary else GoldAccent,
                    trackColor = TextWhite.copy(alpha = 0.1f)
                )
            }

            // ─ Pay Now button (only for open loans) ─
            if (!loan.isClosed) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onPayClick,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Pay Now", color = TextWhite)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = TextWhite.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(8.dp))

            Text("Recent Transactions", style = MaterialTheme.typography.labelMedium, color = TextWhite.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(8.dp))

            LoanTransactionsList(
                loanId = loan.loanId,
                viewModel = viewModel,
                customerName = customerName,
                onEditTransaction = onEditTransaction
            )
        }
    }
}

// ─── Edit Customer Dialog ──────────────────────────────────────────────────────
@Composable
private fun EditCustomerDialog(
    initialName: String,
    initialPhone: String,
    initialAddress: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var address by remember { mutableStateOf(initialAddress) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Customer", color = TextWhite, fontWeight = FontWeight.Bold) },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = editFieldColors()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    colors = editFieldColors()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = editFieldColors()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, phone, address) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) { Text("Save", color = TextWhite) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextWhite.copy(alpha = 0.7f)) }
        }
    )
}

@Composable
private fun editFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextWhite,
    unfocusedTextColor = TextWhite,
    focusedBorderColor = EmeraldPrimary,
    unfocusedBorderColor = TextWhite.copy(alpha = 0.3f),
    focusedLabelColor = EmeraldPrimary,
    unfocusedLabelColor = TextWhite.copy(alpha = 0.6f),
    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
    cursorColor = GoldAccent
)

// ─── Loan Transactions List ────────────────────────────────────────────────────
@Composable
fun LoanTransactionsList(
    loanId: Long,
    viewModel: CustomerDetailViewModel,
    customerName: String,
    onEditTransaction: (Transaction) -> Unit
) {
    val transactions by viewModel.getTransactionsForLoan(loanId).collectAsState(initial = emptyList())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (transactions.isEmpty()) {
        Text("No transactions yet", style = MaterialTheme.typography.bodySmall, color = TextWhite.copy(alpha = 0.5f))
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            transactions.take(5).forEach { transaction ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(transaction.datePaid)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextWhite
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "₹${String.format("%.2f", transaction.amountPaid)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = GoldAccent
                            )
                            Text(
                                text = "(${transaction.paymentMode})",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextWhite.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = { onEditTransaction(transaction) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Transaction",
                                tint = GoldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        val uri = ReceiptGenerator.generateReceipt(
                                            context = context,
                                            customerName = customerName,
                                            amount = transaction.amountPaid,
                                            transactionId = "TXN-${transaction.transactionId}"
                                        )
                                        if (uri != null) {
                                            ShareUtils.shareImage(context, uri)
                                        }
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Receipt",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
