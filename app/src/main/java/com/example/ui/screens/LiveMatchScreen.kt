package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.MatchSides
import com.example.data.PlayerEntity
import com.example.game.engine.MatchPhase
import com.example.game.engine.MatchSnapshot
import com.example.game.input.DualPadTouchOverlay
import com.example.game.input.KeyboardGamepadBridge
import com.example.game.input.MutableControlState
import com.example.game.input.SinglePlayerTouchOverlay
import com.example.game.render.Pitch25DRenderer.renderMatch
import com.example.game.render.PixelSpriteFactory
import com.example.ui.components.ClubCrestBadge
import com.example.ui.components.PixelButton
import com.example.ui.components.PixelPanel
import com.example.ui.theme.ArcadeEmerald
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.FoulCrimson
import com.example.ui.theme.PanelBorder
import com.example.ui.theme.PlayerCyan
import com.example.ui.theme.PlayerOrange
import com.example.ui.theme.StadiumMidnight
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun LiveMatchScreen(
    snapshot: MatchSnapshot?,
    sides: MatchSides,
    isPaused: Boolean,
    sfxEnabled: Boolean,
    p1Controls: MutableControlState,
    p2Controls: MutableControlState,
    keyBridge: KeyboardGamepadBridge,
    onTogglePause: () -> Unit,
    onToggleSfx: () -> Unit,
    onResumeHalftime: () -> Unit,
    onSubstitute: (Boolean, Int, PlayerEntity) -> Unit,
    onExitMatch: () -> Unit
) {
    val sprites = remember { PixelSpriteFactory() }
    val focusRequester = remember { FocusRequester() }
    var showSubsModal by remember { mutableStateOf(false) }

    BackHandler {
        if (!isPaused) onTogglePause() else onExitMatch()
    }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    if (snapshot == null) {
        Box(modifier = Modifier.fillMaxSize().background(StadiumMidnight), contentAlignment = Alignment.Center) {
            Text("LOADING PITCH...", style = MaterialTheme.typography.headlineMedium, color = ChampionshipGold)
        }
        return
    }

    val p1Player = snapshot.players.firstOrNull { it.isControlledByP1 }
    val p2Player = snapshot.players.firstOrNull { it.isControlledByP2 }
    val p1HasBall = p1Player != null && snapshot.ball.ownerSimId == p1Player.simId
    val p2HasBall = p2Player != null && snapshot.ball.ownerSimId == p2Player.simId

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StadiumMidnight)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { keyBridge.handleKeyEvent(it.nativeKeyEvent, p1Controls, p2Controls) }
    ) {
        // 1. 2.5D Pixel Pitch Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            renderMatch(snapshot, sprites)
        }

        // 2. Top Retro Scoreboard HUD (Inspired by Screenshot #4)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left active player stamina tag
            if (p1Player != null) {
                PixelPanel(
                    backgroundColor = Color(0xBB0D1520),
                    borderColor = PlayerCyan,
                    contentPadding = 6.dp
                ) {
                    Column {
                        Text(
                            text = "${p1Player.shirtNumber} ${p1Player.name.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = PlayerCyan
                        )
                        Spacer(Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .height(6.dp)
                                .background(StadiumMidnight)
                                .border(1.dp, PanelBorder)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((p1Player.stamina / 100f).coerceIn(0f, 1f))
                                    .height(6.dp)
                                    .background(if (p1Player.stamina > 30f) ArcadeEmerald else FoulCrimson)
                            )
                        }
                    }
                }
            } else {
                Spacer(Modifier.width(100.dp))
            }

            // Center Scoreboard or Training Banner
            if (snapshot.isTrainingMode) {
                PixelPanel(
                    backgroundColor = Color(0xDD0D1520),
                    borderColor = ChampionshipGold,
                    contentPadding = 8.dp
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = snapshot.trainingDrill?.title?.uppercase() ?: "TRAINING",
                            style = MaterialTheme.typography.labelSmall,
                            color = PlayerCyan
                        )
                        Text(
                            text = "SCORE ${snapshot.trainingScore}",
                            style = MaterialTheme.typography.titleMedium,
                            color = ChampionshipGold
                        )
                        Text(
                            text = "${snapshot.trainingTimeLeft.toInt()}S",
                            style = MaterialTheme.typography.titleMedium,
                            color = ArcadeEmerald
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .background(Color(0xDD0D1520), RoundedCornerShape(3.dp))
                        .border(2.dp, PanelBorder, RoundedCornerShape(3.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ClubCrestBadge(snapshot.homeClub.shortCode, snapshot.homeClub.primaryColorHex, snapshot.homeClub.secondaryColorHex, size = 26.dp)
                    Text(snapshot.homeClub.shortCode, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(snapshot.homeScore.toString(), style = MaterialTheme.typography.headlineMedium, color = ChampionshipGold)

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (snapshot.half == 1) "1ST" else "2ND",
                            style = MaterialTheme.typography.labelSmall,
                            color = ArcadeEmerald
                        )
                        Text(
                            text = "${snapshot.displayMinute}'",
                            style = MaterialTheme.typography.titleMedium,
                            color = PlayerCyan
                        )
                    }

                    Text(snapshot.awayScore.toString(), style = MaterialTheme.typography.headlineMedium, color = ChampionshipGold)
                    Text(snapshot.awayClub.shortCode, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    ClubCrestBadge(snapshot.awayClub.shortCode, snapshot.awayClub.primaryColorHex, snapshot.awayClub.secondaryColorHex, size = 26.dp)
                }
            }

            // Right Controls: Mute & Pause/Subs
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = onToggleSfx,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xBB0D1520), RoundedCornerShape(3.dp))
                        .border(2.dp, PanelBorder, RoundedCornerShape(3.dp))
                        .testTag("match_mute_btn")
                ) {
                    Icon(
                        imageVector = if (sfxEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "Toggle Sound",
                        tint = TextPrimary
                    )
                }
                IconButton(
                    onClick = onTogglePause,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xBB0D1520), RoundedCornerShape(3.dp))
                        .border(2.dp, ChampionshipGold, RoundedCornerShape(3.dp))
                        .testTag("match_pause_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause Match",
                        tint = ChampionshipGold
                    )
                }
            }
        }

        // 3. Dynamic Match Event Banner (GOAL! / OFFSIDE / PENALTY / CORNER)
        val banner = snapshot.banner
        if (banner != null && snapshot.phase != MatchPhase.HALFTIME && snapshot.phase != MatchPhase.FULLTIME) {
            PixelPanel(
                modifier = Modifier.align(Alignment.Center).padding(bottom = 110.dp),
                borderColor = Color(banner.accentHex),
                backgroundColor = Color(0xE80D1520)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(banner.title, style = MaterialTheme.typography.displayLarge, color = Color(banner.accentHex))
                    if (banner.subtitle.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(banner.subtitle, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                }
            }
        }

        // 4. Touch Controls (Single-Player OR Dual-Pad Local Multiplayer)
        if (!isPaused && snapshot.phase != MatchPhase.HALFTIME && snapshot.phase != MatchPhase.FULLTIME) {
            Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                if (sides == MatchSides.P1_VS_P2 && !snapshot.isTrainingMode) {
                    DualPadTouchOverlay(
                        p1HasBall = p1HasBall,
                        p2HasBall = p2HasBall,
                        p1State = p1Controls,
                        p2State = p2Controls
                    )
                } else {
                    SinglePlayerTouchOverlay(
                        p1HasBall = p1HasBall,
                        controlState = p1Controls
                    )
                }
            }
        }

        // 5. Pause & Substitutions Overlay
        if (isPaused) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xCC0D1520)),
                contentAlignment = Alignment.Center
            ) {
                PixelPanel(
                    modifier = Modifier.width(if (showSubsModal) 520.dp else 340.dp),
                    borderColor = ChampionshipGold
                ) {
                    if (showSubsModal) {
                        SubstitutionPanel(
                            snapshot = snapshot,
                            onSubstitute = onSubstitute,
                            onClose = { showSubsModal = false }
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("MATCH PAUSED", style = MaterialTheme.typography.headlineMedium, color = ChampionshipGold)
                            PixelButton(
                                text = "RESUME",
                                onClick = onTogglePause,
                                containerColor = ArcadeEmerald,
                                testTag = "pause_resume_btn",
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (!snapshot.isTrainingMode) {
                                PixelButton(
                                    text = "SUBSTITUTIONS",
                                    icon = Icons.Default.SwapHoriz,
                                    onClick = { showSubsModal = true },
                                    containerColor = PlayerCyan,
                                    testTag = "pause_subs_btn",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            PixelButton(
                                text = "QUIT TO MENU",
                                onClick = onExitMatch,
                                containerColor = FoulCrimson,
                                contentColor = TextPrimary,
                                testTag = "pause_quit_btn",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // 6. Halftime Overlay
        if (snapshot.phase == MatchPhase.HALFTIME && !isPaused) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xCC0D1520)),
                contentAlignment = Alignment.Center
            ) {
                PixelPanel(modifier = Modifier.width(420.dp), borderColor = ChampionshipGold) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("HALF TIME", style = MaterialTheme.typography.headlineMedium, color = ChampionshipGold)
                        Text(
                            text = "${snapshot.homeClub.shortCode} ${snapshot.homeScore} - ${snapshot.awayScore} ${snapshot.awayClub.shortCode}",
                            style = MaterialTheme.typography.displayLarge,
                            color = TextPrimary
                        )
                        MatchStatsRows(snapshot)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PixelButton(
                                text = "SUBS",
                                icon = Icons.Default.SwapHoriz,
                                onClick = { showSubsModal = true; onTogglePause() },
                                containerColor = PlayerCyan,
                                testTag = "halftime_subs_btn",
                                modifier = Modifier.weight(1f)
                            )
                            PixelButton(
                                text = "2ND HALF",
                                onClick = onResumeHalftime,
                                containerColor = ArcadeEmerald,
                                testTag = "halftime_resume_btn",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 7. Fulltime / Drill Complete Modal
        if (snapshot.phase == MatchPhase.FULLTIME) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xD80D1520)),
                contentAlignment = Alignment.Center
            ) {
                PixelPanel(modifier = Modifier.width(440.dp), borderColor = ChampionshipGold) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (snapshot.isTrainingMode) {
                            Text("DRILL COMPLETE!", style = MaterialTheme.typography.headlineMedium, color = ArcadeEmerald)
                            Text("FINAL SCORE: ${snapshot.trainingScore} PTS", style = MaterialTheme.typography.titleLarge, color = ChampionshipGold)
                        } else {
                            Text("FULL TIME", style = MaterialTheme.typography.headlineMedium, color = ChampionshipGold)
                            Text(
                                text = "${snapshot.homeClub.shortCode} ${snapshot.homeScore} - ${snapshot.awayScore} ${snapshot.awayClub.shortCode}",
                                style = MaterialTheme.typography.displayLarge,
                                color = TextPrimary
                            )
                            MatchStatsRows(snapshot)
                            if (snapshot.scorerLogs.isNotEmpty()) {
                                Text(
                                    text = snapshot.scorerLogs.take(4).joinToString(" • "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ArcadeEmerald
                                )
                            }
                        }
                        PixelButton(
                            text = "CONTINUE",
                            onClick = onExitMatch,
                            containerColor = ChampionshipGold,
                            testTag = "fulltime_continue_btn",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchStatsRows(snapshot: MatchSnapshot) {
    val totalPoss = (snapshot.homePossessionTicks + snapshot.awayPossessionTicks).coerceAtLeast(1)
    val homePct = (snapshot.homePossessionTicks * 100) / totalPoss
    val awayPct = 100 - homePct
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        StatCompareRow("POSSESSION", "$homePct%", "$awayPct%")
        StatCompareRow("SHOTS", "${snapshot.homeShots}", "${snapshot.awayShots}")
        StatCompareRow("FOULS", "${snapshot.homeFouls}", "${snapshot.awayFouls}")
    }
}

@Composable
private fun StatCompareRow(label: String, left: String, right: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(left, style = MaterialTheme.typography.bodyLarge, color = PlayerCyan)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        Text(right, style = MaterialTheme.typography.bodyLarge, color = ChampionshipGold)
    }
}

@Composable
private fun SubstitutionPanel(
    snapshot: MatchSnapshot,
    onSubstitute: (Boolean, Int, PlayerEntity) -> Unit,
    onClose: () -> Unit
) {
    var selectedOutSimId by remember { mutableStateOf<Int?>(null) }
    val activeHome = snapshot.players.filter { it.isHome && !it.sentOff }
    val benchHome = snapshot.homeBench

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SUBSTITUTIONS (${snapshot.homeClub.shortCode})", style = MaterialTheme.typography.titleMedium, color = ChampionshipGold)
            PixelButton(text = "DONE", onClick = onClose, containerColor = TacticalSurface, contentColor = TextPrimary)
        }
        Text(
            text = if (selectedOutSimId == null) "1. Tap an on-pitch player to replace:" else "2. Tap a bench player to sub in:",
            style = MaterialTheme.typography.bodyMedium,
            color = ArcadeEmerald
        )
        Row(modifier = Modifier.height(190.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(activeHome) { p ->
                    val isSel = p.simId == selectedOutSimId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSel) PlayerCyan.copy(alpha = 0.25f) else StadiumMidnight)
                            .border(1.dp, if (isSel) PlayerCyan else PanelBorder)
                            .clickable { selectedOutSimId = p.simId }
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${p.role.label} ${p.name}", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text("${p.stamina.toInt()}%", style = MaterialTheme.typography.bodyMedium, color = if (p.stamina > 40f) ArcadeEmerald else FoulCrimson)
                    }
                }
            }
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(benchHome) { sub ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StadiumMidnight)
                            .border(1.dp, PanelBorder)
                            .clickable(enabled = selectedOutSimId != null) {
                                val outId = selectedOutSimId ?: return@clickable
                                onSubstitute(true, outId, sub)
                                selectedOutSimId = null
                            }
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${sub.position.label} ${sub.name}", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text("OVR ${sub.ovr}", style = MaterialTheme.typography.bodyMedium, color = ChampionshipGold)
                    }
                }
            }
        }
    }
}
