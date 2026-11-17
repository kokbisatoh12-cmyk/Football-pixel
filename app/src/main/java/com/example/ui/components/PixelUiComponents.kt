package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ArcadeEmerald
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.PanelBorder
import com.example.ui.theme.PanelHighlight
import com.example.ui.theme.StadiumMidnight
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun PixelPanel(
    modifier: Modifier = Modifier,
    borderColor: Color = PanelBorder,
    backgroundColor: Color = TacticalSurface,
    contentPadding: Dp = 12.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(3.dp))
            .border(2.dp, borderColor, RoundedCornerShape(3.dp))
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun PixelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "",
    icon: ImageVector? = null,
    containerColor: Color = ChampionshipGold,
    contentColor: Color = StadiumMidnight,
    borderColor: Color = PanelHighlight,
    enabled: Boolean = true
) {
    val bg = if (enabled) containerColor else TacticalSurface
    val fg = if (enabled) contentColor else TextMuted
    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
            .clip(RoundedCornerShape(3.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color = bg,
        shape = RoundedCornerShape(3.dp),
        border = BorderStroke(2.dp, if (enabled) borderColor else PanelBorder),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = fg,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ClubCrestBadge(
    shortCode: String,
    primaryColorHex: Long,
    secondaryColorHex: Long,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = this.size.width
            val h = this.size.height
            val shield = Path().apply {
                moveTo(w * 0.1f, h * 0.08f)
                lineTo(w * 0.9f, h * 0.08f)
                lineTo(w * 0.9f, h * 0.62f)
                lineTo(w * 0.5f, h * 0.94f)
                lineTo(w * 0.1f, h * 0.62f)
                close()
            }
            drawPath(shield, Color(primaryColorHex))
            // Center stripe
            drawRect(
                color = Color(secondaryColorHex),
                topLeft = Offset(w * 0.36f, h * 0.12f),
                size = Size(w * 0.28f, h * 0.68f)
            )
            // Pixel border
            drawLine(Color.White, Offset(w * 0.1f, h * 0.08f), Offset(w * 0.9f, h * 0.08f), strokeWidth = 3f)
            drawLine(Color.White, Offset(w * 0.1f, h * 0.08f), Offset(w * 0.1f, h * 0.62f), strokeWidth = 3f)
            drawLine(Color.White, Offset(w * 0.9f, h * 0.08f), Offset(w * 0.9f, h * 0.62f), strokeWidth = 3f)
            drawLine(Color.White, Offset(w * 0.1f, h * 0.62f), Offset(w * 0.5f, h * 0.94f), strokeWidth = 3f)
            drawLine(Color.White, Offset(w * 0.9f, h * 0.62f), Offset(w * 0.5f, h * 0.94f), strokeWidth = 3f)
        }
        Text(
            text = shortCode,
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary,
            modifier = Modifier
                .background(Color(0xAA0D1520), RoundedCornerShape(2.dp))
                .padding(horizontal = 3.dp, vertical = 1.dp)
        )
    }
}

@Composable
fun PixelStatBar(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    maxValue: Int = 100,
    barColor: Color = ArcadeEmerald
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
            Text(text = value.toString(), style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        }
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(StadiumMidnight)
                .border(1.dp, PanelBorder)
        ) {
            val fraction = (value.toFloat() / maxValue).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(barColor)
            )
        }
    }
}
