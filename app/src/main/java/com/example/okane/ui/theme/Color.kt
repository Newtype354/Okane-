package com.example.okane.ui.theme

import androidx.compose.ui.graphics.Color

// Illustration-led brand palette. Hex values live here; screens consume
// Material color roles except for asset-specific SkyBlue and finance semantics.
val SkyBlue = Color(0xFF0365AD)
val White = Color(0xFFFFFFFF)
val DeepNavy = Color(0xFF0D171E)

internal val LightOnPrimary = White
internal val LightPrimaryContainer = Color(0xFFD6ECFA)
internal val LightOnPrimaryContainer = Color(0xFF003A63)
internal val LightSecondary = Color(0xFF8FAA5D)
internal val LightOnSecondary = Color(0xFF1F3100)
internal val LightSecondaryContainer = Color(0xFFE5F0CF)
internal val LightOnSecondaryContainer = Color(0xFF263600)
internal val LightTertiary = Color(0xFFD98FA8)
internal val LightOnTertiary = White
internal val LightTertiaryContainer = Color(0xFFF8DCE5)
internal val LightOnTertiaryContainer = Color(0xFF4A1F30)
internal val LightBackground = Color(0xFFF8F6F0)
internal val LightOnBackground = Color(0xFF17212A)
internal val LightSurface = White
internal val LightOnSurface = LightOnBackground
internal val LightSurfaceVariant = Color(0xFFE9EDE8)
internal val LightOnSurfaceVariant = Color(0xFF59636A)
internal val LightOutline = Color(0xFF788187)
internal val LightSurfaceContainerLow = Color(0xFFFDFBF6)
internal val LightSurfaceContainer = Color(0xFFF3F1EB)
internal val LightSurfaceContainerHigh = Color(0xFFEDEBE5)
internal val LightError = Color(0xFFBA1A1A)
internal val LightOnError = White
internal val LightErrorContainer = Color(0xFFFFDAD6)
internal val LightOnErrorContainer = Color(0xFF410002)

internal val DarkPrimary = Color(0xFF83C7F5)
internal val DarkOnPrimary = Color(0xFF003553)
internal val DarkPrimaryContainer = Color(0xFF004C78)
internal val DarkOnPrimaryContainer = Color(0xFFCDEBFF)
internal val DarkSecondary = Color(0xFFB8CF8A)
internal val DarkOnSecondary = Color(0xFF243600)
internal val DarkSecondaryContainer = Color(0xFF384D12)
internal val DarkOnSecondaryContainer = Color(0xFFD6EFA3)
internal val DarkTertiary = Color(0xFFF0B4C7)
internal val DarkOnTertiary = Color(0xFF4A1F30)
internal val DarkTertiaryContainer = Color(0xFF633247)
internal val DarkOnTertiaryContainer = Color(0xFFFFD8E4)
internal val DarkBackground = DeepNavy
internal val DarkOnBackground = Color(0xFFE7EDF1)
internal val DarkSurface = Color(0xFF111C24)
internal val DarkOnSurface = DarkOnBackground
internal val DarkSurfaceVariant = Color(0xFF24313A)
internal val DarkOnSurfaceVariant = Color(0xFFB8C2C9)
internal val DarkOutline = Color(0xFF89949B)
internal val DarkSurfaceContainerLow = Color(0xFF0F1A22)
internal val DarkSurfaceContainer = Color(0xFF17232C)
internal val DarkSurfaceContainerHigh = Color(0xFF202D36)
internal val DarkError = Color(0xFFFFB4AB)
internal val DarkOnError = Color(0xFF690005)
internal val DarkErrorContainer = Color(0xFF93000A)
internal val DarkOnErrorContainer = Color(0xFFFFDAD6)

// Transaction colors remain semantic and text/icon labels still carry meaning.
// Slightly deepened from the illustration samples so normal-size amount text
// clears WCAG 4.5:1 on the light surface (4.55:1 and 4.65:1 respectively).
val IncomeGreen = Color(0xFF627F3C)
val ExpenseRed = Color(0xFFBA526C)
val TransferBlue = SkyBlue
val IncomeGreenDark = Color(0xFFB8D68A)
val ExpenseRedDark = Color(0xFFF3AEC0)
val TransferBlueDark = DarkPrimary
