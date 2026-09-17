package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.SessionManager
import com.example.data.local.entity.CachedGroupEntity
import com.example.data.remote.model.AdminDashboardStats
import com.example.data.remote.model.GroupDetailsResponse
import com.example.data.remote.model.GroupDto
import com.example.data.repository.GroupTrackRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class GroupActionState {
    object Idle : GroupActionState()
    object Loading : GroupActionState()
    data class Success(val message: String) : GroupActionState()
    data class Error(val message: String) : GroupActionState()
}

class GroupViewModel(
    private val repository: GroupTrackRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val cachedGroups: StateFlow<List<CachedGroupEntity>> = repository.cachedGroupsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeGroupId: StateFlow<String?> = sessionManager.activeGroupIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeGroupName: StateFlow<String?> = sessionManager.activeGroupNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _actionState = MutableStateFlow<GroupActionState>(GroupActionState.Idle)
    val actionState: StateFlow<GroupActionState> = _actionState.asStateFlow()

    private val _selectedGroupDetails = MutableStateFlow<GroupDetailsResponse?>(null)
    val selectedGroupDetails: StateFlow<GroupDetailsResponse?> = _selectedGroupDetails.asStateFlow()

    private val _adminStats = MutableStateFlow<AdminDashboardStats?>(null)
    val adminStats: StateFlow<AdminDashboardStats?> = _adminStats.asStateFlow()

    init {
        refreshGroups()
    }

    fun refreshGroups() {
        viewModelScope.launch {
            repository.fetchGroups()
        }
    }

    fun selectActiveGroup(groupId: String, groupName: String) {
        viewModelScope.launch {
            sessionManager.setActiveGroup(groupId, groupName)
        }
    }

    fun createGroup(name: String, description: String?) {
        if (name.isBlank()) {
            _actionState.value = GroupActionState.Error("Group name is required")
            return
        }

        viewModelScope.launch {
            _actionState.value = GroupActionState.Loading
            val result = repository.createGroup(name.trim(), description?.trim())
            result.onSuccess {
                sessionManager.setActiveGroup(it.id, it.name)
                _actionState.value = GroupActionState.Success("Group '${it.name}' created! Invite code: ${it.inviteCode}")
            }.onFailure {
                _actionState.value = GroupActionState.Error(it.message ?: "Failed to create group")
            }
        }
    }

    fun joinGroup(inviteCode: String) {
        if (inviteCode.isBlank()) {
            _actionState.value = GroupActionState.Error("Please enter an invite code")
            return
        }

        viewModelScope.launch {
            _actionState.value = GroupActionState.Loading
            val result = repository.joinGroup(inviteCode.trim())
            result.onSuccess {
                _actionState.value = GroupActionState.Success("Successfully joined group!")
            }.onFailure {
                _actionState.value = GroupActionState.Error(it.message ?: "Could not join group")
            }
        }
    }

    fun loadGroupDetails(groupId: String) {
        viewModelScope.launch {
            val result = repository.getGroupDetails(groupId)
            result.onSuccess {
                _selectedGroupDetails.value = it
            }
        }
    }

    fun loadAdminStats(groupId: String) {
        viewModelScope.launch {
            val result = repository.getAdminDashboard(groupId)
            result.onSuccess {
                _adminStats.value = it
            }
        }
    }

    fun approveMember(groupId: String, userId: String) {
        viewModelScope.launch {
            val result = repository.approveMember(groupId, userId)
            result.onSuccess {
                loadGroupDetails(groupId)
            }
        }
    }

    fun removeMember(groupId: String, userId: String) {
        viewModelScope.launch {
            val result = repository.removeMember(groupId, userId)
            result.onSuccess {
                loadGroupDetails(groupId)
                refreshGroups()
            }
        }
    }

    fun clearActionState() {
        _actionState.value = GroupActionState.Idle
    }

    class Factory(
        private val repository: GroupTrackRepository,
        private val sessionManager: SessionManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GroupViewModel(repository, sessionManager) as T
        }
    }
}
