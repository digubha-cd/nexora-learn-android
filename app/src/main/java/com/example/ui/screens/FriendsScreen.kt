package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.StudentProfile
import com.example.core.storage.StudentPreferences
import com.example.core.supabase.SupabaseAuthRepository
import com.example.core.supabase.SupabaseClientManager
import com.example.ui.animation.bounceClick
import com.example.ui.components.NexoraAvatar
import com.example.ui.components.NexoraCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.People
import androidx.compose.ui.graphics.Brush
import com.example.ui.components.ComingSoonFeatureCard

@Composable
fun FriendsScreen(
    onBackClick: () -> Unit
) {
    BackHandler {
        onBackClick()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("friends_screen"),
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
                    text = "Friends",
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
                                                NexoraCyan.copy(alpha = 0.28f),
                                                NexoraPurple.copy(alpha = 0.12f),
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
                                            NexoraCyan,
                                            NexoraPurple
                                        )
                                    )
                                )
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.People,
                                        contentDescription = "Friends",
                                        tint = NexoraCyan,
                                        modifier = Modifier.size(46.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // "NEXT UPDATE" Badge
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF0F1B35),
                            border = BorderStroke(
                                1.dp,
                                Brush.horizontalGradient(
                                    listOf(
                                        NexoraCyan.copy(alpha = 0.6f),
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
                                    text = "✨",
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "NEXT UPDATE",
                                    color = NexoraCyan,
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
                            text = "Connect with classmates and study together.",
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
                        icon = "👥",
                        title = "Study Groups & Circles",
                        description = "Form private study circles with your Gujarat Board classmates to share notes and revision schedules.",
                        accentColor = NexoraCyan
                    )
                }

                item {
                    ComingSoonFeatureCard(
                        icon = "🎯",
                        title = "Shared Goals & Focus Streaks",
                        description = "Track group daily study targets, keep each other accountable, and build unstoppable revision momentum.",
                        accentColor = NexoraPurple
                    )
                }

                item {
                    ComingSoonFeatureCard(
                        icon = "💬",
                        title = "Peer Doubts & Discussions",
                        description = "Ask questions on tricky GSEB Commerce topics and solve textbook problems collaboratively.",
                        accentColor = NexoraRoyalBlue
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
 * Preserved existing full Friends implementation so all Supabase friend requests,
 * queries, and models remain completely intact for future release.
 */
@Composable
fun FriendsScreenLegacy(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val studentProfile = remember { StudentPreferences.getStudentProfile(context) }
    val authRepo = remember { SupabaseAuthRepository(context) }
    val isSupabaseConfigured = remember { SupabaseClientManager.isConfigured(context) }

    var searchQuery by remember { mutableStateOf("") }
    var searchResultProfile by remember { mutableStateOf<StudentProfile?>(null) }
    var searchErrorMessage by remember { mutableStateOf<String?>(null) }
    var isSearching by remember { mutableStateOf(false) }

    var pendingRequests by remember { mutableStateOf<List<SupabaseAuthRepository.FriendRequestWithSender>>(emptyList()) }
    var acceptedFriends by remember { mutableStateOf<List<StudentProfile>>(emptyList()) }
    var isLoadingRequests by remember { mutableStateOf(false) }
    var isLoadingFriends by remember { mutableStateOf(false) }

    fun refreshData() {
        if (!isSupabaseConfigured) return
        scope.launch {
            isLoadingRequests = true
            val reqRes = authRepo.getPendingFriendRequests()
            if (reqRes.isSuccess) {
                pendingRequests = reqRes.getOrDefault(emptyList())
            }
            isLoadingRequests = false

            isLoadingFriends = true
            val friendsRes = authRepo.getAcceptedFriends()
            if (friendsRes.isSuccess) {
                acceptedFriends = friendsRes.getOrDefault(emptyList())
            }
            isLoadingFriends = false
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    BackHandler {
        onBackClick()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("friends_screen"),
        containerColor = NexoraBackground,
        contentWindowInsets = WindowInsets.systemBars
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            // Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(44.dp)
                        .background(NexoraSurfaceVariant, CircleShape)
                        .bounceClick { onBackClick() }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NexoraTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Friends & Study Buddies",
                        color = NexoraTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Connect with Gujarat Board Class 12 Commerce peers",
                        color = NexoraTextSecondary,
                        fontSize = 12.sp
                    )
                }
                IconButton(
                    onClick = { refreshData() },
                    modifier = Modifier
                        .size(40.dp)
                        .background(NexoraSurfaceVariant, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Refresh",
                        tint = NexoraCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Student ID Card
                item {
                    NexoraCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 20.dp,
                        contentPadding = 18.dp
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = "Your Student ID", fontSize = 12.sp, color = NexoraTextSecondary, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = studentProfile.studentId.ifBlank { "STU-GUEST" },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = NexoraCyan
                            )
                            Text(
                                text = "Share your Student ID or registered email with friends so they can add you.",
                                fontSize = 11.5.sp,
                                color = NexoraTextMuted
                            )
                        }
                    }
                }

                // Friend Search Section
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "Add Study Buddy", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NexoraTextPrimary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { 
                                    searchQuery = it 
                                    searchResultProfile = null
                                    searchErrorMessage = null
                                },
                                placeholder = { Text("NX-XXXXXXXX or email") },
                                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = NexoraCyan) },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NexoraCyan,
                                    unfocusedBorderColor = NexoraBorder
                                )
                            )
                            Button(
                                onClick = {
                                    if (searchQuery.isNotBlank()) {
                                        isSearching = true
                                        searchResultProfile = null
                                        searchErrorMessage = null
                                        scope.launch {
                                            val result = authRepo.searchStudentByStudentIdOrEmail(searchQuery)
                                            if (result.isSuccess) {
                                                val profile = result.getOrNull()
                                                if (profile != null) {
                                                    if (profile.userId == studentProfile.userId) {
                                                        searchErrorMessage = "You cannot add yourself as a friend."
                                                    } else {
                                                        searchResultProfile = profile
                                                    }
                                                } else {
                                                    searchErrorMessage = "Student ID or Email '$searchQuery' not found."
                                                }
                                            } else {
                                                searchErrorMessage = "Error searching for student. Please check your connection."
                                            }
                                            isSearching = false
                                        }
                                    }
                                },
                                enabled = !isSearching && searchQuery.isNotBlank(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NexoraCyan),
                                modifier = Modifier.height(56.dp)
                            ) {
                                if (isSearching) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        if (searchResultProfile != null) {
                            NexoraCard(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                containerColor = NexoraSurfaceVariant.copy(alpha = 0.5f),
                                cornerRadius = 16.dp,
                                contentPadding = 16.dp
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    NexoraAvatar(
                                        name = searchResultProfile?.studentName ?: "S",
                                        avatarColorHex = searchResultProfile?.avatarColorHex ?: "#00BCD4",
                                        size = 48.dp
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = searchResultProfile?.studentName ?: "Unknown Student",
                                            color = NexoraTextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = searchResultProfile?.studentId ?: "",
                                            color = NexoraTextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Button(
                                        onClick = { 
                                            searchResultProfile?.userId?.let { receiverId ->
                                                isSearching = true
                                                scope.launch {
                                                    val res = authRepo.sendFriendRequest(receiverId)
                                                    if (res.isSuccess) {
                                                        searchResultProfile = null
                                                        searchErrorMessage = "Friend request sent successfully!"
                                                        refreshData()
                                                    } else {
                                                        searchErrorMessage = "Failed to send request: ${res.exceptionOrNull()?.message}"
                                                    }
                                                    isSearching = false
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NexoraCyan),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text("Add Friend", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (searchErrorMessage != null) {
                            Surface(
                                color = if (searchErrorMessage!!.contains("sent")) NexoraCyan.copy(alpha = 0.1f) else NexoraError.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) {
                                Text(
                                    text = searchErrorMessage!!,
                                    color = if (searchErrorMessage!!.contains("sent")) NexoraCyan else NexoraError,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }

                // Friend Requests Section
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Friend Requests", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NexoraTextPrimary)
                            if (pendingRequests.isNotEmpty()) {
                                Surface(
                                    color = NexoraCyan.copy(alpha = 0.2f),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "${pendingRequests.size}",
                                        color = NexoraCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        if (isLoadingRequests) {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = NexoraCyan)
                            }
                        } else if (pendingRequests.isEmpty()) {
                            NexoraCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 20.dp,
                                contentPadding = 20.dp
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "No pending friend requests",
                                        color = NexoraTextSecondary,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Requests from fellow students will appear here when received.",
                                        color = NexoraTextMuted,
                                        fontSize = 11.5.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                pendingRequests.forEach { req ->
                                    NexoraCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        cornerRadius = 16.dp,
                                        contentPadding = 12.dp
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            NexoraAvatar(
                                                name = req.senderProfile.studentName,
                                                avatarColorHex = req.senderProfile.avatarColorHex,
                                                size = 44.dp
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = req.senderProfile.studentName,
                                                    color = NexoraTextPrimary,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = req.senderProfile.studentId,
                                                    color = NexoraTextSecondary,
                                                    fontSize = 11.5.sp
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                IconButton(
                                                    onClick = {
                                                        scope.launch {
                                                            authRepo.acceptFriendRequest(req.senderProfile.userId)
                                                            refreshData()
                                                        }
                                                    },
                                                    modifier = Modifier.size(36.dp).background(NexoraCyan, CircleShape)
                                                ) {
                                                    Icon(Icons.Outlined.Check, contentDescription = "Accept", tint = Color.White, modifier = Modifier.size(18.dp))
                                                }
                                                IconButton(
                                                    onClick = {
                                                        scope.launch {
                                                            authRepo.declineFriendRequest(req.senderProfile.userId)
                                                            refreshData()
                                                        }
                                                    },
                                                    modifier = Modifier.size(36.dp).background(NexoraError.copy(alpha = 0.2f), CircleShape)
                                                ) {
                                                    Icon(Icons.Outlined.Close, contentDescription = "Decline", tint = NexoraError, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Friends List Section
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "Your Friends List", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NexoraTextPrimary)
                        
                        if (isLoadingFriends) {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = NexoraCyan)
                            }
                        } else if (acceptedFriends.isEmpty()) {
                            NexoraCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 20.dp,
                                contentPadding = 28.dp
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "No friends added yet",
                                        color = NexoraTextSecondary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Search for fellow Gujarat Board Class 12 Commerce students above using their Student ID or email to collaborate and track progress together.",
                                        color = NexoraTextMuted,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                acceptedFriends.forEach { friend ->
                                    NexoraCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        cornerRadius = 16.dp,
                                        contentPadding = 14.dp
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            NexoraAvatar(
                                                name = friend.studentName,
                                                avatarColorHex = friend.avatarColorHex,
                                                size = 46.dp
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = friend.studentName,
                                                    color = NexoraTextPrimary,
                                                    fontSize = 14.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "ID: ${friend.studentId} • ${friend.targetMarks}",
                                                    color = NexoraTextSecondary,
                                                    fontSize = 12.sp
                                                )
                                            }
                                            Surface(
                                                color = NexoraCyan.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text(
                                                    text = "🔥 ${friend.studyStreakDays}d streak",
                                                    color = NexoraCyan,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
