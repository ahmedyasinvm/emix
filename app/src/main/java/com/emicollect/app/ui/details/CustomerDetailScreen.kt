package com.emicollect.app.ui.details

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.emicollect.app.data.local.entity.Loan
import com.emicollect.app.data.local.entity.Transaction
import com.emicollect.app.ui.components.EmptyStateView
import com.emicollect.app.ui.components.GlassCard
import com.emicollect.app.ui.components.GlassCardElevated
import com.emicollect.app.ui.components.SmartPaymentDialog
import com.emicollect.app.ui.theme.*
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

    // ─── Dialog state ──────────────────────────────────────────
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showAddLoanDialog by remember { mutableStateOf(false) }
    var selectedLoanId by remember { mutableStateOf<Long?>(null) }
    var selectedLoanBalance by remember { mutableStateOf(0.0) }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showEditCustomerDialog by remember { mutableStateOf(false) }
    var showDeleteCustomerDialog by remember { mutableStateOf(false) }
    var loanToDelete by remember { mutableStateOf<Loan?>(null) }

    // ─── Handle navigation events ──────────────────────────────
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is CustomerDetailUiEvent.NavigateBack -> onBackClick()
            }
        }
    }

    // ─── Snackbar ──────────────────────────────────────────────
    state.snackbarMessage?.let { message ->
        LaunchedEffect(message) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearSnackbar()
        }
    }

    // ─── Add Loan Dialog ───────────────────────────────────────
    if (showAddLoanDialog) {
        com.emicollect.app.ui.components.AddLoanDialog(
            onDismiss = { showAddLoanDialog = false },
            onConfirm = { itemName, price, downPayment, startDate, paymentMode ->
                viewModel.addLoan(itemName, price, downPayment, startDate, paymentMode)
                showAddLoanDialog = false
            }
        )
    }

    // ─── Payment Dialog ────────────────────────────────────────
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

    // ─── Edit Transaction Dialog ───────────────────────────────
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

    // ─── Edit Customer Dialog ──────────────────────────────────
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

    // ─── Delete Customer Confirmation ──────────────────────────
    if (showDeleteCustomerDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteCustomerDialog = false },
            icon = {
                Icon(
                    Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Delete Customer",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "This will permanently delete this customer and all associated loans and payment history. This action cannot be undone.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteCustomerDialog = false
                        viewModel.deleteCustomer()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Delete Permanently", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCustomerDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // ─── Delete Loan Confirmation ──────────────────────────────
    loanToDelete?.let { loan ->
        AlertDialog(
            onDismissRequest = { loanToDelete = null },
            title = {
                Text(
                    "Delete Loan",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Delete '${loan.itemName}'? All transaction history for this loan will also be removed.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteLoan(loan)
                        loanToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Delete", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { loanToDelete = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // ═══ Main Scaffold ═══
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.customerWithLoans?.customer?.name ?: "Customer Details",
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    IconButton(onClick = { showEditCustomerDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Customer", tint = GoldAccent, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { showDeleteCustomerDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Customer", tint = ErrorRed.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                    }
                    // Share Statement
                    var showShareSheet by remember { mutableStateOf(false) }
                    IconButton(onClick = { showShareSheet = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Share Statement", tint = EmeraldLight, modifier = Modifier.size(20.dp))
                    }
                    if (showShareSheet) {
                        ShareStatementBottomSheet(
                            loans = state.customerWithLoans?.loans ?: emptyList(),
                            onDismiss = { showShareSheet = false },
                            onShare = { isCombined, selectedLoanId, start, end ->
                                viewModel.shareStatement(context, isCombined, selectedLoanId, start, end)
                                showShareSheet = false
                            }
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
                contentColor = TextWhite,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Loan")
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EmeraldLight, strokeWidth = 3.dp)
            }
        } else {
            val customer = state.customerWithLoans?.customer
            val loans = state.customerWithLoans?.loans ?: emptyList()

            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                // ═══ Profile Header Card ═══
                item {
                    val totalOwed = loans.sumOf { it.currentBalance }

                    GlassCardElevated(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Avatar
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(GradientEmeraldStart, GradientEmeraldEnd)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (customer?.name ?: "?").take(2).uppercase(),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp
                                        ),
                                        color = Color.White
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        customer?.phone ?: "No Phone",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        customer?.address ?: "No Address",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Total Owed
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "Total Owed",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GoldAccent.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        "₹${String.format("%.0f", totalOwed)}",
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
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Pay Toward Total (FIFO)", color = TextWhite, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // ═══ Master History ═══
                item {
                    var isExpanded by remember { mutableStateOf(false) }
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        "Master History",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = GoldAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                TextButton(onClick = { isExpanded = !isExpanded }) {
                                    Text(
                                        if (isExpanded) "Hide" else "View All",
                                        color = GoldAccent,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }

                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    if (state.masterLedger.isEmpty()) {
                                        Text(
                                            "No activity yet",
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            style = MaterialTheme.typography.bodySmall
                                        )
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
                                                        is LedgerItem.LoanCreated -> "New Loan: ${item.loan.itemName}" to InfoBlue
                                                        is LedgerItem.TransactionItem -> {
                                                            val typeText = when (item.transaction.type) {
                                                                com.emicollect.app.data.local.entity.TransactionType.DOWN_PAYMENT -> "Down Payment"
                                                                com.emicollect.app.data.local.entity.TransactionType.PAYMENT -> "Payment"
                                                                com.emicollect.app.data.local.entity.TransactionType.LOAN_CREATED -> "Loan Created"
                                                            }
                                                            "$typeText (${item.itemName})" to if (item.transaction.type == com.emicollect.app.data.local.entity.TransactionType.PAYMENT) EmeraldLight else SlateLight
                                                        }
                                                    }
                                                    Text(text, color = color, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                                    Text(dateFormat.format(Date(item.timestamp)), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), style = MaterialTheme.typography.labelSmall)
                                                }
                                                val amountStr = when (item) {
                                                    is LedgerItem.LoanCreated -> "+₹${String.format("%.0f", item.loan.totalPrincipal)}"
                                                    is LedgerItem.TransactionItem -> "-₹${String.format("%.0f", item.transaction.amountPaid)}"
                                                }
                                                Text(amountStr, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                            }
                                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ═══ Loans Section Header ═══
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(18.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Brush.verticalGradient(listOf(GoldAccent, GoldDeep)))
                        )
                        Text(
                            "Loans",
                            style = MaterialTheme.typography.titleSmall,
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${loans.size} ${if (loans.size == 1) "loan" else "loans"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }

                if (loans.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Default.CreditCard,
                            title = "No loans yet",
                            subtitle = "Tap + to add a loan for this customer",
                        )
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

                // Bottom spacer
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }
}

// ═══ Loan Card ═══
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            loan.itemName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        if (loan.isClosed) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SuccessGreenTint)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "Settled",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SuccessGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!loan.isClosed) {
                        Text(
                            "₹${String.format("%.0f", loan.currentBalance)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = ErrorRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Loan",
                            tint = ErrorRed.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Progress Bar
            Spacer(modifier = Modifier.height(12.dp))
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${(progress * 100).toInt()}% Repaid",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        "₹${String.format("%.0f", loan.totalPrincipal - loan.currentBalance)} / ₹${String.format("%.0f", loan.totalPrincipal)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (loan.isClosed) SuccessGreen else EmeraldLight,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )
            }

            // Pay Now
            if (!loan.isClosed) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onPayClick,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Pay Now", color = TextWhite, style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
                Text(
                    "Recent Transactions",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            LoanTransactionsList(
                loanId = loan.loanId,
                itemName = loan.itemName,
                viewModel = viewModel,
                customerName = customerName,
                onEditTransaction = onEditTransaction
            )
        }
    }
}

// ═══ Edit Customer Dialog ═══
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
        title = {
            Text(
                "Edit Customer",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = editFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    colors = editFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(18.dp)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = editFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, phone, address) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Save", color = TextWhite) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
private fun editFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedBorderColor = EmeraldPrimary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor = EmeraldLight,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    cursorColor = GoldAccent
)

// ═══ Loan Transactions List ═══
@Composable
fun LoanTransactionsList(
    loanId: Long,
    itemName: String,
    viewModel: CustomerDetailViewModel,
    customerName: String,
    onEditTransaction: (Transaction) -> Unit
) {
    val transactions by viewModel.getTransactionsForLoan(loanId).collectAsState(initial = emptyList())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (transactions.isEmpty()) {
        Text(
            "No transactions yet",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            transactions.take(5).forEach { transaction ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(transaction.datePaid)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "₹${String.format("%.2f", transaction.amountPaid)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = GoldAccent
                            )
                            // Payment mode chip
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (transaction.paymentMode == "GPay")
                                            InfoBlueTint else GlassHighlight
                                    )
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = transaction.paymentMode,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = if (transaction.paymentMode == "GPay")
                                        InfoBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }

                    Row {
                        IconButton(onClick = { onEditTransaction(transaction) }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = GoldAccent.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
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
                                            transactionId = "TXN-${transaction.transactionId}",
                                            itemName = itemName
                                        )
                                        if (uri != null) {
                                            ShareUtils.shareImage(context, uri)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Receipt",
                                tint = EmeraldLight.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareStatementBottomSheet(
    loans: List<com.emicollect.app.data.local.entity.Loan>,
    onDismiss: () -> Unit,
    onShare: (isCombined: Boolean, selectedLoanId: Long?, start: Long?, end: Long?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedRange by remember { mutableStateOf("All Time") }
    var selectedType by remember { mutableStateOf("Combined") }
    var selectedLoanId by remember { mutableStateOf<Long?>(loans.firstOrNull()?.loanId) }
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    customStartDate = datePickerState.selectedStartDateMillis
                    customEndDate = datePickerState.selectedEndDateMillis
                    showDatePicker = false
                }) { Text("Apply", color = EmeraldPrimary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurface) }
            },
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            DateRangePicker(
                state = datePickerState,
                title = { Text("Select Statement Period", modifier = Modifier.padding(16.dp)) },
                modifier = Modifier.fillMaxHeight(0.8f)
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Generate Statement", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

            // Date Range
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Period", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.7f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("All Time", "Last 30 Days", "Custom").forEach { range ->
                        FilterChip(
                            selected = selectedRange == range,
                            onClick = { 
                                selectedRange = range
                                if (range == "Custom") showDatePicker = true 
                            },
                            label = { Text(range) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary.copy(alpha=0.2f),
                                selectedLabelColor = EmeraldLight,
                                containerColor = Color.Transparent,
                                labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha=0.7f)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = MaterialTheme.colorScheme.outline,
                                selectedBorderColor = EmeraldPrimary
                            )
                        )
                    }
                }
                if (selectedRange == "Custom" && customStartDate != null) {
                    val df = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                    Text(
                        "${df.format(java.util.Date(customStartDate!!))} - ${df.format(java.util.Date(customEndDate ?: customStartDate!!))}",
                        style = MaterialTheme.typography.bodySmall, color = EmeraldLight
                    )
                }
            }

            // Statement Type
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Format", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.7f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedType == "Combined",
                        onClick = { selectedType = "Combined" },
                        label = { Text("Combined (All Loans)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary.copy(alpha=0.2f),
                            selectedLabelColor = EmeraldLight,
                            containerColor = Color.Transparent,
                            labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha=0.7f)
                        ),
                        border = FilterChipDefaults.filterChipBorder(borderColor = MaterialTheme.colorScheme.outline, selectedBorderColor = EmeraldPrimary)
                    )
                    FilterChip(
                        selected = selectedType == "Itemized",
                        onClick = { selectedType = "Itemized" },
                        label = { Text("Itemized (Specific Loan)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary.copy(alpha=0.2f),
                            selectedLabelColor = EmeraldLight,
                            containerColor = Color.Transparent,
                            labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha=0.7f)
                        ),
                        border = FilterChipDefaults.filterChipBorder(borderColor = MaterialTheme.colorScheme.outline, selectedBorderColor = EmeraldPrimary)
                    )
                }
            }

            // Loan Selector
            if (selectedType == "Itemized" && loans.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select Loan", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.7f))
                    loans.forEach { loan ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { selectedLoanId = loan.loanId }.padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedLoanId == loan.loanId,
                                onClick = { selectedLoanId = loan.loanId },
                                colors = RadioButtonDefaults.colors(selectedColor = EmeraldPrimary, unselectedColor = MaterialTheme.colorScheme.outline)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(loan.itemName, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action
            Button(
                onClick = {
                    val (start, end) = when (selectedRange) {
                        "Last 30 Days" -> {
                            val cal = java.util.Calendar.getInstance()
                            val tEnd = cal.timeInMillis
                            cal.add(java.util.Calendar.DAY_OF_YEAR, -30)
                            cal.timeInMillis to tEnd
                        }
                        "Custom" -> customStartDate to (customEndDate ?: customStartDate)
                        else -> null to null
                    }
                    onShare(selectedType == "Combined", selectedLoanId, start, end)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(EmeraldPrimary, EmeraldLight))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Generate & Share", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
