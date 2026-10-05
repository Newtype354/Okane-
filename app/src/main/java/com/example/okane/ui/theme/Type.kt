package com.example.okane.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.okane.R

val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
)

private val MaterialTypeScale = Typography()

private fun TextStyle.jakarta(weight: FontWeight, tracking: Float = 0f) =
    copy(fontFamily = PlusJakartaSans, fontWeight = weight, letterSpacing = tracking.em)

/**
 * One family, a quiet weight policy: headings are semibold with slightly tight
 * tracking (the clean, editorial look of the reference), body copy stays
 * regular with a little extra line height so long Indonesian sentences breathe.
 */
val Typography = Typography(
    displayLarge = MaterialTypeScale.displayLarge.jakarta(FontWeight.SemiBold, -0.02f),
    displayMedium = MaterialTypeScale.displayMedium.jakarta(FontWeight.SemiBold, -0.02f),
    displaySmall = MaterialTypeScale.displaySmall.jakarta(FontWeight.SemiBold, -0.02f),
    headlineLarge = MaterialTypeScale.headlineLarge.jakarta(FontWeight.Bold, -0.02f),
    headlineMedium = MaterialTypeScale.headlineMedium.jakarta(FontWeight.SemiBold, -0.015f),
    headlineSmall = MaterialTypeScale.headlineSmall.jakarta(FontWeight.SemiBold, -0.01f),
    titleLarge = MaterialTypeScale.titleLarge.jakarta(FontWeight.SemiBold, -0.01f),
    titleMedium = MaterialTypeScale.titleMedium.jakarta(FontWeight.SemiBold),
    titleSmall = MaterialTypeScale.titleSmall.jakarta(FontWeight.Medium),
    bodyLarge = MaterialTypeScale.bodyLarge.jakarta(FontWeight.Normal).copy(lineHeight = 24.sp),
    bodyMedium = MaterialTypeScale.bodyMedium.jakarta(FontWeight.Normal).copy(lineHeight = 21.sp),
    bodySmall = MaterialTypeScale.bodySmall.jakarta(FontWeight.Normal).copy(lineHeight = 18.sp),
    labelLarge = MaterialTypeScale.labelLarge.jakarta(FontWeight.SemiBold),
    labelMedium = MaterialTypeScale.labelMedium.jakarta(FontWeight.Medium),
    labelSmall = MaterialTypeScale.labelSmall.jakarta(FontWeight.Medium),
)
