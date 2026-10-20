package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.database.entity.FavoriteTipEntity
import com.example.core.repository.FavoriteTipsRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dialog displaying saved favorite study tips stored in the local Room database.
 * Allows students to review, copy, share, and manage their saved tips.
 */
@Composable
fun SavedTipsDialog(
    isGujarati: Boolean,
    onDismissRequest: () -> Unit
) {
    val favorites by FavoriteTipsRepository.favoritesList.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("saved_tips_dialog"),
            color = Color.White,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEE2E2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isGujarati) "સાચવેલી અભ્યાસ ટિપ્સ" else "Saved Study Tips",
                                    color = NexoraTextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(NexoraCyan.copy(alpha = 0.12f))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${favorites.size}",
                                        color = NexoraCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            Text(
                                text = if (isGujarati) "રૂમ ડેટાબેઝમાં સાચવેલી બોર્ડ ટિપ્સ" else "Saved offline in local Room database",
                                color = NexoraTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("close_saved_tips_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = if (isGujarati) "બંધ કરો" else "Close",
                            tint = NexoraTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = NexoraBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // Content
                if (favorites.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(NexoraSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.FavoriteBorder,
                                    contentDescription = null,
                                    tint = NexoraTextMuted,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (isGujarati) "હજી સુધી કોઈ ટિપ સાચવેલી નથી" else "No saved study tips yet",
                                color = NexoraTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isGujarati)
                                    "દૈનિક ટિપ્સ કાર્ડ પર ❤️ (ફેવરિટ) બટન દબાવો જેથી તમે ગમે ત્યારે રિવ્યૂ કરી શકો."
                                else
                                    "Tap the ❤️ favorite button on any daily tip to save it here for later review.",
                                color = NexoraTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(
                            items = favorites,
                            key = { it.id }
                        ) { item ->
                            SavedTipCardItem(
                                item = item,
                                isGujarati = isGujarati,
                                onShare = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(
                                            Intent.EXTRA_SUBJECT,
                                            if (isGujarati) "ધોરણ ૧૨ કોમર્સ અભ્યાસ ટિપ - ${item.subjectName}" else "Class 12 Commerce Study Tip - ${item.subjectName}"
                                        )
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            """
                                            📖 *NEXORA - GSEB Class 12 Commerce Study Tip*
                                            📌 *${item.subjectName}*: ${item.headline}

                                            💡 ${item.tipContent}

                                            ⚡ *${if (isGujarati) "૧૦-મિનિટ એક્શન પ્લાન" else "10-Min Action Plan"}*:
                                            ${item.actionStep}

                                            ✨ Shared via NEXORA Learn
                                            """.trimIndent()
                                        )
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(
                                        sendIntent,
                                        if (isGujarati) "ટિપ મિત્રો સાથે શેર કરો" else "Share study tip"
                                    )
                                    context.startActivity(shareIntent)
                                },
                                onRemove = {
                                    coroutineScope.launch {
                                        FavoriteTipsRepository.removeFavorite(item.id, item.headline)
                                        val toastMsg = if (isGujarati) "ટિપ ફેવરિટમાંથી હટાવી દીધી" else "Tip removed from favorites"
                                        Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedTipCardItem(
    item: FavoriteTipEntity,
    isGujarati: Boolean,
    onShare: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val formattedDate = remember(item.savedAtEpochMillis) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        sdf.format(Date(item.savedAtEpochMillis))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NexoraSurfaceVariant)
            .border(1.dp, NexoraBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("saved_tip_item_${item.id}")
    ) {
        // Top row: Subject badge & Save Date
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.subjectName.uppercase(),
                color = NexoraCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )

            Text(
                text = formattedDate,
                color = NexoraTextMuted,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Headline
        Text(
            text = item.headline,
            color = NexoraTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Tip Content
        Text(
            text = item.tipContent,
            color = NexoraTextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Action Step Mini Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(NexoraCyan.copy(alpha = 0.08f))
                .border(1.dp, NexoraCyan.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(text = "⚡", fontSize = 12.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.actionStep,
                    color = NexoraTextPrimary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom Actions: Copy, Share, Remove
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Copy
            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clip = ClipData.newPlainText(
                        "NEXORA Study Tip",
                        "${item.headline}\n\n${item.tipContent}\n\n${if (isGujarati) "એક્શન પ્લાન" else "Action"}: ${item.actionStep}"
                    )
                    clipboard?.setPrimaryClip(clip)
                    val toastMsg = if (isGujarati) "કોપી થઈ ગયું!" else "Copied!"
                    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = if (isGujarati) "કોપી કરો" else "Copy",
                    tint = NexoraTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Share Button
            IconButton(
                onClick = onShare,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("saved_tip_share_button_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = if (isGujarati) "મિત્રો સાથે શેર કરો" else "Share with classmates",
                    tint = NexoraCyan,
                    modifier = Modifier.size(17.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Remove Button
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("saved_tip_remove_button_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = if (isGujarati) "હટાવો" else "Delete",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
