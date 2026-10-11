package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.UserIdCheckResult
import com.example.data.model.UserProfile
import com.example.data.repository.KhataRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class UserIdAvailabilityState {
    IDLE,
    CHECKING,
    AVAILABLE,
    TAKEN,
    ERROR
}

data class AuthUiState(
    val currentUser: FirebaseUser? = null,
    val userProfile: UserProfile? = null,
    val isLoading: Boolean = false,
    val isCheckingUserId: Boolean = false,
    val isUserIdAvailable: Boolean? = null,
    val userIdAvailabilityState: UserIdAvailabilityState = UserIdAvailabilityState.IDLE,
    val userIdErrorMessage: String? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

class AuthViewModel(
    private val repository: KhataRepository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState(currentUser = auth.currentUser))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _uiState.update { it.copy(currentUser = user) }
            if (user != null) {
                loadProfile(user.uid)
            } else {
                _uiState.update { it.copy(userProfile = null) }
            }
        }
        auth.currentUser?.let { loadProfile(it.uid) }
    }

    fun clearMessages() {
        _uiState.update { 
            it.copy(
                errorMessage = null, 
                infoMessage = null,
                userIdErrorMessage = null
            ) 
        }
    }

    fun loadProfile(uid: String) {
        viewModelScope.launch {
            try {
                val profile = repository.getUserProfile(uid)
                _uiState.update { it.copy(userProfile = profile) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to load profile: ${e.localizedMessage}") }
            }
        }
    }

    fun checkUserIdAvailability(userId: String) {
        val clean = userId.trim().lowercase()
        if (clean.length < 3) {
            _uiState.update { 
                it.copy(
                    isUserIdAvailable = null, 
                    isCheckingUserId = false,
                    userIdAvailabilityState = UserIdAvailabilityState.IDLE,
                    userIdErrorMessage = null
                ) 
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isCheckingUserId = true,
                    userIdAvailabilityState = UserIdAvailabilityState.CHECKING,
                    userIdErrorMessage = null
                ) 
            }
            val result = repository.checkUserIdStatus(clean)
            when (result) {
                is UserIdCheckResult.Available -> {
                    _uiState.update { 
                        it.copy(
                            isCheckingUserId = false, 
                            isUserIdAvailable = true,
                            userIdAvailabilityState = UserIdAvailabilityState.AVAILABLE,
                            userIdErrorMessage = null
                        ) 
                    }
                }
                is UserIdCheckResult.Taken -> {
                    _uiState.update { 
                        it.copy(
                            isCheckingUserId = false, 
                            isUserIdAvailable = false,
                            userIdAvailabilityState = UserIdAvailabilityState.TAKEN,
                            userIdErrorMessage = null
                        ) 
                    }
                }
                is UserIdCheckResult.Error -> {
                    _uiState.update { 
                        it.copy(
                            isCheckingUserId = false, 
                            isUserIdAvailable = null,
                            userIdAvailabilityState = UserIdAvailabilityState.ERROR,
                            userIdErrorMessage = "Unable to check User ID. Please try again."
                        ) 
                    }
                }
            }
        }
    }

    fun signUp(
        name: String,
        email: String,
        pass: String,
        customUserId: String,
        onSuccess: () -> Unit
    ) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        val trimmedUserId = customUserId.trim().lowercase()

        if (trimmedName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your Full Name.") }
            return
        }
        if (trimmedEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid email address.") }
            return
        }
        if (pass.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters.") }
            return
        }
        if (trimmedUserId.length < 3) {
            _uiState.update { it.copy(errorMessage = "Unique User ID must be at least 3 characters.") }
            return
        }
        if (!trimmedUserId.matches(Regex("^[a-z0-9_]+$"))) {
            _uiState.update { it.copy(errorMessage = "User ID can only contain lowercase letters, numbers, and underscores.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // 1. Verify User ID availability
                val statusResult = repository.checkUserIdStatus(trimmedUserId)
                when (statusResult) {
                    is UserIdCheckResult.Taken -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isUserIdAvailable = false,
                                userIdAvailabilityState = UserIdAvailabilityState.TAKEN,
                                errorMessage = "User ID '$trimmedUserId' already taken. Choose another."
                            )
                        }
                        return@launch
                    }
                    is UserIdCheckResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                userIdAvailabilityState = UserIdAvailabilityState.ERROR,
                                userIdErrorMessage = "Unable to check User ID. Please try again.",
                                errorMessage = "Unable to check User ID. Please try again."
                            )
                        }
                        return@launch
                    }
                    is UserIdCheckResult.Available -> {
                        // Availability confirmed, proceed
                    }
                }

                // 2. Create Firebase Auth user
                val authResult = auth.createUserWithEmailAndPassword(trimmedEmail, pass).await()
                val user = authResult.user ?: throw IllegalStateException("User creation failed")

                // 3. Atomically register user profile and username mapping in Firestore
                val profileResult = repository.registerUserProfile(trimmedName, trimmedEmail, trimmedUserId)
                if (profileResult.isSuccess) {
                    val profile = profileResult.getOrNull()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentUser = user,
                            userProfile = profile,
                            infoMessage = "Account created successfully!"
                        )
                    }
                    onSuccess()
                } else {
                    val ex = profileResult.exceptionOrNull()
                    val errorMsg = if (ex?.message?.contains("already claimed", ignoreCase = true) == true) {
                        "User ID '$trimmedUserId' already taken. Choose another."
                    } else {
                        "Profile creation failed: ${ex?.localizedMessage ?: "Unknown error"}"
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = errorMsg
                        )
                    }
                }
            } catch (e: FirebaseAuthUserCollisionException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "An account with this email already exists."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Signup failed. Please try again."
                    )
                }
            }
        }
    }

    fun login(email: String, pass: String, onSuccess: () -> Unit) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your email.") }
            return
        }
        if (pass.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your password.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val authResult = auth.signInWithEmailAndPassword(trimmedEmail, pass).await()
                val user = authResult.user
                if (user != null) {
                    val profile = repository.getUserProfile(user.uid)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentUser = user,
                            userProfile = profile
                        )
                    }
                    onSuccess()
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Sign in failed.") }
                }
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Invalid email or password. Please try again."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Login failed. Please check your network and credentials."
                    )
                }
            }
        }
    }

    fun forgotPassword(email: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid email address.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                auth.sendPasswordResetEmail(trimmedEmail).await()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        infoMessage = "Password reset instructions sent to $trimmedEmail."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Failed to send reset email."
                    )
                }
            }
        }
    }

    fun updateName(newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val res = repository.updateUserProfileName(newName)
            if (res.isSuccess) {
                val user = auth.currentUser
                if (user != null) loadProfile(user.uid)
                _uiState.update { it.copy(isLoading = false, infoMessage = "Name updated successfully.") }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = res.exceptionOrNull()?.message ?: "Failed to update name."
                    )
                }
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _uiState.update {
            AuthUiState(currentUser = null, userProfile = null)
        }
    }
}
