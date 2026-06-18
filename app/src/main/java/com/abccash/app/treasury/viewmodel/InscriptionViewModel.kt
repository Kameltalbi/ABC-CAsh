package com.abccash.app.treasury.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.abccash.app.treasury.data.Entreprise
import com.abccash.app.treasury.data.User
import com.abccash.app.treasury.data.UserRole
import com.abccash.app.treasury.repository.TreasuryRepository
import com.abccash.app.treasury.security.PasswordHasher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InscriptionUiState(
    val nom: String = "",
    val nomEntreprise: String = "",
    val email: String = "",
    val telephone: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val nomError: String? = null,
    val nomEntrepriseError: String? = null,
    val emailError: String? = null,
    val telephoneError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val generalError: String? = null
)

class InscriptionViewModel(private val repository: TreasuryRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(InscriptionUiState())
    val uiState: StateFlow<InscriptionUiState> = _uiState.asStateFlow()

    var onInscriptionSuccess: ((User) -> Unit)? = null

    fun updateNom(nom: String) {
        _uiState.update { it.copy(nom = nom, nomError = null, generalError = null) }
    }

    fun updateNomEntreprise(nom: String) {
        _uiState.update { it.copy(nomEntreprise = nom, nomEntrepriseError = null, generalError = null) }
    }

    fun updateEmail(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, generalError = null) }
    }

    fun updateTelephone(telephone: String) {
        _uiState.update { it.copy(telephone = telephone, telephoneError = null, generalError = null) }
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null) }
    }

    fun updateConfirmPassword(password: String) {
        _uiState.update { it.copy(confirmPassword = password, confirmPasswordError = null) }
    }

    fun checkEmailAvailability(email: String) {
        if (email.isNotBlank() && isValidEmail(email)) {
            viewModelScope.launch {
                if (repository.isEmailTaken(email)) {
                    _uiState.update { it.copy(emailError = "Cette adresse email est déjà utilisée") }
                }
            }
        }
    }

    fun checkTelephoneAvailability(telephone: String) {
        if (telephone.isNotBlank() && isValidPhone(telephone)) {
            viewModelScope.launch {
                if (repository.isTelephoneTaken(telephone)) {
                    _uiState.update { it.copy(telephoneError = "Ce numéro de téléphone est déjà utilisé") }
                }
            }
        }
    }

    fun inscrire() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            if (!validateFields()) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            val state = _uiState.value
            if (repository.isEmailTaken(state.email)) {
                _uiState.update {
                    it.copy(emailError = "Cette adresse email est déjà associée à un compte", isLoading = false)
                }
                return@launch
            }
            if (repository.isTelephoneTaken(state.telephone)) {
                _uiState.update {
                    it.copy(telephoneError = "Ce numéro est déjà associé à un compte", isLoading = false)
                }
                return@launch
            }

            if (repository.hasAnyUser()) {
                _uiState.update {
                    it.copy(
                        generalError = "Un compte entreprise existe déjà. Contactez votre administrateur.",
                        isLoading = false
                    )
                }
                return@launch
            }

            val entreprise = Entreprise(nom = state.nomEntreprise)
            val user = User(
                nom = state.nom,
                email = state.email.trim(),
                telephone = state.telephone.trim(),
                passwordHash = state.password,
                role = UserRole.ADMIN,
                entrepriseId = entreprise.id
            )
            val registered = repository.registerAdmin(entreprise, user)
            _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            onInscriptionSuccess?.invoke(registered)
        }
    }

    private fun validateFields(): Boolean {
        val state = _uiState.value
        var isValid = true

        if (state.nom.isBlank()) {
            _uiState.update { it.copy(nomError = "Le nom est requis") }
            isValid = false
        }
        if (state.nomEntreprise.isBlank()) {
            _uiState.update { it.copy(nomEntrepriseError = "Le nom de l'entreprise est requis") }
            isValid = false
        }
        if (state.email.isBlank()) {
            _uiState.update { it.copy(emailError = "L'email est requis") }
            isValid = false
        } else if (!isValidEmail(state.email)) {
            _uiState.update { it.copy(emailError = "Format d'email invalide") }
            isValid = false
        }
        if (state.telephone.isBlank()) {
            _uiState.update { it.copy(telephoneError = "Le numéro de téléphone est requis") }
            isValid = false
        } else if (!isValidPhone(state.telephone)) {
            _uiState.update { it.copy(telephoneError = "Format de téléphone invalide") }
            isValid = false
        }
        if (state.password.isBlank()) {
            _uiState.update { it.copy(passwordError = "Le mot de passe est requis") }
            isValid = false
        } else if (state.password.length < 6) {
            _uiState.update { it.copy(passwordError = "Au moins 6 caractères") }
            isValid = false
        }
        if (state.confirmPassword != state.password) {
            _uiState.update { it.copy(confirmPasswordError = "Les mots de passe ne correspondent pas") }
            isValid = false
        }
        return isValid
    }

    private fun isValidEmail(email: String): Boolean =
        android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

    private fun isValidPhone(phone: String): Boolean =
        phone.replace("[^0-9]".toRegex(), "").length >= 8
}

class InscriptionViewModelFactory(private val repository: TreasuryRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InscriptionViewModel::class.java)) {
            return InscriptionViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
