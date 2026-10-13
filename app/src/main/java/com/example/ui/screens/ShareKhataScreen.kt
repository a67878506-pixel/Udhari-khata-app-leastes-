package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ShareRequest
import com.example.ui.components.AppConfirmationDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.InitialAvatar
import com.example.ui.theme.LocalFinancialColors
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.KhataViewModel
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareKhataScreen(
    khataViewModel: KhataViewModel,
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val khataState by khataViewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val finColors = LocalFinancialColors.current

    val currentKhata = khataState.currentKhata ?: return
    val currentUid = authState.currentUser?.uid ?: ""
    val currentUserName = authState.userProfile?.name ?: "User"
    val currentUserCustomId = authState.userProfile?.customUserId ?: ""
    val isOwner = currentKhata.ownerUid == currentUid

    var searchUserIdInput by remember { mutableStateOf("") }
    var partnerToRemove by remember { mutableStateOf<Pair<String, String>?>(null) } // uid to name

    val incomingRequests = when (val state = khataState.incomingShareRequests) {
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
                        Text("Share & Partners", fontWeight = FontWeight.Bold)
                        Text(
                            text = currentKhata.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("share_back_button")) {
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Khata-Specific Sharing notice
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Sharing is strictly KHATA-SPECIFIC. Authorized partners can only view & manage '${currentKhata.name}'. Your other khatas remain 100% private.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Current Members Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, finColors.cardBorder.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "AUTHORIZED MEMBERS (${currentKhata.memberUids.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Owner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            InitialAvatar(name = currentKhata.ownerName, size = 38.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (currentKhata.ownerUid == currentUid) "${currentKhata.ownerName} (You)" else currentKhata.ownerName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text("Owner", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Partners
                    val partnerUids = currentKhata.memberUids.filter { it != currentKhata.ownerUid }
                    partnerUids.forEach { pUid ->
                        val partnerName = currentKhata.members[pUid] ?: "Partner"
                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = finColors.cardBorder.copy(alpha = 0.35f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                InitialAvatar(name = partnerName, size = 38.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (pUid == currentUid) "$partnerName (You)" else partnerName,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text("Authorized Partner", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium)
                                }
                            }

                            // Owner can remove partner, or Partner themselves can leave
                            if (isOwner) {
                                TextButton(
                                    onClick = { partnerToRemove = Pair(pUid, partnerName) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.testTag("remove_partner_$pUid")
                                ) {
                                    Text("Stop Sharing", fontWeight = FontWeight.Bold)
                                }
                            } else if (pUid == currentUid) {
                                TextButton(
                                    onClick = { partnerToRemove = Pair(pUid, partnerName) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.testTag("leave_khata_button")
                                ) {
                                    Text("Leave Khata", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (partnerUids.isEmpty()) {
                        Text(
                            text = "No partners added yet. Search a user below to send a share request.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            // Search and Add Partner Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, finColors.cardBorder.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ADD PARTNER BY USER ID",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ask your partner for their unique User ID (e.g. 'rahul_123').",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchUserIdInput,
                            onValueChange = {
                                searchUserIdInput = it.trim().lowercase()
                                khataViewModel.clearSearchedPartner()
                            },
                            label = { Text("Partner's User ID") },
                            placeholder = { Text("e.g. anshul_123") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("partner_search_input"),
                            shape = RoundedCornerShape(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { khataViewModel.searchPartner(searchUserIdInput) },
                            enabled = searchUserIdInput.isNotBlank() && !khataState.isSearchingPartner,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .height(56.dp)
                                .testTag("search_partner_button")
                        ) {
                            if (khataState.isSearchingPartner) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                        }
                    }

                    khataState.partnerSearchError?.let { err ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    // Display found user
                    khataState.searchedPartner?.let { foundUser ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    InitialAvatar(name = foundUser.name, size = 36.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(foundUser.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text("@${foundUser.customUserId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Button(
                                    onClick = {
                                        khataViewModel.sendShareRequest(
                                            khataId = currentKhata.id,
                                            khataName = currentKhata.name,
                                            targetUser = foundUser,
                                            senderName = currentUserName,
                                            senderCustomId = currentUserCustomId,
                                            onSuccess = { searchUserIdInput = "" }
                                        )
                                    },
                                    enabled = !khataState.isActionLoading,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("send_share_request_button")
                                ) {
                                    Text("Send Request", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Pending Share Requests
            if (incomingRequests.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, finColors.cardBorder.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "INCOMING SHARE REQUESTS (${incomingRequests.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        incomingRequests.forEach { req ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "${req.fromName} (@${req.fromCustomUserId})",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "Invites you to collaborate on '${req.khataName}'",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = { khataViewModel.respondToShareRequest(req, accept = false, userName = currentUserName) },
                                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                            modifier = Modifier.testTag("decline_share_request_${req.id}")
                                        ) {
                                            Text("Decline")
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = { khataViewModel.respondToShareRequest(req, accept = true, userName = currentUserName) },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = finColors.lena),
                                            modifier = Modifier.testTag("accept_share_request_${req.id}")
                                        ) {
                                            Text("Accept", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Stop Sharing / Leave Khata Confirmation Dialog
    partnerToRemove?.let { (pUid, pName) ->
        val isSelfLeaving = pUid == currentUid
        AppConfirmationDialog(
            title = if (isSelfLeaving) "Leave '${currentKhata.name}'?" else "Stop Sharing with $pName?",
            message = if (isSelfLeaving)
                "Are you sure you want to leave '${currentKhata.name}'? You will immediately lose access to this shared khata."
            else
                "If you stop sharing, $pName will immediately lose all access to '${currentKhata.name}'. All records remain safe.",
            confirmButtonText = if (isSelfLeaving) "Leave Khata" else "Stop Sharing",
            onConfirm = {
                if (isSelfLeaving) {
                    khataViewModel.leaveSharedKhata(currentKhata.id, currentUserName, onBack)
                } else {
                    khataViewModel.stopSharing(currentKhata.id, pUid, pName, currentUserName)
                }
                partnerToRemove = null
            },
            onDismiss = { partnerToRemove = null }
        )
    }
}
