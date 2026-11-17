package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val PixelColorScheme = darkColorScheme(
    primary = ChampionshipGold,
    onPrimary = StadiumMidnight,
    primaryContainer = ChampionshipGoldDark,
    onPrimaryContainer = TextPrimary,
    secondary = ArcadeEmerald,
    onSecondary = StadiumMidnight,
    secondaryContainer = ArcadeEmeraldDark,
    onSecondaryContainer = TextPrimary,
    tertiary = PlayerCyan,
    onTertiary = StadiumMidnight,
    background = StadiumMidnight,
    onBackground = TextPrimary,
    surface = TacticalSurface,
    onSurface = TextPrimary,
    surfaceVariant = TacticalSurfaceVariant,
    onSurfaceVariant = TextMuted,
    outline = PanelBorder,
    error = FoulCrimson,
    onError = TextPrimary
)

private val PixelShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(3.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(6.dp),
    extraLarge = RoundedCornerShape(8.dp)
)

@Composable
fun PixelKickTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PixelColorScheme,
        typography = PixelTypography,
        shapes = PixelShapes,
        content = content
    )
}
