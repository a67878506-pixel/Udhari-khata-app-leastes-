package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.ui.components.*
import com.example.ui.theme.LocalFinancialColors
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.KhataViewModel
import com.example.ui.viewmodel.UiState
import kotlin.math.abs

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
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, LENA, DENA, SETTLED, TRASH
    var personToRecover by remember { mutableStateOf<Person?>(null) }
    var personToPermanentlyDelete by remember { mutableStateOf<Person?>(null) }

    val people = when (val state = khataState.people) {
        is UiState.Success -> state.data.filter { !it.isDeleted }
        else -> emptyList()
    }

    val trashPeople = when (val state = khataState.trashPeople) {
        is UiState.Success -> state.data.filter { it.isDeleted }
        else -> emptyList()
    }

    val filteredPeople = if (selectedFilter == "TRASH") {
        trashPeople.filter { person ->
            person.name.contains(khataState.personSearchQuery, ignoreCase = true) ||
                    person.phone.contains(khataState.personSearchQuery, ignoreCase = true)
        }
    } else {
        people.filter { person ->
            val matchesQuery = person.name.contains(khataState.personSearchQuery, ignoreCase = true) ||
                    person.phone.contains(khataState.personSearchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "LENA" -> person.netBalance > 0
                "DENA" -> person.netBalance < 0
                "SETTLED" -> person.netBalance == 0.0
                else -> true
            }
            matchesQuery && matchesFilter
        }
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
                        Text(
                            text = currentKhata.name,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isOwner) "Owner (Private)" else "Shared Khata",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
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
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
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

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("All (${people.size})") },
                    shape = RoundedCornerShape(10.dp)
                )
                FilterChip(
                    selected = selectedFilter == "LENA",
                    onClick = { selectedFilter = "LENA" },
                    label = { Text("Lena Hai") },
                    shape = RoundedCornerShape(10.dp)
                )
                FilterChip(
                    selected = selectedFilter == "DENA",
                    onClick = { selectedFilter = "DENA" },
                    label = { Text("Dena Hai") },
                    shape = RoundedCornerShape(10.dp)
                )
                FilterChip(
                    selected = selectedFilter == "SETTLED",
                    onClick = { selectedFilter = "SETTLED" },
                    label = { Text("Settled") },
                    shape = RoundedCornerShape(10.dp)
                )
                FilterChip(
                    selected = selectedFilter == "TRASH",
                    onClick = { selectedFilter = "TRASH" },
                    label = { Text("Trash (${trashPeople.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // People List
            when (if (selectedFilter == "TRASH") khataState.trashPeople else khataState.people) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is UiState.Error -> {
                    EmptyStateCard(
                        title = "Error loading people",
                        subtitle = "Unable to load people records"
                    )
                }
                is UiState.Success -> {
                    if (filteredPeople.isEmpty()) {
                        EmptyStateCard(
                            title = if (selectedFilter == "TRASH") "Trash is Empty" else if (khataState.personSearchQuery.isNotEmpty()) "No people match '${khataState.personSearchQuery}'" else "No People in this Khata",
                            subtitle = if (selectedFilter == "TRASH") "Deleted people will be preserved here and can be recovered anytime." else if (khataState.personSearchQuery.isNotEmpty()) "Try searching with a different name or phone" else "Add your customers, vendors, friends, or partners to start recording hisaab.",
                            actionButton = if (selectedFilter == "TRASH") null else {
                                {
                                    Button(
                                        onClick = {
                                            newPersonName = ""
                                            newPersonPhone = ""
                                            showAddPersonDialog = true
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag("empty_add_person_button")
                                    ) {
                                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Add Person", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 90.dp)
                        ) {
                            items(filteredPeople, key = { it.id }) { person ->
                                if (selectedFilter == "TRASH") {
                                    DeletedPersonCard(
                                        person = person,
                                        onRecover = { personToRecover = person },
                                        onPermanentDelete = { personToPermanentlyDelete = person }
                                    )
                                } else {
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
    }

    // Add Person Dialog
    if (showAddPersonDialog) {
        AlertDialog(
            onDismissRequest = { showAddPersonDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Add Person to Khata", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPersonName,
                        onValueChange = { newPersonName = it },
                        label = { Text("Person Name *") },
                        placeholder = { Text("e.g. Ramesh Kumar") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("add_person_name_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPersonPhone,
                        onValueChange = { newPersonPhone = it },
                        label = { Text("Phone Number (Optional)") },
                        placeholder = { Text("e.g. 9876543210") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("add_person_phone_input")
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
                    shape = RoundedCornerShape(12.dp),
                    enabled = newPersonName.isNotBlank() && !khataState.isActionLoading,
                    modifier = Modifier.testTag("save_person_button")
                ) {
                    Text("Add", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddPersonDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // Rename Khata Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Rename Khata", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    label = { Text("Khata Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
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
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("save_rename_button")
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRenameDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
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

    // Recover Person Confirmation Dialog
    if (personToRecover != null) {
        val p = personToRecover!!
        AppConfirmationDialog(
            title = "Recover Person?",
            message = "Restore '${p.name}' to active customers? All hisaab records and balances will be restored.",
            confirmButtonText = "Recover",
            onConfirm = {
                khataViewModel.recoverPerson(currentKhata.id, p.id, p.name, currentUserName)
                personToRecover = null
            },
            onDismiss = { personToRecover = null }
        )
    }

    // Permanently Delete Person Confirmation Dialog
    if (personToPermanentlyDelete != null) {
        val p = personToPermanentlyDelete!!
        AppConfirmationDialog(
            title = "Permanently Delete Person?",
            message = "Permanently delete '${p.name}' and all associated transactions? This action CANNOT be undone.",
            confirmButtonText = "Delete Permanently",
            onConfirm = {
                khataViewModel.permanentlyDeletePerson(currentKhata.id, p.id, p.name, currentUserName)
                personToPermanentlyDelete = null
            },
            onDismiss = { personToPermanentlyDelete = null }
        )
    }
}

@Composable
fun PersonCard(
    person: Person,
    onClick: () -> Unit
) {
    val finColors = LocalFinancialColors.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("person_card_${person.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, finColors.cardBorder.copy(alpha = 0.5f)),
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
                // Initial avatar
                InitialAvatar(
                    name = person.name,
                    size = 44.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = person.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (person.phone.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = person.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
            val balanceLabel = if (net > 0) "Aapko Milenge" else if (net < 0) "Aapko Dene Hain" else "Hisaab Settled"
            val balanceColor = if (net > 0) finColors.lena else if (net < 0) finColors.dena else MaterialTheme.colorScheme.onSurfaceVariant

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrency(abs(net)),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = balanceColor
                )
                Text(
                    text = balanceLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = balanceColor
                )
            }
        }
    }
}

@Composable
fun DeletedPersonCard(
    person: Person,
    onRecover: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    val finColors = LocalFinancialColors.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("deleted_person_card_${person.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    InitialAvatar(
                        name = person.name,
                        size = 44.dp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = person.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (person.phone.isNotEmpty()) {
                            Text(
                                text = person.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Deleted by ${person.deletedBy ?: "User"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                val net = person.netBalance
                val balanceColor = if (net > 0) finColors.lena else if (net < 0) finColors.dena else MaterialTheme.colorScheme.onSurfaceVariant

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(abs(net)),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = balanceColor
                    )
                    Text(
                        text = if (net > 0) "Lena" else if (net < 0) "Dena" else "Settled",
                        style = MaterialTheme.typography.labelSmall,
                        color = balanceColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                    modifier = Modifier.testTag("perm_delete_person_${person.id}")
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
                    modifier = Modifier.testTag("recover_person_${person.id}")
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Recover", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

