package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Khata
import com.example.data.model.Person
import com.example.data.model.Transaction
import com.example.ui.components.AppConfirmationDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.InitialAvatar
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDateTime
import com.example.ui.theme.LocalFinancialColors
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.KhataViewModel
import com.example.ui.viewmodel.UiState
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    khataViewModel: KhataViewModel,
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val khataState by khataViewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()

    val currentUserName = authState.userProfile?.name ?: "User"

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Khatas, 1: People, 2: Transactions
    var khataToRecover by remember { mutableStateOf<Khata?>(null) }
    var khataToPermanentlyDelete by remember { mutableStateOf<Khata?>(null) }
    var personToRecover by remember { mutableStateOf<Person?>(null) }
    var personToPermanentlyDelete by remember { mutableStateOf<Person?>(null) }
    var txToRecover by remember { mutableStateOf<Transaction?>(null) }
    var txToPermanentlyDelete by remember { mutableStateOf<Transaction?>(null) }

    val activeKhatas = when (val state = khataState.khatas) {
        is UiState.Success -> state.data.filter { !it.isDeleted }
        else -> emptyList()
    }

    val currentKhata = khataState.currentKhata ?: activeKhatas.firstOrNull()

    LaunchedEffect(currentKhata?.id) {
        currentKhata?.let {
            khataViewModel.observeTrashPeopleForKhata(it.id)
            khataViewModel.observeTrashTransactionsForKhata(it.id)
        }
    }

    val trashKhatas = when (val state = khataState.trashKhatas) {
        is UiState.Success -> state.data.filter { it.isDeleted }
        else -> emptyList()
    }

    val trashPeople = when (val state = khataState.trashPeople) {
        is UiState.Success -> state.data.filter { it.isDeleted }
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
                    Column {
                        Text("Trash / Recently Deleted", fontWeight = FontWeight.Bold)
                        Text(
                            text = "${trashKhatas.size} Khatas • ${trashPeople.size} People • ${trashTransactions.size} Hisaab",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("trash_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Notice Card
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Deleted items remain preserved with full security. You can recover them anytime or delete them permanently.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 3-Level Trash Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Khatas (${trashKhatas.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("People (${trashPeople.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Hisaab (${trashTransactions.size})", fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (selectedTab) {
                0 -> {
                    // TAB 0: KHATAS TRASH
                    when (khataState.trashKhatas) {
                        is UiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        is UiState.Error -> {
                            EmptyStateCard(
                                title = "Unable to load trash",
                                subtitle = (khataState.trashKhatas as UiState.Error).message,
                                actionButton = {
                                    Button(onClick = { khataViewModel.observeTrashKhatas() }) {
                                        Text("Retry")
                                    }
                                }
                            )
                        }
                        is UiState.Success -> {
                            if (trashKhatas.isEmpty()) {
                                EmptyStateCard(
                                    title = "No Deleted Khatas",
                                    subtitle = "You have no deleted Khatas. Khatas you move to Trash will appear here and can be recovered."
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(bottom = 32.dp)
                                ) {
                                    items(trashKhatas, key = { it.id }) { khata ->
                                        TrashKhataCard(
                                            khata = khata,
                                            onRecover = { khataToRecover = khata },
                                            onPermanentDelete = { khataToPermanentlyDelete = khata }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // TAB 1: PEOPLE TRASH
                    when (khataState.trashPeople) {
                        is UiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        is UiState.Error -> {
                            EmptyStateCard(
                                title = "Unable to load deleted people",
                                subtitle = (khataState.trashPeople as UiState.Error).message
                            )
                        }
                        is UiState.Success -> {
                            if (trashPeople.isEmpty()) {
                                EmptyStateCard(
                                    title = "No Deleted People",
                                    subtitle = "People you move to Trash will appear here so you can recover them or permanently delete them."
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(bottom = 32.dp)
                                ) {
                                    items(trashPeople, key = { it.id }) { person ->
                                        DeletedPersonCard(
                                            person = person,
                                            onRecover = { personToRecover = person },
                                            onPermanentDelete = { personToPermanentlyDelete = person }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // TAB 2: TRANSACTIONS TRASH
                    when (khataState.trashTransactions) {
                        is UiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        is UiState.Error -> {
                            EmptyStateCard(
                                title = "Unable to load deleted hisaab",
                                subtitle = (khataState.trashTransactions as UiState.Error).message
                            )
                        }
                        is UiState.Success -> {
                            if (trashTransactions.isEmpty()) {
                                EmptyStateCard(
                                    title = "No Deleted Transactions",
                                    subtitle = "Transactions moved to Trash will appear here. Recovering them restores their balance effect immediately."
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(bottom = 32.dp)
                                ) {
                                    items(trashTransactions, key = { it.id }) { tx ->
                                        DeletedTransactionItemCard(
                                            transaction = tx,
                                            onRecover = { txToRecover = tx },
                                            onPermanentDelete = { txToPermanentlyDelete = tx }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Recover Khata Dialog
    khataToRecover?.let { khata ->
        AppConfirmationDialog(
            title = "Recover Khata?",
            message = "Do you want to restore '${khata.name}'? It will immediately return to your Home Khatas list with all people and transactions intact.",
            confirmButtonText = "Recover",
            isDestructive = false,
            onConfirm = {
                khataViewModel.recoverKhata(khata.id, currentUserName)
                khataToRecover = null
            },
            onDismiss = { khataToRecover = null }
        )
    }

    // Permanently Delete Khata Dialog
    khataToPermanentlyDelete?.let { khata ->
        AppConfirmationDialog(
            title = "Permanently Delete '${khata.name}'?",
            message = "WARNING: This action is permanent and IRREVERSIBLE. All associated people, transactions, and history will be completely erased. Are you sure you want to proceed?",
            confirmButtonText = "Permanently Delete",
            isDestructive = true,
            onConfirm = {
                khataViewModel.permanentlyDeleteKhata(khata.id)
                khataToPermanentlyDelete = null
            },
            onDismiss = { khataToPermanentlyDelete = null }
        )
    }

    // Recover Person Dialog
    personToRecover?.let { p ->
        AppConfirmationDialog(
            title = "Recover Person?",
            message = "Do you want to restore '${p.name}'? All hisaab records and balances will be restored.",
            confirmButtonText = "Recover",
            isDestructive = false,
            onConfirm = {
                val khataId = p.khataId.ifBlank { currentKhata?.id ?: "" }
                khataViewModel.recoverPerson(khataId, p.id, p.name, currentUserName)
                personToRecover = null
            },
            onDismiss = { personToRecover = null }
        )
    }

    // Permanently Delete Person Dialog
    personToPermanentlyDelete?.let { p ->
        AppConfirmationDialog(
            title = "Permanently Delete Person?",
            message = "Permanently delete '${p.name}' and all associated transactions? This action CANNOT be undone.",
            confirmButtonText = "Delete Permanently",
            isDestructive = true,
            onConfirm = {
                val khataId = p.khataId.ifBlank { currentKhata?.id ?: "" }
                khataViewModel.permanentlyDeletePerson(khataId, p.id, p.name, currentUserName)
                personToPermanentlyDelete = null
            },
            onDismiss = { personToPermanentlyDelete = null }
        )
    }

    // Recover Transaction Dialog
    txToRecover?.let { tx ->
        AppConfirmationDialog(
            title = "Recover Transaction?",
            message = "Restore this transaction of ${formatCurrency(tx.amount)}? The balance will immediately recalculate to include it.",
            confirmButtonText = "Recover",
            isDestructive = false,
            onConfirm = {
                val khataId = tx.khataId.ifBlank { currentKhata?.id ?: "" }
                khataViewModel.recoverTransaction(
                    khataId = khataId,
                    transactionId = tx.id,
                    personId = tx.personId,
                    personName = tx.personName,
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
            isDestructive = true,
            onConfirm = {
                val khataId = tx.khataId.ifBlank { currentKhata?.id ?: "" }
                khataViewModel.permanentlyDeleteTransaction(
                    khataId = khataId,
                    transactionId = tx.id,
                    personId = tx.personId,
                    personName = tx.personName,
                    amount = tx.amount,
                    performerName = currentUserName
                )
                txToPermanentlyDelete = null
            },
            onDismiss = { txToPermanentlyDelete = null }
        )
    }
}

@Composable
fun TrashKhataCard(
    khata: Khata,
    onRecover: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    val finColors = LocalFinancialColors.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trash_khata_card_${khata.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, finColors.cardBorder.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier.size(38.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = khata.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = "In Trash",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // People count & balances
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${khata.totalPeople} People preserved",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val net = khata.netBalance
                val netLabel = if (net > 0) "Lena" else if (net < 0) "Dena" else "Settled"
                val netColor = if (net > 0) finColors.lena else if (net < 0) finColors.dena else MaterialTheme.colorScheme.onSurfaceVariant
                Text(
                    text = "$netLabel: ${formatCurrency(abs(net))}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = netColor
                )
            }

            // Deletion Metadata
            if (khata.deletedAt != null || !khata.deletedBy.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Deleted ${if (khata.deletedAt != null) formatDateTime(khata.deletedAt.toDate()) else ""} ${if (!khata.deletedBy.isNullOrBlank()) "by ${khata.deletedBy}" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = finColors.cardBorder.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons: Recover & Permanent Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onPermanentDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("perm_delete_button_${khata.id}")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Permanent Delete", fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onRecover,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("recover_button_${khata.id}")
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Recover", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
