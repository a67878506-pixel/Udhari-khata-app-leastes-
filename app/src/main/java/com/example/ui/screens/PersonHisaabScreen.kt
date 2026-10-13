package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonHisaabScreen(
    khataViewModel: KhataViewModel,
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val khataState by khataViewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val finColors = LocalFinancialColors.current

    val currentKhata = khataState.currentKhata ?: return
    val currentPerson = khataState.currentPerson ?: return
    val currentUserName = authState.userProfile?.name ?: "User"

    var showAddTxDialog by remember { mutableStateOf(false) }
    var selectedTxType by remember { mutableStateOf("LENA") } // LENA or DENA
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var deletingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var selectedTxTab by remember { mutableStateOf("ACTIVE") } // ACTIVE or TRASH
    var txToRecover by remember { mutableStateOf<Transaction?>(null) }
    var txToPermanentlyDelete by remember { mutableStateOf<Transaction?>(null) }

    var showEditPersonDialog by remember { mutableStateOf(false) }
    var editPersonName by remember { mutableStateOf(currentPerson.name) }
    var editPersonPhone by remember { mutableStateOf(currentPerson.phone) }
    var showDeletePersonDialog by remember { mutableStateOf(false) }
    var showPersonMenu by remember { mutableStateOf(false) }

    val transactions = when (val state = khataState.transactions) {
        is UiState.Success -> state.data.filter { !it.isDeleted }
        else -> emptyList()
    }

    val trashTransactions = when (val state = khataState.trashTransactions) {
        is UiState.Success -> state.data.filter { it.isDeleted }
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InitialAvatar(name = currentPerson.name, size = 38.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = currentPerson.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            if (currentPerson.phone.isNotEmpty()) {
                                Text(
                                    text = currentPerson.phone,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                )
                            }
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
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.navigationBarsPadding()
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
                        colors = ButtonDefaults.buttonColors(containerColor = finColors.lena),
                        shape = RoundedCornerShape(14.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("button_gave_lena")
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("MAINE DIYE", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color.White)
                            Text("(Aapko Lena Hai)", fontSize = 10.sp, color = Color.White.copy(alpha = 0.9f))
                        }
                    }

                    // Maine Liye (You Got / Dena) - Red
                    Button(
                        onClick = {
                            selectedTxType = "DENA"
                            editingTransaction = null
                            showAddTxDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = finColors.dena),
                        shape = RoundedCornerShape(14.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("button_got_dena")
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("MAINE LIYE", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color.White)
                            Text("(Aapko Dena Hai)", fontSize = 10.sp, color = Color.White.copy(alpha = 0.9f))
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
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, finColors.cardBorder.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val net = currentPerson.netBalance
                    val netLabel = if (net > 0) "Aapko Milenge (Lena Hai)" else if (net < 0) "Aapko Dene Hain (Dena Hai)" else "Hisaab Barabar (Settled)"
                    val netColor = if (net > 0) finColors.lena else if (net < 0) finColors.dena else MaterialTheme.colorScheme.onSurfaceVariant
                    val netBg = if (net > 0) finColors.lenaContainer else if (net < 0) finColors.denaContainer else MaterialTheme.colorScheme.surfaceVariant

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = netBg
                    ) {
                        Text(
                            text = netLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = netColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = formatCurrency(abs(net)),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = netColor,
                        fontSize = 32.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = finColors.cardBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Total Given (Maine Diye)",
                                style = MaterialTheme.typography.labelSmall,
                                color = finColors.onLenaContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formatCurrency(currentPerson.totalLena),
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleMedium,
                                color = finColors.lena
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Total Received (Maine Liye)",
                                style = MaterialTheme.typography.labelSmall,
                                color = finColors.onDenaContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formatCurrency(currentPerson.totalDena),
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleMedium,
                                color = finColors.dena
                            )
                        }
                    }
                }
            }

            // Transaction History Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedTxTab == "ACTIVE",
                    onClick = { selectedTxTab = "ACTIVE" },
                    label = { Text("Active (${transactions.size})") },
                    shape = RoundedCornerShape(10.dp)
                )
                FilterChip(
                    selected = selectedTxTab == "TRASH",
                    onClick = { selectedTxTab = "TRASH" },
                    label = { Text("Trash (${trashTransactions.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Transactions List
            when (if (selectedTxTab == "TRASH") khataState.trashTransactions else khataState.transactions) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is UiState.Error -> {
                    EmptyStateCard(
                        title = "Error loading transactions",
                        subtitle = "Unable to load transactions"
                    )
                }
                is UiState.Success -> {
                    val currentList = if (selectedTxTab == "TRASH") trashTransactions else transactions
                    if (currentList.isEmpty()) {
                        EmptyStateCard(
                            title = if (selectedTxTab == "TRASH") "Trash is Empty" else "No Transactions Yet",
                            subtitle = if (selectedTxTab == "TRASH") "Deleted transactions are stored here safely and can be recovered." else "Use the green and red buttons below to record when you give money or receive money from ${currentPerson.name}."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(currentList, key = { it.id }) { tx ->
                                if (selectedTxTab == "TRASH") {
                                    DeletedTransactionItemCard(
                                        transaction = tx,
                                        onRecover = { txToRecover = tx },
                                        onPermanentDelete = { txToPermanentlyDelete = tx }
                                    )
                                } else {
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

    // Move Transaction to Trash Dialog
    deletingTransaction?.let { tx ->
        AppConfirmationDialog(
            title = "Move Transaction to Trash?",
            message = "Move this transaction of ${formatCurrency(tx.amount)} to Trash? Balances will immediately recalculate without it, and you can recover it anytime.",
            confirmButtonText = "Move to Trash",
            onConfirm = {
                khataViewModel.softDeleteTransaction(
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

    // Recover Transaction Dialog
    txToRecover?.let { tx ->
        AppConfirmationDialog(
            title = "Recover Transaction?",
            message = "Restore this transaction of ${formatCurrency(tx.amount)}? The balance will immediately recalculate to include it.",
            confirmButtonText = "Recover",
            onConfirm = {
                khataViewModel.recoverTransaction(
                    khataId = currentKhata.id,
                    transactionId = tx.id,
                    personId = currentPerson.id,
                    personName = currentPerson.name,
                    amount = tx.amount,
                    performerName = currentUserName
                )
                txToRecover = null
            },
            onDismiss = { txToRecover = null }
        )
    }

    // Permanently Delete Transaction Dialog
    txToPermanentlyDelete?.let { tx ->
        AppConfirmationDialog(
            title = "Permanently Delete Transaction?",
            message = "Permanently delete this transaction of ${formatCurrency(tx.amount)}? This action CANNOT be undone.",
            confirmButtonText = "Delete Permanently",
            onConfirm = {
                khataViewModel.permanentlyDeleteTransaction(
                    khataId = currentKhata.id,
                    transactionId = tx.id,
                    personId = currentPerson.id,
                    personName = currentPerson.name,
                    amount = tx.amount,
                    performerName = currentUserName
                )
                txToPermanentlyDelete = null
            },
            onDismiss = { txToPermanentlyDelete = null }
        )
    }

    // Edit Person Dialog
    if (showEditPersonDialog) {
        AlertDialog(
            onDismissRequest = { showEditPersonDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Edit Person Details", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editPersonName,
                        onValueChange = { editPersonName = it },
                        label = { Text("Name *") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("edit_person_name_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editPersonPhone,
                        onValueChange = { editPersonPhone = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
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
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("save_edit_person_button")
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditPersonDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // Delete Person Confirmation Dialog
    if (showDeletePersonDialog) {
        AppConfirmationDialog(
            title = "Move Person to Trash?",
            message = "Move '${currentPerson.name}' to Trash? This person and their hisaab will disappear from active views, but records are safely preserved in Trash and can be recovered anytime.",
            confirmButtonText = "Move to Trash",
            onConfirm = {
                showDeletePersonDialog = false
                khataViewModel.softDeletePerson(
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
    val finColors = LocalFinancialColors.current
    val isLena = transaction.type == "LENA"
    val typeColor = if (isLena) finColors.lena else finColors.dena
    val typeBg = if (isLena) finColors.lenaContainer else finColors.denaContainer
    val typeText = if (isLena) "Maine Diye (Lena)" else "Maine Liye (Dena)"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_card_${transaction.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, finColors.cardBorder.copy(alpha = 0.45f)),
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
                        shape = RoundedCornerShape(8.dp),
                        color = typeBg
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isLena) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = typeColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = typeText,
                                color = typeColor,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
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
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = finColors.cardBorder.copy(alpha = 0.35f))
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
                        modifier = Modifier.size(32.dp).testTag("edit_tx_${transaction.id}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp).testTag("delete_tx_${transaction.id}")
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
    val finColors = LocalFinancialColors.current
    var amountText by remember { mutableStateOf(if (initialAmount > 0) initialAmount.toString() else "") }
    var txType by remember { mutableStateOf(initialType) }
    var note by remember { mutableStateOf(initialNote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = if (isEditing) "Edit Transaction" else "Add Transaction Entry",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
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
                        label = { Text("Maine Diye (Lena)", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = finColors.lenaContainer,
                            selectedLabelColor = finColors.lena
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("type_lena_chip")
                    )
                    FilterChip(
                        selected = txType == "DENA",
                        onClick = { txType = "DENA" },
                        label = { Text("Maine Liye (Dena)", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = finColors.denaContainer,
                            selectedLabelColor = finColors.dena
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("type_dena_chip")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amount input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹) *") },
                    placeholder = { Text("0.00") },
                    leadingIcon = {
                        Text(
                            text = "₹",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 14.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("tx_amount_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

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
                            shape = RoundedCornerShape(8.dp),
                            label = { Text("+₹$quickVal", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Note input
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Item details (Optional)") },
                    placeholder = { Text("e.g. Rice & Flour purchase, Bill payment") },
                    singleLine = false,
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("tx_note_input")
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
                shape = RoundedCornerShape(12.dp),
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
                modifier = Modifier.testTag("save_transaction_button")
            ) {
                Text(if (isEditing) "Update" else "Save Entry", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel", fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
fun DeletedTransactionItemCard(
    transaction: Transaction,
    onRecover: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    val finColors = LocalFinancialColors.current
    val isLena = transaction.type == "LENA"
    val typeColor = if (isLena) finColors.lena else finColors.dena
    val typeBg = if (isLena) finColors.lenaContainer else finColors.denaContainer
    val typeText = if (isLena) "Maine Diye (Lena)" else "Maine Liye (Dena)"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("deleted_transaction_card_${transaction.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = typeBg
                    ) {
                        Text(
                            text = typeText,
                            color = typeColor,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatDateTime(transaction.timestamp?.toDate()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${if (isLena) "+" else "-"} ${formatCurrency(transaction.amount)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = typeColor
                )
            }

            if (transaction.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = transaction.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "In Trash • Deleted by ${transaction.deletedBy ?: "User"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPermanentDelete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("perm_delete_tx_${transaction.id}")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete Permanently", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onRecover,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("recover_tx_${transaction.id}")
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Recover", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

