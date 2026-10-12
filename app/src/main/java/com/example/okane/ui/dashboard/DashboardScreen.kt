package com.example.okane.ui.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.okane.ui.components.AnimatedAmountText
import com.example.okane.ui.components.calmEntrance
import com.example.okane.ui.format.Formatters
import com.example.okane.ui.illustration.CalmLandscape
import com.example.okane.ui.illustration.SkyTime

/**
 * Home: a greeting and the total of every wallet, over a calm landscape.
 * Nothing else lives here on purpose — recording, history and wallets are one
 * tap away in the navigation bar, and "Perlu konfirmasi" sits in Riwayat.
 *
 * The total is the same all-time sum Dompetku shows: every wallet's initial
 * balance plus its transactions, archived wallets included, transfers neutral.
 */
@Composable
fun DashboardScreen(
    greeting: String,
    totalMoney: Long,
    modifier: Modifier = Modifier,
    skyTime: SkyTime = remember { SkyTime.now() },
) {
    Box(modifier = modifier.fillMaxSize()) {
        CalmLandscape(time = skyTime, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 64.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = greeting,
                style = onSky(30.sp, FontWeight.Medium),
                color = Color.White.copy(alpha = 0.94f),
                modifier = Modifier
                    .calmEntrance(0, "home-greeting")
                    .testTag("homeGreeting"),
            )
            AnimatedAmountText(
                value = totalMoney,
                format = Formatters::balanceRupiah,
                style = onSky(amountSize(totalMoney), FontWeight.SemiBold).copy(fontFeatureSettings = "tnum"),
                color = Color.White,
                modifier = Modifier
                    .padding(top = 18.dp)
                    .calmEntrance(1, "home-total")
                    .testTag("homeTotal"),
            )
            Text(
                text = "Total uangmu",
                style = onSky(15.sp, FontWeight.Medium),
                color = Color.White.copy(alpha = 0.86f),
                modifier = Modifier
                    .padding(top = 2.dp)
                    .calmEntrance(1, "home-total-label"),
            )
        }
    }
}

/** White text on sky, with a soft shadow so it holds on the pale parts too. */
@Composable
private fun onSky(size: TextUnit, weight: FontWeight): TextStyle =
    MaterialTheme.typography.headlineLarge.copy(
        fontSize = size,
        lineHeight = size * 1.15f,
        fontWeight = weight,
        textAlign = TextAlign.Center,
        shadow = Shadow(Color(0x40102030), offset = Offset(0f, 2f), blurRadius = 14f),
    )

/** Shrinks only for totals in the billions, so the amount stays on one line. */
internal fun amountSize(total: Long): TextUnit {
    val digits = Formatters.balanceRupiah(total).length
    return when {
        digits <= 13 -> 44.sp   // up to Rp999.999.999
        digits <= 15 -> 38.sp   // up to Rp99.999.999.999
        else -> 32.sp
    }
}
