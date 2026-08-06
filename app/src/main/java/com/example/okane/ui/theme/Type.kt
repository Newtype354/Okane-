package com.example.okane.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.okane.R

val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
)

private val MaterialTypeScale = Typography()

/** One centralized family and weight policy for every Material semantic style. */
val Typography = Typography(
    displayLarge = MaterialTypeScale.displayLarge.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold),
    displayMedium = MaterialTypeScale.displayMedium.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold),
    displaySmall = MaterialTypeScale.displaySmall.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold),
    headlineLarge = MaterialTypeScale.headlineLarge.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold),
    headlineMedium = MaterialTypeScale.headlineMedium.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold),
    headlineSmall = MaterialTypeScale.headlineSmall.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold),
    titleLarge = MaterialTypeScale.titleLarge.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold),
    titleMedium = MaterialTypeScale.titleMedium.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold),
    titleSmall = MaterialTypeScale.titleSmall.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Medium),
    bodyLarge = MaterialTypeScale.bodyLarge.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Normal),
    bodyMedium = MaterialTypeScale.bodyMedium.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Normal),
    bodySmall = MaterialTypeScale.bodySmall.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Normal),
    labelLarge = MaterialTypeScale.labelLarge.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Medium),
    labelMedium = MaterialTypeScale.labelMedium.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Medium),
    labelSmall = MaterialTypeScale.labelSmall.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Normal),
)
