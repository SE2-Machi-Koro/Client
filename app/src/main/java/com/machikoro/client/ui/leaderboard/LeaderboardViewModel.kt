package com.machikoro.client.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.machikoro.client.network.leaderboard.LeaderboardApi
import com.machikoro.client.network.leaderboard.LeaderboardEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class LeaderboardState {
    data object Loading : LeaderboardState()
    data class Success(val entries: List<LeaderboardEntry>) : LeaderboardState()
    data class Error(val message: String) : LeaderboardState()
}

class LeaderboardViewModel(private val api: LeaderboardApi) : ViewModel() {

    private val mutableState = MutableStateFlow<LeaderboardState>(LeaderboardState.Loading)
    val state: StateFlow<LeaderboardState> = mutableState

    fun load() {
        viewModelScope.launch {
            mutableState.value = LeaderboardState.Loading
            try {
                // Filter out players with zero wins per acceptance criteria
                val entries = api.getLeaderboard(limit = 50).filter { it.totalWins > 0 }
                mutableState.value = LeaderboardState.Success(entries)
            } catch (e: Exception) {
                mutableState.value = LeaderboardState.Error(e.message ?: "Failed to load leaderboard")
            }
        }
    }

    class Factory(private val api: LeaderboardApi) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(LeaderboardViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return LeaderboardViewModel(api) as T
        }
    }
}