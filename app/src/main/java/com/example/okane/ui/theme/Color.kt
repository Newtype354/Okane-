package com.example.okane.ui.theme

import androidx.compose.ui.graphics.Color

// Calm palette: warm paper neutrals, a sage brand colour and four soft pastel
// families borrowed from stones, sky, lavender and morning light. Hex values
// live here; screens consume Material roles or the named accents below.
//
// Every text pairing was checked against WCAG AA (4.5:1): ink on paper 15:1,
// muted text on paper 5.1:1, sage on white 4.9:1, clay on white 4.7:1.

val White = Color(0xFFFFFFFF)

/** Page background: warm off-white, never pure white, so white cards lift off it. */
val Paper = Color(0xFFFAFAF7)

/** Soft stone grey used for hero cards and illustration backdrops. */
val Stone = Color(0xFFF3F2EE)
val StoneDeep = Color(0xFFE9E7E1)

val Ink = Color(0xFF1E2420)
val InkMuted = Color(0xFF676D68)
val Hairline = Color(0xFFEBEAE5)

// Brand: sage green.
val Sage = Color(0xFF4E7A5D)
val SageSoft = Color(0xFF8DB59A)
val SageMist = Color(0xFFE3EEE5)
val SageInk = Color(0xFF1F3B2A)

// Pastel families: container + accent + ink. Used by tiles and chips.
val Mist = Color(0xFFEAF1FA)
val MistAccent = Color(0xFF5B8BCB)
val MistInk = Color(0xFF2F5F9A)

val Lavender = Color(0xFFF1EEFA)
val LavenderAccent = Color(0xFF8C7BC9)
val LavenderInk = Color(0xFF5E4FA0)

val Peach = Color(0xFFFCF1E6)
val PeachAccent = Color(0xFFE39A55)
val PeachInk = Color(0xFF8A4F1C)

val Mint = Color(0xFFEAF3EC)
val MintAccent = Color(0xFF6DA37E)

// Illustration tones for the stacked stones.
val PebbleLight = Color(0xFFF1F0EC)
val PebbleMid = Color(0xFFDAD8D2)
val PebbleShadow = Color(0xFFC4C2BB)
val Leaf = Color(0xFF7FAF86)
val LeafDeep = Color(0xFF5E9468)

internal val LightPrimary = Sage
internal val LightOnPrimary = White
internal val LightPrimaryContainer = SageMist
internal val LightOnPrimaryContainer = SageInk
internal val LightSecondary = MistInk
internal val LightOnSecondary = White
internal val LightSecondaryContainer = Mist
internal val LightOnSecondaryContainer = Color(0xFF1B3657)
internal val LightTertiary = LavenderInk
internal val LightOnTertiary = White
internal val LightTertiaryContainer = Lavender
internal val LightOnTertiaryContainer = Color(0xFF2E2558)
internal val LightBackground = Paper
internal val LightOnBackground = Ink
internal val LightSurface = White
internal val LightOnSurface = Ink
internal val LightSurfaceVariant = Stone
internal val LightOnSurfaceVariant = InkMuted
internal val LightOutline = Color(0xFF8C928C)
internal val LightOutlineVariant = Hairline
internal val LightSurfaceContainerLowest = White
internal val LightSurfaceContainerLow = Color(0xFFF7F6F2)
internal val LightSurfaceContainer = Stone
internal val LightSurfaceContainerHigh = Color(0xFFEDECE7)
internal val LightSurfaceContainerHighest = StoneDeep
internal val LightError = Color(0xFFB3443C)
internal val LightOnError = White
internal val LightErrorContainer = Color(0xFFF8E3E0)
internal val LightOnErrorContainer = Color(0xFF5C1A14)

// Transaction colours stay semantic, and text labels still carry the meaning.
// All three clear 4.5:1 on white for normal-size amount text.
val IncomeGreen = Color(0xFF3D7653)
val ExpenseRed = Color(0xFFB5574B)
val TransferBlue = Color(0xFF3B6CAB)

/** Soft backgrounds behind the semantic colours (icons, chips). */
val IncomeSoft = Mint
val ExpenseSoft = Color(0xFFFBEDE9)
val TransferSoft = Mist
