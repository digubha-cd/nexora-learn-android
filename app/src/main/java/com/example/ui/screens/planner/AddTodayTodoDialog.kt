package com.example.ui.screens.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.TaskPriority
import com.example.core.repository.StudyPlannerRepository
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTodayTodoDialog(
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var isLoading by remember { mutableStateOf(false) }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    AlertDialog(
        onDismissRequest = {
            keyboardController?.hide()
            onDismiss()
        },
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        containerColor = NexoraSurface.copy(alpha = 0.95f), // Glassy effect
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.WbSunny, null, tint = NexoraCyan)
                    Text("Today's To-Do", fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = {
                    keyboardController?.hide()
                    onDismiss()
                }) { Icon(Icons.Outlined.Close, null) }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Task Title *") }, modifier = Modifier.fillMaxWidth())
                Text("Priority", fontSize = 12.sp, color = NexoraTextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskPriority.values().forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank() || isLoading) return@Button
                    isLoading = true
                    StudyPlannerRepository.addTask(
                        title = title,
                        scheduledDate = StudyPlannerRepository.TODAY,
                        priority = priority,
                        context = context
                    )
                    keyboardController?.hide()
                    onSave()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NexoraCyan)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                else Text("Save Task")
            }
        }
    )
}
