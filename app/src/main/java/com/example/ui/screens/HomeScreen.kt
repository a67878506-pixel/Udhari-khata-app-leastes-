package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Khata
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FinancialSummaryCard
import com.example.ui.components.SearchTextField
import com.example.ui.components.formatCurrency
import com.example.ui.theme.*
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.KhataViewModel
import com.example.ui.viewmodel.UiState
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    khataViewModel: KhataViewModel,
    authViewModel: AuthViewModel,
    onKhataClick: (Khata) -> Unit,
    onNavigateToShareRequests: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val khataState by khataViewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var newKhataName by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, OWNED, SHARED

    val currentUid = authState.currentUser?.uid ?: ""
    val currentUserName = authState.userProfile?.name ?: "User"

    val khatas = when (val state = khataState.khatas) {
        is UiState.Success -> state.data.filter { !it.isDeleted }
        else -> emptyList()
    }

    // Filter by search query and tab filter
    val filteredKhatas = khatas.filter { khata ->
        !khata.isDeleted &&
        khata.name.contains(khataState.khataSearchQuery, ignoreCase = true) &&
        when (selectedFilter) {
            "OWNED" -> khata.ownerUid == currentUid
            "SHARED" -> khata.ownerUid != currentUid
            else -> true
        }
    }

    // Overall balances across all accessible khatas
    val totalLena = khatas.sumOf { it.totalLena }
    val totalDena = khatas.sumOf { it.totalDena }
    val netBalance = totalLena - totalDena

    // Pending share requests count
    val pendingRequestsCount = when (val reqs = khataState.incomingShareRequests) {
        is UiState.Success -> reqs.data.size
        else -> 0
    }

    // Trash count
    val trashCount = when (val trash = khataState.trashKhatas) {
        is UiState.Success -> trash.data.size
        else -> 0
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
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Udhaar Khata",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 19.sp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            authState.userProfile?.let {
                                Text(
                                    text = "@${it.customUserId}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Trash / Recently Deleted button
                    IconButton(
                        onClick = onNavigateToTrash,
                        modifier = Modifier.testTag("trash_screen_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (trashCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ) { Text("$trashCount") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Trash / Deleted Khatas")
                        }
                    }

                    // Pending Share Requests icon with badge
                    IconButton(
                        onClick = onNavigateToShareRequests,
                        modifier = Modifier.testTag("incoming_requests_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (pendingRequestsCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.tertiary,
                                        contentColor = Color.White
                                    ) { Text("$pendingRequestsCount") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.GroupAdd, contentDescription = "Share Requests")
                        }
                    }

                    // Profile icon
                    IconButton(
                        onClick = onNavigateToProfile,
                        modifier = Modifier.testTag("profile_button")
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    newKhataName = ""
                    showCreateDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Create Khata", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                modifier = Modifier.testTag("create_khata_fab")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Overall summary banner
            FinancialSummaryCard(
                totalLena = totalLena,
                totalDena = totalDena,
                netBalance = netBalance,
                title = "Total Business Hisaab",
                modifier = Modifier.padding(16.dp)
            )

            // Search Bar for Khatas
            SearchTextField(
                value = khataState.khataSearchQuery,
                onValueChange = { khataViewModel.setKhataSearchQuery(it) },
                placeholder = "Search khatas by name...",
                modifier = Modifier.padding(horizontal = 16.dp),
                testTag = "khata_search_input"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("All (${khatas.size})", fontWeight = FontWeight.Medium) },
                    shape = RoundedCornerShape(10.dp)
                )
                FilterChip(
                    selected = selectedFilter == "OWNED",
                    onClick = { selectedFilter = "OWNED" },
                    label = { Text("My Khatas", fontWeight = FontWeight.Medium) },
                    shape = RoundedCornerShape(10.dp)
                )
                FilterChip(
                    selected = selectedFilter == "SHARED",
                    onClick = { selectedFilter = "SHARED" },
                    label = { Text("Shared with Me", fontWeight = FontWeight.Medium) },
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Khatas List
            when (khataState.khatas) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is UiState.Error -> {
                    EmptyStateCard(
                        title = "Unable to load khatas",
                        subtitle = (khataState.khatas as UiState.Error).message,
                        actionButton = {
                            Button(onClick = { khataViewModel.observeKhatas() }) {
                                Text("Retry")
                            }
                        }
                    )
                }
                is UiState.Success -> {
                    if (filteredKhatas.isEmpty()) {
                        EmptyStateCard(
                            title = if (khataState.khataSearchQuery.isNotEmpty()) "No Khatas match '${khataState.khataSearchQuery}'" else "No Khatas Created Yet",
                            subtitle = if (khataState.khataSearchQuery.isNotEmpty()) "Check the spelling or try a different search" else "Create your first Khata to start maintaining udhaar & hisaab records with customers and suppliers.",
                            actionButton = {
                                Button(
                                    onClick = {
                                        newKhataName = ""
                                        showCreateDialog = true
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("empty_create_khata_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Create Khata", fontWeight = FontWeight.Bold)
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
                            items(filteredKhatas, key = { it.id }) { khata ->
                                KhataCard(
                                    khata = khata,
                                    currentUid = currentUid,
                                    onClick = {
                                        khataViewModel.selectKhata(khata)
                                        onKhataClick(khata)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Khata Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Create New Khata",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Give this khata a name (e.g., Shop, Business, Personal). Each khata is private to you until shared.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newKhataName,
                        onValueChange = { newKhataName = it },
                        label = { Text("Khata Name") },
                        placeholder = { Text("e.g. Ramesh General Store") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_khata_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newKhataName.trim()
                        if (name.isNotEmpty()) {
                            showCreateDialog = false
                            khataViewModel.createKhata(name, currentUserName) { khataId ->
                                val created = khatas.find { it.id == khataId }
                                if (created != null) {
                                    khataViewModel.selectKhata(created)
                                    onKhataClick(created)
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    enabled = newKhataName.isNotBlank() && !khataState.isActionLoading,
                    modifier = Modifier.testTag("save_new_khata_button")
                ) {
                    Text("Create", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreateDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@Composable
fun KhataCard(
    khata: Khata,
    currentUid: String,
    onClick: () -> Unit
) {
    val isOwner = khata.ownerUid == currentUid
    val partnerCount = if (khata.memberUids.size > 1) khata.memberUids.size - 1 else 0
    val finColors = LocalFinancialColors.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("khata_card_${khata.id}"),
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
                    // Modern book emblem
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = khata.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Group,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${khata.totalPeople} People",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Ownership / Sharing tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isOwner) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isOwner) Icons.Default.Lock else Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (isOwner) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isOwner) {
                                if (partnerCount > 0) "Owner ($partnerCount partner)" else "Private"
                            } else "Shared (${khata.ownerName})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isOwner) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = finColors.cardBorder.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            // Balance summary
            val net = khata.netBalance
            val netLabel = if (net > 0) "Aapko Milenge (Lena)" else if (net < 0) "Aapko Dene Hain (Dena)" else "Hisaab Settled"
            val netColor = if (net > 0) finColors.lena else if (net < 0) finColors.dena else MaterialTheme.colorScheme.onSurfaceVariant

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = netLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = formatCurrency(abs(net)),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = netColor
                )
            }
        }
    }
}
