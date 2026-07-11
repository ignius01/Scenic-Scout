package com.example.domain

/**
 * A generic sealed interface representing UI state boundaries for asynchronous operations.
 * 
 * Centralizing UI states ensures structured handling of network loading, failures, and content states.
 */
sealed interface UiState<out T> {
    object Idle : UiState<Nothing>
    object Loading : UiState<Nothing>
    data class Success<out T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
