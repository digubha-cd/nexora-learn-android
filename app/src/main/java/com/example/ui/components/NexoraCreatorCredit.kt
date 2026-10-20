package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NexoraCyan
import com.example.ui.theme.NexoraTextMuted

/**
 * Subtle, professional, and elegant creator credit footer.
 */
@Composable
fun NexoraCreatorCredit(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 24.dp)
            .testTag("creator_credit_footer"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "This App is Made by",
            color = NexoraTextMuted.copy(alpha = 0.75f),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "DIGVIJAYSINH CHAUHAN",
            color = NexoraCyan.copy(alpha = 0.9f),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
    }
}
