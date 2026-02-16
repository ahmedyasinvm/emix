package com.emicollect.app.ui.details

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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

    var showPaymentDialog by remember { mutableStateOf(false) }
    var showAddLoanDialog by remember { mutableStateOf(false) }
    var selectedLoanId by remember { mutableStateOf<Long?>(null) }
    var selectedLoanBalance by remember { mutableStateOf(0.0) }

    // Handle Snackbar
    state.snackbarMessage?.let { message ->
        LaunchedEffect(message) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearSnackbar()
        }
    }

    if (showAddLoanDialog) {
        com.emicollect.app.ui.components.AddLoanDialog(
            onDismiss = { showAddLoanDialog = false },
            onConfirm = { itemName, price, downPayment ->
                viewModel.addLoan(itemName, price, downPayment)
                showAddLoanDialog = false
            }
        )
    }

    if (showPaymentDialog && selectedLoanId != null) {
        SmartPaymentDialog(
            defaultAmount = state.defaultCollectionAmount,
            currentBalance = selectedLoanBalance,
            onDismiss = { showPaymentDialog = false },
            onConfirm = { amount, _ -> // Payment mode support can be added to ViewModel if needed
                viewModel.processPayment(selectedLoanId!!, amount)
                showPaymentDialog = false
            }
        )
    }

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
                // Customer Header
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Contact Info", style = MaterialTheme.typography.labelMedium, color = TextWhite.copy(alpha = 0.7f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(customer?.phone ?: "No Phone", style = MaterialTheme.typography.bodyLarge, color = TextWhite)
                            Text(customer?.address ?: "No Address", style = MaterialTheme.typography.bodyMedium, color = TextWhite.copy(alpha = 0.8f))
                        }
                    }
                }

                // Active Loans Section
                item {
                    Text("Active Loans", style = MaterialTheme.typography.titleMedium, color = GoldAccent, fontWeight = FontWeight.Bold)
                }

                if (loans.isEmpty()) {
                    item {
                        Text("No active loans", color = TextWhite.copy(alpha = 0.5f))
                    }
                } else {
                    items(loans) { loan ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(loan.itemName, style = MaterialTheme.typography.titleMedium, color = TextWhite, fontWeight = FontWeight.Bold)
                                    Text("Due: ₹${String.format("%.2f", loan.currentBalance)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        selectedLoanId = loan.loanId
                                        selectedLoanBalance = loan.currentBalance
                                        showPaymentDialog = true
                                    },
                                    modifier = Modifier.align(Alignment.End),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                ) {
                                    Text("Pay Now", color = TextWhite)
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                Divider(color = TextWhite.copy(alpha = 0.1f))
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // History Section for this Loan
                                Text("Recent Transactions", style = MaterialTheme.typography.labelMedium, color = TextWhite.copy(alpha = 0.7f))
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Using a SideEffect or State to load transactions would be better, 
                                // but for now we'll fetch them via a Composable that observes the Flow.
                                LoanTransactionsList(loanId = loan.loanId, viewModel = viewModel, customerName = customer?.name ?: "")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoanTransactionsList(
    loanId: Long, 
    viewModel: CustomerDetailViewModel,
    customerName: String
) {
    val transactions by viewModel.getTransactionsForLoan(loanId).collectAsState(initial = emptyList())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (transactions.isEmpty()) {
        Text("No transactions yet", style = MaterialTheme.typography.bodySmall, color = TextWhite.copy(alpha = 0.5f))
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            transactions.take(3).forEach { transaction ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(transaction.datePaid)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextWhite
                        )
                        Text(
                            text = "₹${String.format("%.2f", transaction.amountPaid)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GoldAccent
                        )
                    }
                    
                    IconButton(
                        onClick = {
                            scope.launch {
                                // Generate and Share Receipt
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
