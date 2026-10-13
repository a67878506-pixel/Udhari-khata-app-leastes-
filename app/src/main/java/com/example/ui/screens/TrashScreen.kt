package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Khata
import com.example.ui.components.AppConfirmationDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDateTime
import com.example.ui.theme.GreenLena
import com.example.ui.theme.RedDena
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.KhataViewModel
import com.example.ui.viewmodel.UiState

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

    var khataToRecover by remember { mutableStateOf<Khata?>(null) }
    var khataToPermanentlyDelete by remember { mutableStateOf<Khata?>(null) }

    val trashKhatas = when (val state = khataState.trashKhatas) {
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
                        Text("Trash / Recently Deleted", fontWeight = FontWeight.Bold)
                        Text(
                            text = "${trashKhatas.size} Deleted Khatas",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
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
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Deleted khatas remain safely preserved with all customers and hisaab records. You can recover them anytime or delete them permanently.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when (khataState.trashKhatas) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
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
                            title = "Trash is Empty",
                            subtitle = "You have no deleted Khatas. Khatas you delete will appear here so you can recover them anytime."
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
    }

    // Recover Confirmation Dialog
    khataToRecover?.let { khata ->
        AppConfirmationDialog(
            title = "Recover Khata?",
            message = "Do you want to restore '${khata.name}'? It will immediately return to your Home Khatas list with all people and transactions intact.",
            confirmButtonText = "Recover",
            onConfirm = {
                khataViewModel.recoverKhata(khata.id, currentUserName)
                khataToRecover = null
            },
            onDismiss = { khataToRecover = null }
        )
    }

    // Permanent Delete Confirmation Dialog
    khataToPermanentlyDelete?.let { khata ->
        AppConfirmationDialog(
            title = "Permanently Delete '${khata.name}'?",
            message = "WARNING: This action is permanent and IRREVERSIBLE. All associated people, transactions, and history will be completely erased. Are you sure you want to proceed?",
            confirmButtonText = "Permanently Delete",
            onConfirm = {
                khataViewModel.permanentlyDeleteKhata(khata.id)
                khataToPermanentlyDelete = null
            },
            onDismiss = { khataToPermanentlyDelete = null }
        )
    }
}

@Composable
fun TrashKhataCard(
    khata: Khata,
    onRecover: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trash_khata_card_${khata.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
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
                val netColor = if (net > 0) GreenLena else if (net < 0) RedDena else Color.Gray
                Text(
                    text = "$netLabel: ${formatCurrency(kotlin.math.abs(net))}",
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
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                    Text("Permanent Delete")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onRecover,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("recover_button_${khata.id}")
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Recover")
                }
            }
        }
    }
}
