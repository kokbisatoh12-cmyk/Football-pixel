package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.AppSettings
import com.example.data.MatchDifficulty
import com.example.ui.components.PixelPanel
import com.example.ui.theme.ArcadeEmerald
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.PanelBorder
import com.example.ui.theme.PlayerCyan
import com.example.ui.theme.StadiumMidnight
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StadiumMidnight)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TacticalSurface)
                .border(2.dp, PanelBorder)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("settings_back_btn")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text("GAME SETTINGS", style = MaterialTheme.typography.headlineMedium, color = ChampionshipGold)
        }

        Spacer(Modifier.height(10.dp))

        Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PixelPanel(modifier = Modifier.weight(1f), borderColor = PlayerCyan) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("AUDIO & CONTROLS", style = MaterialTheme.typography.titleMedium, color = PlayerCyan)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("8-Bit Retro SFX", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                        Switch(
                            checked = settings.sfxEnabled,
                            onCheckedChange = { enabled -> onUpdateSettings { it.copy(sfxEnabled = enabled) } },
                            modifier = Modifier.testTag("settings_sfx_switch")
                        )
                    }

                    Column {
                        Text("SFX Volume: ${(settings.sfxVolume * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        Slider(
                            value = settings.sfxVolume,
                            onValueChange = { v -> onUpdateSettings { it.copy(sfxVolume = v) } },
                            colors = SliderDefaults.colors(thumbColor = ChampionshipGold, activeTrackColor = ArcadeEmerald)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Default Dual Virtual Pad", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            Text("2-Player split touch controls", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        }
                        Switch(
                            checked = settings.dualPadEnabled,
                            onCheckedChange = { d -> onUpdateSettings { it.copy(dualPadEnabled = d) } },
                            modifier = Modifier.testTag("settings_dual_pad_switch")
                        )
                    }
                }
            }

            PixelPanel(modifier = Modifier.weight(1f), borderColor = ChampionshipGold) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("MATCHPLAY & AI", style = MaterialTheme.typography.titleMedium, color = ChampionshipGold)

                    Text("AI Difficulty", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MatchDifficulty.entries.forEach { diff ->
                            val sel = settings.difficulty == diff
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (sel) ChampionshipGold else StadiumMidnight)
                                    .border(2.dp, if (sel) ChampionshipGold else PanelBorder)
                                    .clickable { onUpdateSettings { it.copy(difficulty = diff) } }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(diff.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = if (sel) StadiumMidnight else TextPrimary)
                            }
                        }
                    }

                    Text("Match Duration (Min)", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(2, 3, 5).forEach { mins ->
                            val sel = settings.defaultDurationMin == mins
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (sel) ArcadeEmerald else StadiumMidnight)
                                    .border(2.dp, if (sel) ArcadeEmerald else PanelBorder)
                                    .clickable { onUpdateSettings { it.copy(defaultDurationMin = mins) } }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${mins}M", style = MaterialTheme.typography.labelSmall, color = if (sel) StadiumMidnight else TextPrimary)
                            }
                        }
                    }

                    Text("Game Speed", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0.85f to "SLOW", 1.0f to "NORMAL", 1.25f to "FAST").forEach { (spd, label) ->
                            val sel = kotlin.math.abs(settings.defaultSpeed - spd) < 0.05f
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (sel) PlayerCyan else StadiumMidnight)
                                    .border(2.dp, if (sel) PlayerCyan else PanelBorder)
                                    .clickable { onUpdateSettings { it.copy(defaultSpeed = spd) } }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, style = MaterialTheme.typography.labelSmall, color = if (sel) StadiumMidnight else TextPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}
