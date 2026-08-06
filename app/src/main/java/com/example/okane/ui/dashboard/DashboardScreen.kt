package com.example.okane.ui.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.okane.ui.components.GlassFloatingActionButton

/** Minimal Home landing page: greeting, all-wallet total, art, and keypad entry. */
@Composable
fun DashboardScreen(
    greeting: String,
    totalMoney: Long,
    onAddTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        HomeHero(
            greeting = greeting,
            totalRecorded = totalMoney,
            modifier = Modifier.fillMaxSize()
        )
        GlassFloatingActionButton(
            onClick = onAddTransaction,
            icon = Icons.Filled.Add,
            contentDescription = "Catat transaksi",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 104.dp)
                .testTag("homeAddTransaction"),
        )
    }
}
