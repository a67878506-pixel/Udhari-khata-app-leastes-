package com.example.ui.screens

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Person
import com.example.ui.components.AppConfirmationDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FinancialSummaryCard
import com.example.ui.components.SearchTextField
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDateOnly
import com.example.ui.theme.GreenLena
import com.example.ui.theme.RedDena
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.KhataViewModel
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhataDetailScreen(
    khataViewModel: KhataViewModel,
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onPersonClick: (Person) -> Unit,
    onShareKhataClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    val khataState by khataViewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()

    val currentKhata = khataState.currentKhata ?: return
    val currentUid = authState.currentUser?.uid ?: ""
    val currentUserName = authState.userProfile?.name ?: "User"
    val isOwner = currentKhata.ownerUid == currentUid

    var showAddPersonDialog by remember { mutableStateOf(false) }
    var newPersonName by remember { mutableStateOf("") }
    var newPersonPhone by remember { mutableStateOf("") }

    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameValue by remember { mutableStateOf(currentKhata.name) }
    var showDeleteKhataDialog by remember { mutableStateOf(false) }

    val people = when (val state = khataState.people) {
        is UiState.Success -> state.data
        else -> emptyList()
    }

    val filteredPeople = people.filter {
        it.name.contains(khataState.personSearchQuery, ignoreCase = true) ||
                it.phone.contains(khataState.personSearchQuery, ignoreCase = true)
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
                        Text(currentKhata.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = if (isOwner) "Owner (Private)" else "Shared Khata",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("khata_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // History button
                    IconButton(onClick = onHistoryClick, modifier = Modifier.testTag("khata_history_button")) {
                        Icon(Icons.Default.History, contentDescription = "Khata Activity History")
                    }
                    // Share / Manage Partners button
                    IconButton(onClick = onShareKhataClick, modifier = Modifier.testTag("khata_share_button")) {
                        Icon(Icons.Default.Share, contentDescription = "Share Khata")
                    }
                    // More options menu
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.testTag("khata_options_button")) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename Khata") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                renameValue = currentKhata.name
                                showRenameDialog = true
                            }
                        )
                        if (isOwner) {
                            DropdownMenuItem(
                                text = { Text("Move to Trash", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    showDeleteKhataDialog = true
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Leave Shared Khata", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    khataViewModel.leaveSharedKhata(currentKhata.id, currentUserName, onBack)
                                }
                            )
                        }
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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    newPersonName = ""
                    newPersonPhone = ""
                    showAddPersonDialog = true
                },
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("Add Person", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_person_fab")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Financial Summary Card for this Khata
            FinancialSummaryCard(
                totalLena = currentKhata.totalLena,
                totalDena = currentKhata.totalDena,
                netBalance = currentKhata.netBalance,
                title = "Khata Balance (${people.size} People)",
                modifier = Modifier.padding(16.dp)
            )

            // Person Search Bar
            SearchTextField(
                value = khataState.personSearchQuery,
                onValueChange = { khataViewModel.setPersonSearchQuery(it) },
                placeholder = "Search person by name or phone...",
                modifier = Modifier.padding(horizontal = 16.dp),
                testTag = "person_search_input"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // People List
            when (khataState.people) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    EmptyStateCard(
                        title = "Error loading people",
                        subtitle = (khataState.people as UiState.Error).message
                    )
                }
                is UiState.Success -> {
                    if (filteredPeople.isEmpty()) {
                        EmptyStateCard(
                            title = if (khataState.personSearchQuery.isNotEmpty()) "No people match '${khataState.personSearchQuery}'" else "No People in this Khata",
                            subtitle = if (khataState.personSearchQuery.isNotEmpty()) "Try searching with a different name" else "Add your customers, friends, or partners to start recording hisaab.",
                            actionButton = {
                                Button(
                                    onClick = {
                                        newPersonName = ""
                                        newPersonPhone = ""
                                        showAddPersonDialog = true
                                    },
                                    modifier = Modifier.testTag("empty_add_person_button")
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add Person")
                                }
                            }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(filteredPeople, key = { it.id }) { person ->
                                PersonCard(
                                    person = person,
                                    onClick = {
                                        khataViewModel.selectPerson(person)
                                        onPersonClick(person)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Person Dialog
    if (showAddPersonDialog) {
        AlertDialog(
            onDismissRequest = { showAddPersonDialog = false },
            title = { Text("Add Person to Khata", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPersonName,
                        onValueChange = { newPersonName = it },
                        label = { Text("Person Name *") },
                        placeholder = { Text("e.g. Ramesh Kumar") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_person_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPersonPhone,
                        onValueChange = { newPersonPhone = it },
                        label = { Text("Phone Number (Optional)") },
                        placeholder = { Text("e.g. 9876543210") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_person_phone_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newPersonName.trim()
                        if (name.isNotEmpty()) {
                            showAddPersonDialog = false
                            khataViewModel.addPerson(
                                khataId = currentKhata.id,
                                name = name,
                                phone = newPersonPhone.trim(),
                                performerName = currentUserName,
                                onSuccess = {}
                            )
                        }
                    },
                    enabled = newPersonName.isNotBlank() && !khataState.isActionLoading,
                    modifier = Modifier.testTag("save_person_button")
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPersonDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename Khata Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Khata") },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    label = { Text("Khata Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("rename_khata_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameValue.isNotBlank()) {
                            showRenameDialog = false
                            khataViewModel.updateKhataName(currentKhata.id, renameValue.trim(), currentUserName)
                        }
                    },
                    modifier = Modifier.testTag("save_rename_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Khata (Move to Trash) Confirmation Dialog
    if (showDeleteKhataDialog) {
        AppConfirmationDialog(
            title = "Move to Trash?",
            message = "Move '${currentKhata.name}' to Trash? It will disappear from your active Khatas but all people and transactions are safely preserved. You can restore it anytime from Trash.",
            confirmButtonText = "Move to Trash",
            onConfirm = {
                showDeleteKhataDialog = false
                khataViewModel.softDeleteKhata(currentKhata.id, currentUserName, onBack)
            },
            onDismiss = { showDeleteKhataDialog = false }
        )
    }
}

@Composable
fun PersonCard(
    person: Person,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("person_card_${person.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Initials circle
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = person.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = person.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (person.phone.isNotEmpty()) {
                        Text(
                            text = person.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (person.lastTransactionAt != null) {
                        Text(
                            text = "Last: ${formatDateOnly(person.lastTransactionAt.toDate())}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Person balance
            val net = person.netBalance
            val balanceLabel = if (net > 0) "Lena Hai" else if (net < 0) "Dena Hai" else "Settled"
            val balanceColor = if (net > 0) GreenLena else if (net < 0) RedDena else Color.Gray

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrency(kotlin.math.abs(net)),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = balanceColor
                )
                Text(
                    text = balanceLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = balanceColor
                )
            }
        }
    }
}
