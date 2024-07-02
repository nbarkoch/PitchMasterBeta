package com.example.pitchmasterbeta.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink70 = Color(0xFFEEABBE)
val Pink80 = Color(0xFFF5CAD7)

val Blue10 = Color(0xFF3834C0)
val Pink10 = Color(0xFFDD34A4)
val PinkDark10 = Color(0xFF69357C)
val DarkGrey10 = Color(0xFF333333)
val PinkLight10 = Color(0xFFE44EA0)
val GreenGrey10 = Color(0xff7dab52)
val Green10 = Color(0xFF34DD3C)
val Red10 = Color(0xffd52737)
val PurpleDarkWeak10 = Color(0xFF5C519C)
val PurpleLight10 = Color(0xFFD59EFD)
val PurpleDark10 = Color(0xFF2E265E)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
val Pink50 = Color(0xFF9E6C7C)


val MainGradientBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xFF403C63),
        PurpleDark10,
        Color(0xFF121314),
    ),
)

val HeaderGradientBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF132152),
        Color(0x750E0D3A),
        Color.Transparent
    )
)

val FooterGradientBrush = Brush.verticalGradient(
    colors = listOf(
        Color.Transparent,
        Color(0xFF1E0D3A),
        Color(0xFF0E0D3A)
    ),
)


val DynamicGradientBrush: (Color) -> Brush = {
    Brush.linearGradient(
        colors = listOf(
            Color(0xFF403C63),
            it.copy(alpha = 0.05f),
            Color(0xFF121314),
        ),
    )
}