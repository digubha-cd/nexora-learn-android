package com.example.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.core.model.Subject
import com.example.ui.components.NexoraTab

class MainAppViewModel : ViewModel() {
    var currentTab by mutableStateOf(NexoraTab.HOME)
    var selectedSubject by mutableStateOf<Subject?>(null)
    val fullScreenBackStack = mutableStateListOf<FullScreenRoute>()

    fun navigateToFullScreen(route: FullScreenRoute) {
        if (fullScreenBackStack.lastOrNull() != route) {
            fullScreenBackStack.add(route)
        }
    }

    fun popFullScreenRoute() {
        if (fullScreenBackStack.isNotEmpty()) {
            fullScreenBackStack.removeAt(fullScreenBackStack.lastIndex)
        }
    }

    fun clearFullScreenRoutes() {
        fullScreenBackStack.clear()
    }
}
