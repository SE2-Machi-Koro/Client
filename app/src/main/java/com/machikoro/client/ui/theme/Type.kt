package com.machikoro.client.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.machikoro.client.R

// Set of Material typography styles to start with

// Title Font (z.B. "MACHI KORO" - simple font)
val TitleFontClean = FontFamily(
    Font(R.font.bebas_neue)
)

// Title Font (original)
val TitleFont = FontFamily(
    Font(R.font.plaster)
)

// Body Font (Cabin)
val BodyFont = FontFamily(
    Font(R.font.cabin)
)


val Typography = Typography(

    // GROßER LOGIN TITEL (64sp)
    headlineLarge = TextStyle(
        fontFamily = TitleFont,
        fontWeight = FontWeight.Normal,
        fontSize = 64.sp,
        letterSpacing = 2.sp
    ),

    titleLarge = TextStyle(
        fontFamily = TitleFontClean,
        fontWeight = FontWeight.Normal,
        fontSize = 64.sp,
        letterSpacing = 2.sp
    ),


    // Überschrift (z.B. "WILLKOMMEN")
    headlineMedium = TextStyle(
        fontFamily = TitleFont,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp
    ),

    // normaler Text (16sp)
    bodyLarge = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),

    // kleiner Text (14sp)
    bodyMedium = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),

    // Button Text (20sp)
    labelLarge = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp
    )
)