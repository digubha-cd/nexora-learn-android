package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.StudentProfile
import com.example.core.model.Subject
import com.example.core.repository.StudyPlannerRepository
import com.example.core.repository.SyllabusRepository
import com.example.core.timer.StudyTimerManager
import com.example.ui.components.GlobalPersistentTimerBar
import com.example.ui.components.NexoraBottomNavigationBar
import com.example.ui.components.NexoraTab
import com.example.ui.theme.NexoraBackground
import com.example.core.storage.StudentPreferences
import com.example.ui.screens.planner.TodoListScreen
import com.example.ui.screens.planner.AddChapterTodoScreen
import com.example.ui.screens.planner.CustomTodoScreen
import com.example.ui.screens.planner.PersonalScheduleScreen
import com.example.ui.screens.planner.TodayTodoScreen

/**
 * Navigation state for full-screen overlay pages:
 * - None (Regular Tab views with bottom bar)
 * - MarksCalculator
 * - Login
 * - NexoraAi
 * - TodoList
 * - AddChapterTodo
 * - CustomTodo
 * - PersonalSchedule
 * - TodayTodo
 */
sealed interface FullScreenRoute {
    data object None : FullScreenRoute
    data object MarksCalculator : FullScreenRoute
    data object Login : FullScreenRoute
    data object NexoraAi : FullScreenRoute
    data object TodoList : FullScreenRoute
    data object AddChapterTodo : FullScreenRoute
    data object CustomTodo : FullScreenRoute
    data object PersonalSchedule : FullScreenRoute
    data object TodayTodo : FullScreenRoute
    data object Friends : FullScreenRoute
    data object Chat : FullScreenRoute
    data object Leaderboard : FullScreenRoute
    data object EditProfile : FullScreenRoute
    data object Roadmap : FullScreenRoute
    data object TaskWeeklyBreakdown : FullScreenRoute
}

/**
 * Main application screen with clean 5-tab bottom navigation:
 * - Home
 * - Subjects
 * - Planner
 * - Exam
 * - Profile
 */
