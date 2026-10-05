package com.example.okane.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.okane.ui.components.AnimatedAmountText
import com.example.okane.ui.components.CalmTokens
import com.example.okane.ui.format.Formatters
import com.example.okane.ui.illustration.StackedStones
import com.example.okane.ui.theme.OkaneMotion
import com.example.okane.ui.theme.Stone
import com.example.okane.ui.theme.SageMist

/**
 * Home's hero: the all-wallet total on a soft stone card, with the cairn
 * illustration settling into place the first time it appears.
 *
 * The total is the same computed-balance sum Dompetku shows (initial balances
 * plus every transaction, archived wallets included, transfers neutral).
 */
@Composable
fun TotalMoneyCard(
    totalMoney: Long,
    caption: String,
    modifier: Modifier = Modifier,
) {
    var settled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { settled = true }
    val stones by animateFloatAsState(
        targetValue = if (settled) 1f else 0f,
        animationSpec = OkaneMotion.enter(duration = 1400, delay = 150),
        label = "heroStones",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CalmTokens.cardRadius))
            .background(Brush.linearGradient(listOf(Stone, Stone, SageMist.copy(alpha = 0.7f))))
            .testTag("homeTotal"),
    ) {
        Row(
            modifier = Modifier.padding(start = 22.dp, top = 22.dp, bottom = 22.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Total uangmu",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val formatted = Formatters.balanceRupiah(totalMoney)
                AnimatedAmountText(
                    value = totalMoney,
                    format = Formatters::balanceRupiah,
                    startFromZero = true,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = when {
                            formatted.length > 16 -> 22.sp
                            formatted.length > 13 -> 25.sp
                            else -> 28.sp
                        },
                        fontFeatureSettings = "tnum",
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            StackedStones(
                progress = stones,
                modifier = Modifier.size(width = 112.dp, height = 104.dp),
            )
        }
    }
}
