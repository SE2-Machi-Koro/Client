
package com.machikoro.client.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.machikoro.client.R

// -----------------------------
// Font definitions
// -----------------------------


// Title Font
val TitleFont = FontFamily(
    Font(R.font.lithosbold)
)
// Title Font
val OldTitleFont = FontFamily(
    Font(R.font.jaro)
)
// Default body font
val BodyFont = FontFamily(
    Font(R.font.cabin)
)

// -----------------------------
// Typography styles
// -----------------------------

val Typography = Typography(

    // Large main title (e.g. Login title - 64sp)
    headlineLarge = TextStyle(
        fontFamily = TitleFont,
        fontWeight = FontWeight.Normal,
        fontSize = 64.sp,
        letterSpacing = 1.sp
    ),

    // Alternative large title (clean version)
    titleLarge = TextStyle(
        fontFamily = TitleFont,
        fontWeight = FontWeight.Normal,
        fontSize = 64.sp,
        letterSpacing = 1.sp
    ),


    // Section title (e.g. "WELCOME")
    headlineMedium = TextStyle(
        fontFamily = TitleFont,
        fontWeight = FontWeight.Normal,
        fontSize = 64.sp,
    ),

    // Section title small(e.g. "LOGIN; REGISTER; PLAYERS; LEAVE LOBBY")
    headlineSmall = TextStyle(
        fontFamily = TitleFont,
        fontWeight = FontWeight.Normal,
        color = TextBlueDark,
        fontSize = 24.sp,
        letterSpacing = 1.sp
    ),

    // Default text (16sp)
    bodyLarge = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp
    ),

    // Smaller text (14sp)
    bodyMedium = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 0.5.sp
    ),

    // Button text (20sp)
    labelLarge = TextStyle(
        fontFamily = TitleFont,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp
    )
)