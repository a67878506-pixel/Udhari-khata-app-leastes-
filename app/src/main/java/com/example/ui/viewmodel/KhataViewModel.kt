package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Khata
import com.example.data.model.KhataHistory
import com.example.data.model.Person
import com.example.data.model.ShareRequest
import com.example.data.model.Transaction
import com.example.data.model.UserProfile
import com.example.data.repository.KhataRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class KhataUiState(
    val khatas: UiState<List<Khata>> = UiState.Loading,
    val khataSearchQuery: String = "",
    val currentKhata: Khata? = null,
    val people: UiState<List<Person>> = UiState.Loading,
    val personSearchQuery: String = "",
    val currentPerson: Person? = null,
    val transactions: UiState<List<Transaction>> = UiState.Loading,
    val history: UiState<List<KhataHistory>> = UiState.Loading,
    val incomingShareRequests: UiState<List<ShareRequest>> = UiState.Loading,
    val searchedPartner: UserProfile? = null,
    val isSearchingPartner: Boolean = false,
    val partnerSearchError: String? = null,
    val isActionLoading: Boolean = false,
    val actionErrorMessage: String? = null,
    val actionSuccessMessage: String? = null
)

class KhataViewModel(
    private val repository: KhataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(KhataUiState())
    val uiState: StateFlow<KhataUiState> = _uiState.asStateFlow()

    private var peopleJob: Job? = null
    private var transactionsJob: Job? = null
    private var historyJob: Job? = null

    init {
        if (repository.getCurrentUid() != null) {
            observeKhatas()
            observeShareRequests()
        } else {
            _uiState.update { it.copy(khatas = UiState.Success(emptyList()), incomingShareRequests = UiState.Success(emptyList())) }
        }
    }

    fun resetState() {
        peopleJob?.cancel()
        transactionsJob?.cancel()
        historyJob?.cancel()
        _uiState.value = KhataUiState(
            khatas = UiState.Success(emptyList()),
            incomingShareRequests = UiState.Success(emptyList())
        )
    }

    fun clearActionMessages() {
        _uiState.update { it.copy(actionErrorMessage = null, actionSuccessMessage = null) }
    }

    fun setKhataSearchQuery(query: String) {
        _uiState.update { it.copy(khataSearchQuery = query) }
    }

    fun setPersonSearchQuery(query: String) {
        _uiState.update { it.copy(personSearchQuery = query) }
    }

    fun observeKhatas() {
        val uid = repository.getCurrentUid()
        if (uid == null) {
            _uiState.update { it.copy(khatas = UiState.Success(emptyList())) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(khatas = UiState.Loading) }
            try {
                repository.observeUserKhatas()
                    .catch { e ->
                        _uiState.update { it.copy(khatas = UiState.Error(e.localizedMessage ?: "Failed to load khatas")) }
                    }
                    .collect { list ->
                        _uiState.update { currentState ->
                            val updatedCurrentKhata = currentState.currentKhata?.let { curr ->
                                list.find { it.id == curr.id } ?: curr
                            }
                            currentState.copy(
                                khatas = UiState.Success(list),
                                currentKhata = updatedCurrentKhata
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(khatas = UiState.Error(e.localizedMessage ?: "Failed to load khatas")) }
            }
        }
    }

    fun observeShareRequests() {
        val uid = repository.getCurrentUid()
        if (uid == null) {
            _uiState.update { it.copy(incomingShareRequests = UiState.Success(emptyList())) }
            return
        }
        viewModelScope.launch {
            try {
                repository.observeIncomingShareRequests()
                    .catch { e ->
                        _uiState.update { it.copy(incomingShareRequests = UiState.Error(e.localizedMessage ?: "Failed to load requests")) }
                    }
                    .collect { list ->
                        _uiState.update { it.copy(incomingShareRequests = UiState.Success(list)) }
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(incomingShareRequests = UiState.Error(e.localizedMessage ?: "Failed to load requests")) }
            }
        }
    }

    fun selectKhata(khata: Khata) {
        _uiState.update { it.copy(currentKhata = khata, personSearchQuery = "") }
        observePeopleForKhata(khata.id)
        observeHistoryForKhata(khata.id)
    }

    private fun observePeopleForKhata(khataId: String) {
        peopleJob?.cancel()
        peopleJob = viewModelScope.launch {
            _uiState.update { it.copy(people = UiState.Loading) }
            repository.observePeople(khataId)
                .catch { e ->
                    _uiState.update { it.copy(people = UiState.Error(e.localizedMessage ?: "Failed to load people")) }
                }
                .collect { list ->
                    _uiState.update { currentState ->
                        val updatedCurrentPerson = currentState.currentPerson?.let { curr ->
                            list.find { it.id == curr.id } ?: curr
                        }
                        currentState.copy(
                            people = UiState.Success(list),
                            currentPerson = updatedCurrentPerson
                        )
                    }
                }
        }
    }

    private fun observeHistoryForKhata(khataId: String) {
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            _uiState.update { it.copy(history = UiState.Loading) }
            repository.observeKhataHistory(khataId)
                .catch { e ->
                    _uiState.update { it.copy(history = UiState.Error(e.localizedMessage ?: "Failed to load history")) }
                }
                .collect { list ->
                    _uiState.update { it.copy(history = UiState.Success(list)) }
                }
        }
    }

    fun selectPerson(person: Person) {
        _uiState.update { it.copy(currentPerson = person) }
        val khata = _uiState.value.currentKhata ?: return
        observeTransactionsForPerson(khata.id, person.id)
    }

    private fun observeTransactionsForPerson(khataId: String, personId: String) {
        transactionsJob?.cancel()
        transactionsJob = viewModelScope.launch {
            _uiState.update { it.copy(transactions = UiState.Loading) }
            repository.observeTransactions(khataId, personId)
                .catch { e ->
                    _uiState.update { it.copy(transactions = UiState.Error(e.localizedMessage ?: "Failed to load transactions")) }
                }
                .collect { list ->
                    _uiState.update { it.copy(transactions = UiState.Success(list)) }
                }
        }
    }

    // --- KHATA ACTIONS ---

    fun createKhata(name: String, ownerName: String, onSuccess: (String) -> Unit) {
        if (name.isBlank()) {
            _uiState.update { it.copy(actionErrorMessage = "Khata name cannot be empty.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, actionErrorMessage = null) }
            val res = repository.createKhata(name, ownerName)
            _uiState.update { it.copy(isActionLoading = false) }
            if (res.isSuccess) {
                _uiState.update { it.copy(actionSuccessMessage = "Khata '$name' created successfully.") }
                onSuccess(res.getOrThrow())
            } else {
                _uiState.update { it.copy(actionErrorMessage = res.exceptionOrNull()?.message ?: "Failed to create khata.") }
            }
        }
    }

    fun updateKhataName(khataId: String, newName: String, editorName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            val res = repository.updateKhataName(khataId, newName, editorName)
            _uiState.update { it.copy(isActionLoading = false) }
            if (res.isSuccess) {
                _uiState.update { it.copy(actionSuccessMessage = "Khata updated successfully.") }
            } else {
                _uiState.update { it.copy(actionErrorMessage = res.exceptionOrNull()?.message ?: "Failed to update khata.") }
            }
        }
    }

    fun deleteKhata(khataId: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            val res = repository.deleteKhata(khataId)
            _uiState.update { it.copy(isActionLoading = false) }
            if (res.isSuccess) {
                _uiState.update { it.copy(currentKhata = null, actionSuccessMessage = "Khata deleted.") }
                onDeleted()
            } else {
                _uiState.update { it.copy(actionErrorMessage = res.exceptionOrNull()?.message ?: "Failed to delete khata.") }
            }
        }
    }

    // --- PEOPLE ACTIONS ---

    fun addPerson(khataId: String, name: String, phone: String, performerName: String, onSuccess: () -> Unit) {
        if (name.isBlank()) {
            _uiState.update { it.copy(actionErrorMessage = "Person name cannot be empty.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            val res = repository.addPerson(khataId, name, phone, performerName)
            _uiState.update { it.copy(isActionLoading = false) }
            if (res.isSuccess) {
                _uiState.update { it.copy(actionSuccessMessage = "'$name' added successfully.") }
                onSuccess()
            } else {
                _uiState.update { it.copy(actionErrorMessage = res.exceptionOrNull()?.message ?: "Failed to add person.") }
            }
        }
    }

    fun updatePerson(khataId: String, personId: String, name: String, phone: String, performerName: String, onSuccess: () -> Unit) {
        if (name.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            val res = repository.updatePerson(khataId, personId, name, phone, performerName)
            _uiState.update { it.copy(isActionLoading = false) }
            if (res.isSuccess) {
                _uiState.update { it.copy(actionSuccessMessage = "Details updated.") }
                onSuccess()
            } else {
                _uiState.update { it.copy(actionErrorMessage = res.exceptionOrNull()?.message ?: "Failed to update.") }
            }
        }
    }

    fun deletePerson(khataId: String, personId: String, personName: String, performerName: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            val res = repository.deletePerson(khataId, personId, personName, performerName)
            _uiState.update { it.copy(isActionLoading = false) }
            if (res.isSuccess) {
                _uiState.update { it.copy(currentPerson = null, actionSuccessMessage = "Person deleted.") }
                onDeleted()
            } else {
                _uiState.update { it.copy(actionErrorMessage = res.exceptionOrNull()?.message ?: "Failed to delete person.") }
            }
        }
    }

    // --- TRANSACTIONS ACTIONS ---

    fun addTransaction(
        khataId: String,
        personId: String,
        personName: String,
        amount: Double,
        type: String, // LENA or DENA
        note: String,
        dateMillis: Long,
        performerName: String,
        onSuccess: () -> Unit
    ) {
        if (amount <= 0) {
            _uiState.update { it.copy(actionErrorMessage = "Amount must be greater than zero.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            val res = repository.addTransaction(khataId, personId, personName, amount, type, note, dateMillis, performerName)
            _uiState.update { it.copy(isActionLoading = false) }
            if (res.isSuccess) {
                _uiState.update { it.copy(actionSuccessMessage = "Transaction recorded successfully.") }
                onSuccess()
            } else {
                _uiState.update { it.copy(actionErrorMessage = res.exceptionOrNull()?.message ?: "Failed to add transaction.") }
            }
        }
    }

    fun updateTransaction(
        khataId: String,
        transactionId: String,
        personId: String,
        personName: String,
        newAmount: Double,
        newType: String,
        newNote: String,
        newDateMillis: Long,
        performerName: String,
        onSuccess: () -> Unit
    ) {
        if (newAmount <= 0) {
            _uiState.update { it.copy(actionErrorMessage = "Amount must be greater than zero.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            val res = repository.updateTransaction(
                khataId, transactionId, personId, personName,
                newAmount, newType, newNote, newDateMillis, performerName
            )
            _uiState.update { it.copy(isActionLoading = false) }
            if (res.isSuccess) {
                _uiState.update { it.copy(actionSuccessMessage = "Transaction updated.") }
                onSuccess()
            } else {
                _uiState.update { it.copy(actionErrorMessage = res.exceptionOrNull()?.message ?: "Failed to update transaction.") }
            }
        }
    }

    fun deleteTransaction(
        khataId: String,
        transactionId: String,
        personId: String,
        personName: String,
        amount: Double,
        performerName: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            val res = repository.deleteTransaction(khataId, transactionId, personId, personName, amount, performerName)
            _uiState.update { it.copy(isActionLoading = false) }
            if (res.isSuccess) {
                _uiState.update { it.copy(actionSuccessMessage = "Transaction deleted.") }
            } else {
                _uiState.update { it.copy(actionErrorMessage = res.exceptionOrNull()?.message ?: "Failed to delete transaction.") }
            }
        }
    }

    // --- PARTNER & SHARING ACTIONS ---

    fun searchPartner(customUserId: String) {
        val clean = customUserId.trim().lowercase()
        if (clean.isBlank()) {
            _uiState.update { it.copy(searchedPartner = null, partnerSearchError = "Please enter a User ID.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingPartner = true, partnerSearchError = null, searchedPartner = null) }
            val res = repository.searchUserByCustomId(clean)
            _uiState.update { it.copy(isSearchingPartner = false) }
            if (res.isSuccess) {
                val user = res.getOrNull()
                if (user != null) {
                    _uiState.update { it.copy(searchedPartner = user, partnerSearchError = null) }
                } else {
                    _uiState.update { it.copy(partnerSearchError = "No registered user found with User ID '$clean'.") }
                }
            } else {
                _uiState.update { it.copy(partnerSearchError = res.exceptionOrNull()?.message ?: "Error searching user.") }
            }
        }
    }

    fun clearSearchedPartner() {
        _uiState.update { it.copy(searchedPartner = null, partnerSearchError = null) }
    }

    fun sendShareRequest(
        khataId: String,
        khataName: String,
        targetUser: UserProfile,
        senderName: String,
        senderCustomId: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            val res = repository.sendShareRequest(khataId, khataName, targetUser, senderName, senderCustomId)
            _uiState.update { it.copy(isActionLoading = false) }
            if (res.isSuccess) {
                _uiState.update {
                    it.copy(
                        actionSuccessMessage = "Share request sent to @${targetUser.customUserId} (${targetUser.name}).",
                        searchedPartner = null
                    )
                }
                onSuccess()
            } else {
                _uiState.update { it.copy(actionErrorMessage = res.exceptionOrNull()?.message ?: "Failed to send request.") }
            }
        }
    }

    fun respondToShareRequest(request: ShareRequest, accept: Boolean, userName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            val res = repository.respondToShareRequest(request, accept, userName)
            _uiState.update { it.copy(isActionLoading = false) }
            if (res.isSuccess) {
                val action = if (accept) "accepted" else "rejected"
                _uiState.update { it.copy(actionSuccessMessage = "Share request $action.") }
            } else {
                _uiState.update { it.copy(actionErrorMessage = res.exceptionOrNull()?.message ?: "Failed to respond.") }
            }
        }
    }

    fun stopSharing(khataId: String, partnerUid: String, partnerName: String, ownerName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            val res = repository.stopSharing(khataId, partnerUid, partnerName, ownerName)
            _uiState.update { it.copy(isActionLoading = false) }
            if (res.isSuccess) {
                _uiState.update { it.copy(actionSuccessMessage = "Access removed for $partnerName.") }
            } else {
                _uiState.update { it.copy(actionErrorMessage = res.exceptionOrNull()?.message ?: "Failed to remove partner.") }
            }
        }
    }
}
