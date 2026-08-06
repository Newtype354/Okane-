package com.example.okane.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.okane.R
import com.example.okane.ui.format.Formatters
import com.example.okane.ui.theme.SkyBlue
import com.example.okane.ui.theme.White

/** Static, full-bleed landing hero. The illustration is fit to width, never cropped. */
@Composable
fun HomeHero(
    greeting: String,
    totalRecorded: Long,
    modifier: Modifier = Modifier
) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val fraction by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(900),
        label = "recordedTotalCount"
    )
    val shown = (totalRecorded.toDouble() * fraction).toLong()
    val formattedTotal = Formatters.balanceRupiah(shown)
    val amountSize = when {
        formattedTotal.length > 20 -> 26.sp
        formattedTotal.length > 16 -> 30.sp
        else -> 38.sp
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SkyBlue)
    ) {
        Image(
            painter = painterResource(R.drawable.hero_cat),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.BottomCenter,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        )

        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 28.dp)
        ) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
                fontWeight = FontWeight.Bold,
                color = White
            )
            Text(
                text = "Total uangmu:",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = White.copy(alpha = 0.88f),
                modifier = Modifier.padding(top = 24.dp)
            )
            Text(
                text = formattedTotal,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = amountSize,
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum",
                ),
                color = White,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Preview(name = "Home 20:9", widthDp = 360, heightDp = 720, showBackground = true)
@Preview(name = "Home 18:9", widthDp = 432, heightDp = 768, showBackground = true)
@Preview(name = "Home tablet", widthDp = 800, heightDp = 1100, showBackground = true)
@Composable
private fun HomeHeroPreview() {
    HomeHero(greeting = "Konnichiwa", totalRecorded = 12_500_000)
}
