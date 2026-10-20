package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.animation.bounceClick
import com.example.ui.theme.*

enum class NexoraTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val color: Color,
    val testTag: String
) {
    HOME(
        route = "tab_home",
        label = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        color = CategoryHome,
        testTag = "nav_tab_home"
    ),
    PLANNER(
        route = "tab_planner",
        label = "Planner",
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth,
        color = CategoryPlanner,
        testTag = "nav_tab_planner"
    ),
    SUBJECTS(
        route = "tab_subjects",
        label = "Subjects",
        selectedIcon = Icons.Filled.MenuBook,
        unselectedIcon = Icons.Outlined.MenuBook,
        color = CategorySubjects,
        testTag = "nav_tab_subjects"
    ),
    PROFILE(
        route = "tab_profile",
        label = "Profile",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        color = CategoryProfile,
        testTag = "nav_tab_profile"
    )
}

@Composable
fun NexoraBottomNavigationBar(
    currentTab: NexoraTab,
    onTabSelected: (NexoraTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(NexoraBackground)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("nexora_bottom_navigation")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .background(NexoraSurface, shape = RoundedCornerShape(22.dp))
                .border(1.5.dp, NexoraBorder, shape = RoundedCornerShape(22.dp))
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NexoraTab.entries.forEach { tab ->
                val isSelected = tab == currentTab

                val iconTint by animateColorAsState(
                    targetValue = if (isSelected) tab.color else NexoraTextMuted.copy(alpha = 0.8f),
                    animationSpec = tween(durationMillis = 300),
                    label = "tabIconTint"
                )

                val labelColor by animateColorAsState(
                    targetValue = if (isSelected) NexoraTextPrimary else NexoraTextMuted.copy(alpha = 0.8f),
                    animationSpec = tween(durationMillis = 300),
                    label = "tabLabelColor"
                )

                val indicatorAlpha by animateColorAsState(
                    targetValue = if (isSelected) tab.color.copy(alpha = 0.12f) else Color.Transparent,
                    animationSpec = tween(durationMillis = 300),
                    label = "tabBgColor"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .bounceClick { onTabSelected(tab) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(indicatorAlpha, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.label,
                            tint = iconTint,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tab.label,
                        color = labelColor,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        letterSpacing = 0.2.sp
                    )
                }
            }
        }
    }
}
