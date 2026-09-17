package ru.finni.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import ru.finni.core.data.ProfileEntity
import ru.finni.core.data.ProfileRepository
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    repository: ProfileRepository,
) : ViewModel() {

    val profile: StateFlow<ProfileEntity?> = repository.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