@Composable
fun MainAppScreen(
    studentProfile: StudentProfile,
    onOpenSettings: () -> Unit = {},
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MainAppViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val currentTab = viewModel.currentTab
    val selectedSubject = viewModel.selectedSubject
    val fullScreenBackStack = viewModel.fullScreenBackStack
    val fullScreenRoute = fullScreenBackStack.lastOrNull() ?: FullScreenRoute.None

    // Root System Back Navigation Handling across tabs and full-screen routes
    val canGoBack = selectedSubject != null || fullScreenBackStack.isNotEmpty() || currentTab != NexoraTab.HOME

    androidx.activity.compose.BackHandler(enabled = canGoBack) {
        when {
            selectedSubject != null -> {
                viewModel.selectedSubject = null
            }
            fullScreenBackStack.isNotEmpty() -> {
                viewModel.popFullScreenRoute()
            }
            currentTab != NexoraTab.HOME -> {
                viewModel.currentTab = NexoraTab.HOME
            }
        }
    }

    val timerState by StudyTimerManager.timerState.collectAsStateWithLifecycle()
    val isFullScreenActive = selectedSubject != null || fullScreenRoute !is FullScreenRoute.None
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("main_app_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            // Bottom navigation bar is ONLY visible when on normal tab screens
            if (!isFullScreenActive) {
                Column {
                    // Floating persistent timer bar when timer is running/paused and not currently viewing the Planner tab
                    if ((timerState.isRunning || timerState.isPaused) && currentTab != NexoraTab.PLANNER) {
                        GlobalPersistentTimerBar(
                            timerState = timerState,
                            onPauseTimer = { StudyTimerManager.pauseTimer() },
                            onResumeTimer = { StudyTimerManager.resumeTimer() },
                            onClickBar = {
                                viewModel.currentTab = NexoraTab.PLANNER
                                viewModel.selectedSubject = null
                                viewModel.clearFullScreenRoutes()
                            }
                        )
                    }

                    NexoraBottomNavigationBar(
                        currentTab = currentTab,
                        onTabSelected = { tab ->
                            viewModel.currentTab = tab
                            viewModel.selectedSubject = null
                            viewModel.clearFullScreenRoutes()
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        val isGuest = remember { StudentPreferences.isGuestMode(context) }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(if (isFullScreenActive) androidx.compose.foundation.layout.PaddingValues() else innerPadding)
        ) {
            // 1. Full-screen Subject Detail View
            if (selectedSubject != null) {
                SubjectDetailScreen(
                    subject = selectedSubject!!,
                    onBackClick = { viewModel.selectedSubject = null },
                    onAddChapterTodo = { viewModel.navigateToFullScreen(FullScreenRoute.AddChapterTodo) }
                )
            }
            // 2. Full-screen Overlay Pages
            else if (fullScreenRoute !is FullScreenRoute.None) {
                when (fullScreenRoute) {
                    is FullScreenRoute.MarksCalculator -> {
                        MarksCalculatorScreen(
                            onBackClick = { viewModel.popFullScreenRoute() }
                        )
                    }
                    is FullScreenRoute.Login -> {
                        com.example.ui.screens.auth.LoginScreen(
                            onLoginSuccess = { viewModel.popFullScreenRoute() }
                        )
                    }
                    is FullScreenRoute.NexoraAi -> {
                        com.example.ui.screens.NexoraAiChatScreen(
                            onBackClick = { viewModel.popFullScreenRoute() }
                        )
                    }
                    is FullScreenRoute.TodoList -> {
                        TodoListScreen(
                            onBackClick = { viewModel.popFullScreenRoute() },
                            onNavigateToTodayTodo = { viewModel.navigateToFullScreen(FullScreenRoute.TodayTodo) },
                            onNavigateToAddChapterTodo = { viewModel.navigateToFullScreen(FullScreenRoute.AddChapterTodo) },
                            onNavigateToCustomTodo = { viewModel.navigateToFullScreen(FullScreenRoute.CustomTodo) },
                            onNavigateToPersonalSchedule = { viewModel.navigateToFullScreen(FullScreenRoute.PersonalSchedule) },
                            onNavigateToBreakdown = { viewModel.navigateToFullScreen(FullScreenRoute.TaskWeeklyBreakdown) }
                        )
                    }
                    is FullScreenRoute.TaskWeeklyBreakdown -> {
                        com.example.ui.screens.planner.TaskWeeklyBreakdownScreen(
                            onBackClick = { viewModel.popFullScreenRoute() }
                        )
                    }
                    is FullScreenRoute.AddChapterTodo -> {
                        AddChapterTodoScreen(
                            onBackClick = { viewModel.popFullScreenRoute() }
                        )
                    }
                    is FullScreenRoute.CustomTodo -> {
                        CustomTodoScreen(
                            onBackClick = { viewModel.popFullScreenRoute() }
                        )
                    }
                    is FullScreenRoute.PersonalSchedule -> {
                        PersonalScheduleScreen(
                            onBackClick = { viewModel.popFullScreenRoute() }
                        )
                    }
                    is FullScreenRoute.TodayTodo -> {
                        TodayTodoScreen(
                            onBackClick = { viewModel.popFullScreenRoute() }
                        )
                    }
                    is FullScreenRoute.Friends -> {
                        FriendsScreen(
                            onBackClick = { viewModel.popFullScreenRoute() }
                        )
                    }
                    is FullScreenRoute.Chat -> {
                        ChatScreen(
                            onBackClick = { viewModel.popFullScreenRoute() }
                        )
                    }
                    is FullScreenRoute.Leaderboard -> {
                        LeaderboardScreen(
                            onBackClick = { viewModel.popFullScreenRoute() }
                        )
                    }
                    is FullScreenRoute.EditProfile -> {
                        EditProfileScreen(
                            studentProfile = studentProfile,
                            onBackClick = { viewModel.popFullScreenRoute() }
                        )
                    }
                    is FullScreenRoute.Roadmap -> {
                        RoadmapScreen(
                            onBackClick = { viewModel.popFullScreenRoute() },
                            onSubjectClick = { subject ->
                                viewModel.selectedSubject = subject
                                viewModel.popFullScreenRoute()
                            }
                        )
                    }
                    else -> Unit
                }
            }
            // 3. Normal Bottom Navigation Tab Content
            else {
                Crossfade(
                    targetState = currentTab,
                    label = "tab_transition"
                ) { tab ->
                    when (tab) {
                        NexoraTab.HOME -> {
                            HomeScreen(
                                studentProfile = studentProfile,
                                onSubjectClick = { subject ->
                                    viewModel.selectedSubject = subject
                                    viewModel.currentTab = NexoraTab.SUBJECTS
                                },
                                onNavigateToSubjects = {
                                    viewModel.currentTab = NexoraTab.SUBJECTS
                                },
                                onNavigateToPlanner = {
                                    viewModel.currentTab = NexoraTab.PLANNER
                                },
                                onOpenMarksCalculator = {
                                    viewModel.navigateToFullScreen(FullScreenRoute.MarksCalculator)
                                },
                                onOpenNexoraAi = {
                                    viewModel.navigateToFullScreen(FullScreenRoute.NexoraAi)
                                },
                                onOpenFriends = {
                                    viewModel.navigateToFullScreen(FullScreenRoute.Friends)
                                },
                                onOpenChat = {
                                    viewModel.navigateToFullScreen(FullScreenRoute.Chat)
                                },
                                onOpenLeaderboard = {
                                    viewModel.navigateToFullScreen(FullScreenRoute.Leaderboard)
                                },
                                onOpenSettings = onOpenSettings,
                                onNavigateToProfile = {
                                    viewModel.currentTab = NexoraTab.PROFILE
                                },
                                onNavigateToRoadmap = {
                                    viewModel.navigateToFullScreen(FullScreenRoute.Roadmap)
                                }
                            )
                        }
                        NexoraTab.PLANNER -> {
                            StudyPlannerScreen(
                                onNavigateToSubjects = {
                                    viewModel.currentTab = NexoraTab.SUBJECTS
                                },
                                onOpenTodoList = {
                                    viewModel.navigateToFullScreen(FullScreenRoute.TodoList)
                                }
                            )
                        }
                        NexoraTab.SUBJECTS -> {
                            SubjectsScreen(
                                onSubjectClick = { subject ->
                                    viewModel.selectedSubject = subject
                                },
                                onOpenMarksCalculator = {
                                    viewModel.navigateToFullScreen(FullScreenRoute.MarksCalculator)
                                }
                            )
                        }
                        NexoraTab.PROFILE -> {
                            ProfileScreen(
                                studentProfile = studentProfile,
                                isGuest = isGuest,
                                onOpenLogin = { viewModel.navigateToFullScreen(FullScreenRoute.Login) },
                                onSignOut = {
                                    onSignOut()
                                },
                                onOpenSettings = onOpenSettings,
                                onOpenEditProfile = { viewModel.navigateToFullScreen(FullScreenRoute.EditProfile) }
                            )
                        }
                    }
                }
            }
        }
    }
}
