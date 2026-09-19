package ru.finni.app.ui.pet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.finni.core.data.ProfileRepository
import ru.finni.core.economy.RewardEngine
import javax.inject.Inject

data class CreatePetUiState(
    val childName: String = "",
    val petName: String = "",
    val body: Int = 0,
    /** Возрастная группа: 0 — 7–9 лет, 1 — 10–11 лет. */
    val ageGroup: Int = 0,
    val created: Boolean = false,
) {
    val canCreate: Boolean get() = petName.isNotBlank()
}

@HiltViewModel
class CreatePetViewModel @Inject constructor(
    private val repository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePetUiState())
    val uiState: StateFlow<CreatePetUiState> = _uiState

    fun onChildNameChange(value: String) = _uiState.update { it.copy(childName = value) }
    fun onPetNameChange(value: String) = _uiState.update { it.copy(petName = value) }
    fun onBodyChange(index: Int) = _uiState.update { it.copy(body = index) }
    fun onAgeGroupChange(index: Int) = _uiState.update { it.copy(ageGroup = index) }

    fun createProfile() {
        val s = _uiState.value
        if (!s.canCreate) return
        viewModelScope.launch {
            repository.createProfile(
                childName = s.childName.trim().ifBlank { "Игрок" },
                petName = s.petName.trim(),
                petBody = s.body,
                petColor = 0,
                petAccessory = 0,
                ageGroup = s.ageGroup,
                startBalance = RewardEngine.START_BALANCE,
            )
            _uiState.update { it.copy(created = true) }
        }
    }
}
