package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.StudentProfile
import com.example.core.storage.StudentPreferences
import com.example.core.supabase.SupabaseClientManager
import com.example.core.supabase.model.SupabaseStudentProfile
import com.example.ui.animation.bounceClick
import com.example.ui.components.NexoraAvatar
import com.example.ui.theme.*
import androidx.compose.material.icons.filled.EmojiEvents
import com.example.ui.components.ComingSoonFeatureCard
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import java.text.NumberFormat
import java.util.Locale

enum class LeaderboardFilter {
    COMPETITION_POINTS,
    STUDY_HOURS
}

@Composable
fun LeaderboardScreen(
    onBackClick: () -> Unit
) {
    BackHandler {
        onBackClick()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("leaderboard_screen"),
        containerColor = Color(0xFF070B18),
        contentWindowInsets = WindowInsets.systemBars
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0xFF0F172A), CircleShape)
                        .border(BorderStroke(1.dp, Color(0xFF1E293B)), CircleShape)
                        .bounceClick { onBackClick() }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFFF1F5F9),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Leaderboard",
                    color = Color(0xFFF8FAFC),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }

            // Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
            ) {
                // Main Hero Section
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Ambient Hero Container with Glowing Rings
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(120.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFFFFD700).copy(alpha = 0.25f),
                                                NexoraPurple.copy(alpha = 0.15f),
                                                NexoraCyan.copy(alpha = 0.08f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                            Surface(
                                modifier = Modifier.size(92.dp),
                                shape = CircleShape,
                                color = Color(0xFF0D1426),
                                border = BorderStroke(
                                    width = 1.5.dp,
                                    brush = Brush.linearGradient(
                                        listOf(
                                            Color(0xFFFFD700),
                                            Color(0xFFFFA500),
                                            NexoraCyan
                                        )
                                    )
                                )
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.EmojiEvents,
                                        contentDescription = "Leaderboard",
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(46.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // "NEXT UPDATE" Badge
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF1E180B),
                            border = BorderStroke(
                                1.dp,
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFFFFD700).copy(alpha = 0.7f),
                                        NexoraPurple.copy(alpha = 0.6f)
                                    )
                                )
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "🏆",
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "NEXT UPDATE",
                                    color = Color(0xFFFFD700),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Heading
                        Text(
                            text = "Coming Soon",
                            color = Color(0xFFF8FAFC),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            letterSpacing = (-0.5).sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Subtitle
                        Text(
                            text = "Compete with classmates and climb the rankings.",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.5.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "WHAT TO EXPECT",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    )
                }

                // Feature Preview Cards
                item {
                    ComingSoonFeatureCard(
                        icon = "🏆",
                        title = "Top 3 Podium Honors",
                        description = "Climb to the top with special gold, silver, and bronze crowns celebrating the top performers.",
                        accentColor = Color(0xFFFFD700)
                    )
                }

                item {
                    ComingSoonFeatureCard(
                        icon = "⚡",
                        title = "Streak & Points Multipliers",
                        description = "Earn bonus competition points for daily study streaks, focused hours, and chapter mastery.",
                        accentColor = NexoraCyan
                    )
                }

                item {
                    ComingSoonFeatureCard(
                        icon = "🎖️",
                        title = "Gujarat Board Scholar Rankings",
                        description = "Compare your performance across Class 12 Commerce subjects: Accountancy, Economics, B.A., and English.",
                        accentColor = NexoraPurple
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onBackClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .bounceClick { onBackClick() },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF00E5FF), Color(0xFF0070F3))
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Back to Study Suite",
                                color = Color(0xFF050814),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Preserved existing full Leaderboard implementation with Supabase queries,
 * podium rendering, top 10 rankings, and student profile inspection.
 */
@Composable
fun LeaderboardScreenLegacy(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val localProfile = remember { StudentPreferences.getStudentProfile(context) }
    val currentUserId = remember { SupabaseClientManager.getCurrentUserId() ?: localProfile.userId }

    // Selected sort metric: Total Points by default
    var selectedFilter by remember { mutableStateOf(LeaderboardFilter.COMPETITION_POINTS) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var studentList by remember { mutableStateOf<List<StudentProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedProfileForDetail by remember { mutableStateOf<Pair<StudentProfile, Int?>?>(null) }

    LaunchedEffect(selectedFilter, refreshTrigger) {
        isLoading = true
        errorMessage = null
        try {
            val client = SupabaseClientManager.client
            if (client != null && SupabaseClientManager.isConfigured(context)) {
                val primarySortColumn = if (selectedFilter == LeaderboardFilter.STUDY_HOURS) {
                    "total_focused_minutes"
                } else {
                    "competition_points"
                }
                val secondarySortColumn = if (selectedFilter == LeaderboardFilter.STUDY_HOURS) {
                    "competition_points"
                } else {
                    "total_focused_minutes"
                }

                val remoteDtos = client.from("student_profiles")
                    .select {
                        order(primarySortColumn, Order.DESCENDING)
                        order(secondarySortColumn, Order.DESCENDING)
                        limit(100)
                    }
                    .decodeList<SupabaseStudentProfile>()

                val realRemoteProfiles = remoteDtos.map { it.toDomain() }
                    .filter { it.competitionPoints > 0 || it.totalFocusedMinutes > 0 }

                // Include local student only if they have real recorded study activity
                val hasLocalActivity = localProfile.competitionPoints > 0 || localProfile.totalFocusedMinutes > 0
                val combined = if (hasLocalActivity && realRemoteProfiles.none { it.userId == currentUserId || it.studentId == localProfile.studentId }) {
                    realRemoteProfiles + localProfile
                } else {
                    realRemoteProfiles
                }

                // Strictly sort by selected metric
                val sorted = if (selectedFilter == LeaderboardFilter.STUDY_HOURS) {
                    combined.sortedWith(
                        compareByDescending<StudentProfile> { it.totalFocusedMinutes }
                            .thenByDescending { it.competitionPoints }
                    )
                } else {
                    combined.sortedWith(
                        compareByDescending<StudentProfile> { it.competitionPoints }
                            .thenByDescending { it.totalFocusedMinutes }
                    )
                }

                studentList = sorted.distinctBy { it.userId.ifBlank { it.studentId } }
            } else {
                studentList = if (localProfile.competitionPoints > 0 || localProfile.totalFocusedMinutes > 0) listOf(localProfile) else emptyList()
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            errorMessage = "Unable to load online leaderboard: ${e.message}"
            studentList = if (localProfile.competitionPoints > 0 || localProfile.totalFocusedMinutes > 0) listOf(localProfile) else emptyList()
        } finally {
            isLoading = false
        }
    }

    BackHandler {
        if (selectedProfileForDetail != null) {
            selectedProfileForDetail = null
        } else {
            onBackClick()
        }
    }

    if (selectedProfileForDetail != null) {
        StudentProfileDetailScreen(
            profile = selectedProfileForDetail!!.first,
            rank = selectedProfileForDetail!!.second,
            onBackClick = { selectedProfileForDetail = null }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B18))
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("leaderboard_screen")
    ) {
        // Subtle ambient luxury glows
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x1800E5FF),
                        Color(0x0606B6D4),
                        Color.Transparent
                    ),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.12f),
                    radius = size.width * 0.65f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x158B5CF6),
                        Color(0x057C3AED),
                        Color.Transparent
                    ),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.88f, size.height * 0.40f),
                    radius = size.width * 0.55f
                )
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Compact Luxury Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF0D1426), CircleShape)
                        .border(BorderStroke(1.dp, Color(0xFF1E293B)), CircleShape)
                        .bounceClick { onBackClick() }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Leaderboard",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Class 12 Commerce Rankings",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                IconButton(
                    onClick = { refreshTrigger++ },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF0D1426), CircleShape)
                        .border(BorderStroke(1.dp, Color(0xFF1E293B)), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Refresh",
                        tint = NexoraCyan,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // 2. Refined Segmented Control: [ ⭐ Total Points ] [ 📚 Study Hours ]
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0D1426),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Option 1: Total Points
                    val isPointsSelected = selectedFilter == LeaderboardFilter.COMPETITION_POINTS
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedFilter = LeaderboardFilter.COMPETITION_POINTS },
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isPointsSelected) {
                                        Modifier.background(
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFF00E5FF), Color(0xFF0070F3))
                                            )
                                        )
                                    } else {
                                        Modifier.background(Color.Transparent)
                                    }
                                )
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⭐ Total Points",
                                fontSize = 12.5.sp,
                                fontWeight = if (isPointsSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isPointsSelected) Color(0xFF050814) else Color(0xFF94A3B8)
                            )
                        }
                    }

                    // Option 2: Study Hours
                    val isHoursSelected = selectedFilter == LeaderboardFilter.STUDY_HOURS
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedFilter = LeaderboardFilter.STUDY_HOURS },
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isHoursSelected) {
                                        Modifier.background(
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFF00E5FF), Color(0xFF0070F3))
                                            )
                                        )
                                    } else {
                                        Modifier.background(Color.Transparent)
                                    }
                                )
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "📚 Study Hours",
                                fontSize = 12.5.sp,
                                fontWeight = if (isHoursSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isHoursSelected) Color(0xFF050814) else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = NexoraCyan, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Loading Leaderboard...",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                    }
                }
            } else if (studentList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            color = Color(0xFF0D1426),
                            border = BorderStroke(1.dp, Color(0xFF1E293B))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = "🏆", fontSize = 32.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "No leaderboard data yet. Start studying to appear here!",
                            color = Color(0xFFCBD5E1),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Complete study sessions, solve quizzes, and finish chapters to earn competition points.",
                            color = Color(0xFF64748B),
                            fontSize = 12.5.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val uniqueStudents = studentList.distinctBy { it.userId.ifBlank { it.studentId } }
                val top10Students = uniqueStudents.take(10)
                val otherRankStudents = if (uniqueStudents.size > 10) uniqueStudents.drop(10) else emptyList()

                val currentUserRank = uniqueStudents.indexOfFirst {
                    it.userId == currentUserId || it.studentId == localProfile.studentId
                } + 1

                // First, Second, Third students for Podium
                val firstStudent = uniqueStudents.getOrNull(0)
                val secondStudent = uniqueStudents.getOrNull(1)
                val thirdStudent = uniqueStudents.getOrNull(2)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 3. TOP 3 LUXURY PODIUM (Always complete 3-podium layout)
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Subtitle above podium
                            Text(
                                text = "ELITE SCHOLARS PODIUM",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp,
                                color = Color(0xFFF59E0B),
                                modifier = Modifier
                                    .align(Alignment.Start)
                                    .padding(bottom = 12.dp)
                            )

                            // 3 Podium Columns Side-by-Side: Rank 2 on Left, Rank 1 Center, Rank 3 on Right
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                // Rank 2 (Silver) on Left
                                PodiumColumn(
                                    profile = secondStudent,
                                    rank = 2,
                                    crownEmoji = "🥈",
                                    accentColor = Color(0xFFCBD5E1),
                                    badgeBorderColor = Color(0xFF94A3B8),
                                    pedestalHeight = 28.dp,
                                    pedestalLabel = "2nd",
                                    isCurrentStudent = secondStudent != null && (secondStudent.userId == currentUserId || secondStudent.studentId == localProfile.studentId),
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        secondStudent?.let { selectedProfileForDetail = Pair(it, 2) }
                                    }
                                )

                                // Rank 1 (Gold) in Center (Elevated)
                                PodiumColumn(
                                    profile = firstStudent,
                                    rank = 1,
                                    crownEmoji = "👑",
                                    accentColor = Color(0xFFF59E0B),
                                    badgeBorderColor = Color(0xFFFDE68A),
                                    pedestalHeight = 44.dp,
                                    pedestalLabel = "1st",
                                    isCurrentStudent = firstStudent != null && (firstStudent.userId == currentUserId || firstStudent.studentId == localProfile.studentId),
                                    modifier = Modifier.weight(1.15f),
                                    onClick = {
                                        firstStudent?.let { selectedProfileForDetail = Pair(it, 1) }
                                    }
                                )

                                // Rank 3 (Bronze) on Right
                                PodiumColumn(
                                    profile = thirdStudent,
                                    rank = 3,
                                    crownEmoji = "🥉",
                                    accentColor = Color(0xFFD97706),
                                    badgeBorderColor = Color(0xFFFDBA74),
                                    pedestalHeight = 20.dp,
                                    pedestalLabel = "3rd",
                                    isCurrentStudent = thirdStudent != null && (thirdStudent.userId == currentUserId || thirdStudent.studentId == localProfile.studentId),
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        thirdStudent?.let { selectedProfileForDetail = Pair(it, 3) }
                                    }
                                )
                            }
                        }
                    }

                    // 4. TOP 10 LEADERBOARD SECTION HEADER
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "TOP 10 LEADERBOARD",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "The top performers",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = Color(0x2200E5FF),
                                    border = BorderStroke(1.dp, Color(0x4400E5FF))
                                ) {
                                    Text(
                                        text = "Top 10",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NexoraCyan,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    // TOP 10 ROWS (Ranks 1 to 10)
                    itemsIndexed(top10Students) { index, student ->
                        val rank = index + 1
                        val isCurrent = student.userId == currentUserId || student.studentId == localProfile.studentId

                        LeaderboardRowItem(
                            rank = rank,
                            student = student,
                            isCurrentStudent = isCurrent,
                            onClick = { selectedProfileForDetail = Pair(student, rank) }
                        )
                    }

                    // 5. OTHER RANKS SECTION (Ranks 11 and below)
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "OTHER RANKS",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Keep learning. Keep climbing.",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = Color(0xFF1E293B)
                                ) {
                                    Text(
                                        text = "Ranks 11+",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF94A3B8),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (otherRankStudents.isNotEmpty()) {
                        itemsIndexed(otherRankStudents) { index, student ->
                            val rank = index + 11
                            val isCurrent = student.userId == currentUserId || student.studentId == localProfile.studentId

                            LeaderboardRowItem(
                                rank = rank,
                                student = student,
                                isCurrentStudent = isCurrent,
                                onClick = { selectedProfileForDetail = Pair(student, rank) }
                            )
                        }
                    } else {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF0D1426),
                                border = BorderStroke(1.dp, Color(0xFF1E293B))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🎯", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Ranks #11 and beyond",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Will appear dynamically as more Class 12 students participate.",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Pinned student card if the logged-in student is ranked beyond the loaded list
                    if (currentUserRank > uniqueStudents.size || currentUserRank <= 0) {
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "YOUR POSITION",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                color = NexoraCyan
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LeaderboardRowItem(
                                rank = if (currentUserRank > 0) currentUserRank else (uniqueStudents.size + 1),
                                student = localProfile,
                                isCurrentStudent = true,
                                onClick = { selectedProfileForDetail = Pair(localProfile, null) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(36.dp))
                    }
                }
            }
        }
    }
}

