package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Transaction
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.KhataViewModel
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonHisaabScreen(
    khataViewModel: KhataViewModel,
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val khataState by khataViewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()

    val currentKhata = khataState.currentKhata ?: return
    val currentPerson = khataState.currentPerson ?: return
    val currentUserName = authState.userProfile?.name ?: "User"

    var showAddTxDialog by remember { mutableStateOf(false) }
    var selectedTxType by remember { mutableStateOf("LENA") } // LENA or DENA
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var deletingTransaction by remember { mutableStateOf<Transaction?>(null) }

    var showEditPersonDialog by remember { mutableStateOf(false) }
    var editPersonName by remember { mutableStateOf(currentPerson.name) }
    var editPersonPhone by remember { mutableStateOf(currentPerson.phone) }
    var showDeletePersonDialog by remember { mutableStateOf(false) }
    var showPersonMenu by remember { mutableStateOf(false) }

    val transactions = when (val state = khataState.transactions) {
        is UiState.Success -> state.data
        else -> emptyList()
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(khataState.actionSuccessMessage, khataState.actionErrorMessage) {
        khataState.actionSuccessMessage?.let {
            snackbarHostState.showSnackbar(it)
            khataViewModel.clearActionMessages()
        }
        khataState.actionErrorMessage?.let {
            snackbarHostState.showSnackbar(it)
            khataViewModel.clearActionMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(currentPerson.name, fontWeight = FontWeight.Bold)
                        if (currentPerson.phone.isNotEmpty()) {
                            Text(
                                text = currentPerson.phone,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("person_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showPersonMenu = true }, modifier = Modifier.testTag("person_options_button")) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = showPersonMenu,
                        onDismissRequest = { showPersonMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Person Details") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showPersonMenu = false
                                editPersonName = currentPerson.name
                                editPersonPhone = currentPerson.phone
                                showEditPersonDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Person", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showPersonMenu = false
                                showDeletePersonDialog = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            // Prominent Quick Action Buttons
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Maine Diye (You Gave / Lena) - Green
                    Button(
                        onClick = {
                            selectedTxType = "LENA"
                            editingTransaction = null
                            showAddTxDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenLena),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("button_gave_lena")
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("MAINE DIYE", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color.White)
                            Text("(Lena Hai)", fontSize = 11.sp, color = Color.White.copy(alpha = 0.9f))
                        }
                    }

                    // Maine Liye (You Got / Dena) - Red
                    Button(
                        onClick = {
                            selectedTxType = "DENA"
                            editingTransaction = null
                            showAddTxDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedDena),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("button_got_dena")
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("MAINE LIYE", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color.White)
                            Text("(Dena Hai)", fontSize = 11.sp, color = Color.White.copy(alpha = 0.9f))
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Balance Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val net = currentPerson.netBalance
                    val netLabel = if (net > 0) "You will get (Lena Hai)" else if (net < 0) "You will give (Dena Hai)" else "Settled (Hisaab Chukta)"
                    val netColor = if (net > 0) GreenLena else if (net < 0) RedDena else Color.Gray

                    Text(
                        text = netLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = netColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatCurrency(kotlin.math.abs(net)),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = netColor
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Given (Lena)", style = MaterialTheme.typography.labelSmall, color = OnGreenLenaContainer)
                            Text(formatCurrency(currentPerson.totalLena), fontWeight = FontWeight.Bold, color = GreenLena)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Received (Dena)", style = MaterialTheme.typography.labelSmall, color = OnRedDenaContainer)
                            Text(formatCurrency(currentPerson.totalDena), fontWeight = FontWeight.Bold, color = RedDena)
                        }
                    }
                }
            }

            // Transaction History Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TRANSACTION HISTORY (${transactions.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Transactions List
            when (khataState.transactions) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    EmptyStateCard(
                        title = "Error loading transactions",
                        subtitle = (khataState.transactions as UiState.Error).message
                    )
                }
                is UiState.Success -> {
                    if (transactions.isEmpty()) {
                        EmptyStateCard(
                            title = "No Transactions Yet",
                            subtitle = "Use the buttons below to record when you give money or receive money from ${currentPerson.name}."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(transactions, key = { it.id }) { tx ->
                                TransactionItemCard(
                                    transaction = tx,
                                    onEdit = { editingTransaction = tx },
                                    onDelete = { deletingTransaction = tx }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add or Edit Transaction Dialog
    if (showAddTxDialog || editingTransaction != null) {
        val txToEdit = editingTransaction
        TransactionInputDialog(
            initialAmount = txToEdit?.amount ?: 0.0,
            initialType = txToEdit?.type ?: selectedTxType,
            initialNote = txToEdit?.note ?: "",
            initialDateMillis = txToEdit?.timestamp?.toDate()?.time ?: System.currentTimeMillis(),
            isEditing = txToEdit != null,
            onDismiss = {
                showAddTxDialog = false
                editingTransaction = null
            },
            onSave = { amount, type, note, dateMillis ->
                if (txToEdit != null) {
                    khataViewModel.updateTransaction(
                        khataId = currentKhata.id,
                        transactionId = txToEdit.id,
                        personId = currentPerson.id,
                        personName = currentPerson.name,
                        newAmount = amount,
                        newType = type,
                        newNote = note,
                        newDateMillis = dateMillis,
                        performerName = currentUserName,
                        onSuccess = { editingTransaction = null }
                    )
                } else {
                    khataViewModel.addTransaction(
                        khataId = currentKhata.id,
                        personId = currentPerson.id,
                        personName = currentPerson.name,
                        amount = amount,
                        type = type,
                        note = note,
                        dateMillis = dateMillis,
                        performerName = currentUserName,
                        onSuccess = { showAddTxDialog = false }
                    )
                }
            }
        )
    }

    // Delete Transaction Dialog
    deletingTransaction?.let { tx ->
        AppConfirmationDialog(
            title = "Delete Transaction?",
            message = "Are you sure you want to delete this transaction of ${formatCurrency(tx.amount)}? The balances will automatically update.",
            confirmButtonText = "Delete",
            onConfirm = {
                khataViewModel.deleteTransaction(
                    khataId = currentKhata.id,
                    transactionId = tx.id,
                    personId = currentPerson.id,
                    personName = currentPerson.name,
                    amount = tx.amount,
                    performerName = currentUserName
                )
                deletingTransaction = null
            },
            onDismiss = { deletingTransaction = null }
        )
    }

    // Edit Person Dialog
    if (showEditPersonDialog) {
        AlertDialog(
            onDismissRequest = { showEditPersonDialog = false },
            title = { Text("Edit Person Details") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editPersonName,
                        onValueChange = { editPersonName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_person_name_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editPersonPhone,
                        onValueChange = { editPersonPhone = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_person_phone_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editPersonName.isNotBlank()) {
                            showEditPersonDialog = false
                            khataViewModel.updatePerson(
                                khataId = currentKhata.id,
                                personId = currentPerson.id,
                                name = editPersonName.trim(),
                                phone = editPersonPhone.trim(),
                                performerName = currentUserName,
                                onSuccess = {}
                            )
                        }
                    },
                    modifier = Modifier.testTag("save_edit_person_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPersonDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Person Confirmation Dialog
    if (showDeletePersonDialog) {
        AppConfirmationDialog(
            title = "Delete Person?",
            message = "Are you sure you want to delete '${currentPerson.name}'? This will permanently delete all ${transactions.size} transactions for this person.",
            confirmButtonText = "Delete Person",
            onConfirm = {
                showDeletePersonDialog = false
                khataViewModel.deletePerson(
                    khataId = currentKhata.id,
                    personId = currentPerson.id,
                    personName = currentPerson.name,
                    performerName = currentUserName,
                    onDeleted = onBack
                )
            },
            onDismiss = { showDeletePersonDialog = false }
        )
    }
}

@Composable
fun TransactionItemCard(
    transaction: Transaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isLena = transaction.type == "LENA"
    val typeColor = if (isLena) GreenLena else RedDena
    val typeBg = if (isLena) GreenLenaContainer else RedDenaContainer
    val typeText = if (isLena) "Maine Diye (Lena)" else "Maine Liye (Dena)"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_card_${transaction.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type badge & Date
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = typeBg
                    ) {
                        Text(
                            text = typeText,
                            color = typeColor,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatDateTime(transaction.timestamp?.toDate()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Amount
                Text(
                    text = "${if (isLena) "+" else "-"} ${formatCurrency(transaction.amount)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = typeColor
                )
            }

            if (transaction.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = transaction.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(6.dp))

            // Footer: Performed by user & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Recorded by: ${transaction.createdByName.ifBlank { "User" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp).testTag("edit_tx_${transaction.id}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp).testTag("delete_tx_${transaction.id}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionInputDialog(
    initialAmount: Double,
    initialType: String,
    initialNote: String,
    initialDateMillis: Long,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onSave: (amount: Double, type: String, note: String, dateMillis: Long) -> Unit
) {
    var amountText by remember { mutableStateOf(if (initialAmount > 0) initialAmount.toString() else "") }
    var txType by remember { mutableStateOf(initialType) }
    var note by remember { mutableStateOf(initialNote) }
    var isQuickIncrease by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEditing) "Edit Transaction" else "Add Transaction", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                // Type selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = txType == "LENA",
                        onClick = { txType = "LENA" },
                        label = { Text("Maine Diye (Lena)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenLenaContainer,
                            selectedLabelColor = GreenLena
                        ),
                        modifier = Modifier.weight(1f).testTag("type_lena_chip")
                    )
                    FilterChip(
                        selected = txType == "DENA",
                        onClick = { txType = "DENA" },
                        label = { Text("Maine Liye (Dena)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RedDenaContainer,
                            selectedLabelColor = RedDena
                        ),
                        modifier = Modifier.weight(1f).testTag("type_dena_chip")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Amount input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹) *") },
                    placeholder = { Text("0.00") },
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(start = 12.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("tx_amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick amount chips (+100, +500, +1000, +2000)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(100, 500, 1000, 2000).forEach { quickVal ->
                        SuggestionChip(
                            onClick = {
                                val cur = amountText.toDoubleOrNull() ?: 0.0
                                amountText = (cur + quickVal).toString()
                            },
                            label = { Text("+₹$quickVal", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Note input
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Item details (Optional)") },
                    placeholder = { Text("e.g. Rice & Flour purchase, Bill payment") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("tx_note_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt != null && amt > 0) {
                        onSave(amt, txType, note.trim(), initialDateMillis)
                    }
                },
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
                modifier = Modifier.testTag("save_transaction_button")
            ) {
                Text(if (isEditing) "Update" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
