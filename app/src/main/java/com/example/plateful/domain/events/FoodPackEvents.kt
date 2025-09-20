package com.example.plateful.domain.events

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Global event system for food pack updates
 * This allows different parts of the app to communicate food pack changes
 */
object FoodPackEvents {
    
    private val _foodPackCreated = MutableSharedFlow<String>() // Food pack ID
    val foodPackCreated: SharedFlow<String> = _foodPackCreated.asSharedFlow()
    
    private val _foodPackUpdated = MutableSharedFlow<String>() // Food pack ID
    val foodPackUpdated: SharedFlow<String> = _foodPackUpdated.asSharedFlow()
    
    private val _foodPackDeleted = MutableSharedFlow<String>() // Food pack ID
    val foodPackDeleted: SharedFlow<String> = _foodPackDeleted.asSharedFlow()
    
    private val _foodPacksChanged = MutableSharedFlow<Unit>() // General refresh trigger
    val foodPacksChanged: SharedFlow<Unit> = _foodPacksChanged.asSharedFlow()
    
    /**
     * Emit when a new food pack is created
     */
    suspend fun emitFoodPackCreated(foodPackId: String) {
        _foodPackCreated.emit(foodPackId)
        _foodPacksChanged.emit(Unit) // Also emit general change
    }
    
    /**
     * Emit when a food pack is updated
     */
    suspend fun emitFoodPackUpdated(foodPackId: String) {
        _foodPackUpdated.emit(foodPackId)
        _foodPacksChanged.emit(Unit) // Also emit general change
    }
    
    /**
     * Emit when a food pack is deleted
     */
    suspend fun emitFoodPackDeleted(foodPackId: String) {
        _foodPackDeleted.emit(foodPackId)
        _foodPacksChanged.emit(Unit) // Also emit general change
    }
    
    /**
     * Emit general food packs change event
     */
    suspend fun emitFoodPacksChanged() {
        _foodPacksChanged.emit(Unit)
    }
}