private fun formatStudyHours(minutes: Int): String {
    if (minutes <= 0) return "0.0 study hours"
    val hours = minutes / 60.0
    return String.format(Locale.US, "%.1f study hours", hours)
}

private fun formatPoints(points: Int): String {
    return NumberFormat.getIntegerInstance(Locale.US).format(points) + " points"
}

@Composable
private fun PodiumColumn(
    profile: StudentProfile?,
    rank: Int,
    crownEmoji: String,
    accentColor: Color,
    badgeBorderColor: Color,
    pedestalHeight: Dp,
    pedestalLabel: String,
    isCurrentStudent: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = profile != null, onClick = onClick)
            .shadow(
                elevation = if (rank == 1) 12.dp else 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = accentColor.copy(alpha = 0.35f),
                spotColor = accentColor.copy(alpha = 0.35f)
            ),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF0D1426),
        border = BorderStroke(
            width = if (rank == 1) 1.5.dp else 1.dp,
            color = if (isCurrentStudent) NexoraCyan else accentColor.copy(alpha = 0.65f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Crown / Medal Icon
            Text(
                text = crownEmoji,
                fontSize = if (rank == 1) 24.sp else 18.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )

            // Avatar or Position-Open Placeholder
            if (profile != null) {
                Box(contentAlignment = Alignment.Center) {
                    NexoraAvatar(
                        photoUrl = profile.photoUrl,
                        name = profile.studentName,
                        avatarColorHex = profile.avatarColorHex,
                        size = if (rank == 1) 54.dp else 44.dp
                    )
                }
            } else {
                Surface(
                    modifier = Modifier.size(if (rank == 1) 54.dp else 44.dp),
                    shape = CircleShape,
                    color = Color(0xFF070B18),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = accentColor.copy(alpha = 0.5f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Rank Pill Badge
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = accentColor,
                border = BorderStroke(1.dp, badgeBorderColor)
            ) {
                Text(
                    text = "#$rank",
                    color = Color(0xFF050814),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.5.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Student Name or Open Position
            Text(
                text = profile?.studentName?.ifBlank { "Scholar" } ?: "Position Open",
                fontSize = if (rank == 1) 13.sp else 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (profile != null) Color.White else Color(0xFF94A3B8),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            if (isCurrentStudent) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = NexoraCyan,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = "YOU",
                        color = Color(0xFF050814),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Stats or Subtitle
            if (profile != null) {
                Text(
                    text = "⭐ ${formatPoints(profile.competitionPoints)}",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor,
                    maxLines = 1
                )
                Text(
                    text = "📚 ${formatStudyHours(profile.totalFocusedMinutes)}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF94A3B8),
                    maxLines = 1
                )
            } else {
                Text(
                    text = "Claim Rank #$rank",
                    fontSize = 10.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pedestal Block at base
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(pedestalHeight),
                shape = RoundedCornerShape(8.dp),
                color = accentColor.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = pedestalLabel,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        color = accentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRowItem(
    rank: Int,
    student: StudentProfile,
    isCurrentStudent: Boolean,
    onClick: () -> Unit
) {
    val rankBadgeColor = when (rank) {
        1 -> Color(0xFFF59E0B)
        2 -> Color(0xFFCBD5E1)
        3 -> Color(0xFFD97706)
        in 4..10 -> NexoraCyan
        else -> Color(0xFF64748B)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isCurrentStudent) Color(0xFF0A1E35) else Color(0xFF0D1426),
        border = BorderStroke(
            width = if (isCurrentStudent) 1.5.dp else 1.dp,
            color = if (isCurrentStudent) NexoraCyan else Color(0xFF1E293B)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Badge: Gold for #1, Silver for #2, Bronze for #3, Neutral for others
            Surface(
                modifier = Modifier.size(34.dp),
                shape = CircleShape,
                color = if (rank in 1..3) rankBadgeColor else Color(0xFF070B18),
                border = BorderStroke(
                    1.dp,
                    if (rank in 1..3) Color.Transparent else rankBadgeColor.copy(alpha = 0.45f)
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (rank in 1..99) "#$rank" else "#99+",
                        fontWeight = FontWeight.ExtraBold,
                        color = if (rank in 1..3) Color(0xFF050814) else rankBadgeColor,
                        fontSize = if (rank >= 100) 9.sp else 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Circular Avatar
            NexoraAvatar(
                photoUrl = student.photoUrl,
                name = student.studentName,
                avatarColorHex = student.avatarColorHex,
                size = 40.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Student Info and Compact Combined Stats: ⭐ 1,250 points  •  📚 24.5 study hours
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = student.studentName.ifBlank { "Scholar" },
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isCurrentStudent) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = NexoraCyan
                        ) {
                            Text(
                                text = "YOU",
                                color = Color(0xFF050814),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Compact Combined Stats Area
                Text(
                    text = "⭐ ${formatPoints(student.competitionPoints)}  •  📚 ${formatStudyHours(student.totalFocusedMinutes)}",
                    color = NexoraCyan,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Streak indicator badge on right
            if (student.studyStreakDays > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B)
                ) {
                    Text(
                        text = "🔥 ${student.studyStreakDays}d",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}
