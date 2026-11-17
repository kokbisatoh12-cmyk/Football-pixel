package com.example.game.input

import android.view.KeyEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.game.engine.ControlInput
import com.example.ui.theme.ArcadeEmerald
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.FoulCrimson
import com.example.ui.theme.PlayerCyan
import com.example.ui.theme.PlayerOrange
import com.example.ui.theme.TextPrimary
import kotlin.math.hypot

class MutableControlState {
    var moveX = 0f
    var moveY = 0f
    var sprint = false
    var shootHolding = false
    private var passQueued = false
    private var throughQueued = false
    private var shootReleaseQueued = false
    private var shootTapQueued = false
    private var switchQueued = false
    private var tackleQueued = false

    fun queuePass() { passQueued = true }
    fun queueThrough() { throughQueued = true }
    fun queueShootTap() { shootTapQueued = true }
    fun queueShootRelease() { shootReleaseQueued = true }
    fun queueSwitch() { switchQueued = true }
    fun queueTackle() { tackleQueued = true }

    fun consumeSnapshot(kbMoveX: Float = 0f, kbMoveY: Float = 0f, kbSprint: Boolean = false): ControlInput {
        val mx = if (kotlin.math.abs(moveX) > 0.05f) moveX else kbMoveX
        val my = if (kotlin.math.abs(moveY) > 0.05f) moveY else kbMoveY
        val inp = ControlInput(
            moveX = mx.coerceIn(-1f, 1f),
            moveY = my.coerceIn(-1f, 1f),
            sprint = sprint || kbSprint,
            passJustPressed = passQueued,
            throughJustPressed = throughQueued,
            shootHolding = shootHolding,
            shootJustReleased = shootReleaseQueued,
            shootQuickTap = shootTapQueued,
            switchJustPressed = switchQueued,
            tackleJustPressed = tackleQueued
        )
        passQueued = false
        throughQueued = false
        shootReleaseQueued = false
        shootTapQueued = false
        switchQueued = false
        tackleQueued = false
        return inp
    }
}

class KeyboardGamepadBridge {
    private val pressedKeys = mutableSetOf<Int>()

    fun handleKeyEvent(
        nativeEvent: KeyEvent,
        p1: MutableControlState,
        p2: MutableControlState
    ): Boolean {
        val code = nativeEvent.keyCode
        if (nativeEvent.action == KeyEvent.ACTION_DOWN) {
            val isFirst = pressedKeys.add(code)
            if (isFirst) {
                when (code) {
                    KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_BUTTON_A -> p1.queuePass()
                    KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_BUTTON_Y -> p1.queueThrough()
                    KeyEvent.KEYCODE_L, KeyEvent.KEYCODE_BUTTON_X -> p1.shootHolding = true
                    KeyEvent.KEYCODE_E, KeyEvent.KEYCODE_TAB, KeyEvent.KEYCODE_BUTTON_L1 -> p1.queueSwitch()
                    KeyEvent.KEYCODE_U, KeyEvent.KEYCODE_NUMPAD_1 -> p2.queuePass()
                    KeyEvent.KEYCODE_I, KeyEvent.KEYCODE_NUMPAD_2 -> p2.shootHolding = true
                    KeyEvent.KEYCODE_O, KeyEvent.KEYCODE_NUMPAD_3 -> p2.queueThrough()
                }
            }
            return isRelevantKey(code)
        } else if (nativeEvent.action == KeyEvent.ACTION_UP) {
            pressedKeys.remove(code)
            when (code) {
                KeyEvent.KEYCODE_L, KeyEvent.KEYCODE_BUTTON_X -> {
                    p1.shootHolding = false
                    p1.queueShootRelease()
                }
                KeyEvent.KEYCODE_I, KeyEvent.KEYCODE_NUMPAD_2 -> {
                    p2.shootHolding = false
                    p2.queueShootRelease()
                }
            }
            return isRelevantKey(code)
        }
        return false
    }

    fun p1MoveX(): Float {
        var x = 0f
        if (KeyEvent.KEYCODE_A in pressedKeys || KeyEvent.KEYCODE_DPAD_LEFT in pressedKeys) x -= 1f
        if (KeyEvent.KEYCODE_D in pressedKeys || KeyEvent.KEYCODE_DPAD_RIGHT in pressedKeys) x += 1f
        return x
    }

    fun p1MoveY(): Float {
        var y = 0f
        if (KeyEvent.KEYCODE_W in pressedKeys || KeyEvent.KEYCODE_DPAD_UP in pressedKeys) y -= 1f
        if (KeyEvent.KEYCODE_S in pressedKeys || KeyEvent.KEYCODE_DPAD_DOWN in pressedKeys) y += 1f
        return y
    }

    fun p1Sprint(): Boolean =
        KeyEvent.KEYCODE_SHIFT_LEFT in pressedKeys || KeyEvent.KEYCODE_BUTTON_R1 in pressedKeys

    fun p2MoveX(): Float {
        var x = 0f
        if (KeyEvent.KEYCODE_NUMPAD_4 in pressedKeys) x -= 1f
        if (KeyEvent.KEYCODE_NUMPAD_6 in pressedKeys) x += 1f
        return x
    }

    fun p2MoveY(): Float {
        var y = 0f
        if (KeyEvent.KEYCODE_NUMPAD_8 in pressedKeys) y -= 1f
        if (KeyEvent.KEYCODE_NUMPAD_5 in pressedKeys) y += 1f
        return y
    }

    private fun isRelevantKey(code: Int): Boolean = code in setOf(
        KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_D,
        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_L, KeyEvent.KEYCODE_E,
        KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_TAB,
        KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_X,
        KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1
    )
}

@Composable
fun VirtualJoystickPad(
    accentColor: Color,
    size: Dp = 112.dp,
    onMove: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "virtual_joystick"
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(size)
            .testTag(testTag)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val center = Offset(this.size.width / 2f, this.size.height / 2f)
                    val maxR = this.size.width * 0.36f
                    fun updateFromPos(pos: Offset) {
                        val dx = pos.x - center.x
                        val dy = pos.y - center.y
                        val dist = hypot(dx, dy)
                        val clamped = if (dist > maxR && dist > 0f) {
                            Offset(dx / dist * maxR, dy / dist * maxR)
                        } else {
                            Offset(dx, dy)
                        }
                        knobOffset = clamped
                        onMove(clamped.x / maxR, clamped.y / maxR)
                    }
                    updateFromPos(down.position)
                    while (true) {
                        val event = awaitPointerEvent()
                        val pointer = event.changes.firstOrNull { it.id == down.id }
                        if (pointer == null || !pointer.pressed) {
                            knobOffset = Offset.Zero
                            onMove(0f, 0f)
                            break
                        }
                        updateFromPos(pointer.position)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(this.size.width / 2f, this.size.height / 2f)
            val r = this.size.minDimension * 0.44f
            drawCircle(Color(0x660D1520), radius = r, center = c)
            drawCircle(accentColor.copy(alpha = 0.65f), radius = r, center = c, style = Stroke(width = 3f))
            // Crosshair guide
            drawLine(accentColor.copy(alpha = 0.28f), Offset(c.x - r * 0.6f, c.y), Offset(c.x + r * 0.6f, c.y), 2f)
            drawLine(accentColor.copy(alpha = 0.28f), Offset(c.x, c.y - r * 0.6f), Offset(c.x, c.y + r * 0.6f), 2f)
            // Thumb knob
            drawCircle(accentColor.copy(alpha = 0.85f), radius = r * 0.42f, center = c + knobOffset)
            drawCircle(Color.White.copy(alpha = 0.7f), radius = r * 0.42f, center = c + knobOffset, style = Stroke(width = 2f))
        }
    }
}

@Composable
fun ArcadeActionButton(
    label: String,
    color: Color,
    size: Dp = 54.dp,
    testTag: String,
    onPressStart: () -> Unit = {},
    onPressEnd: () -> Unit = {},
    onTap: () -> Unit = {}
) {
    var pressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(size)
            .testTag(testTag)
            .background(
                color = if (pressed) color.copy(alpha = 0.92f) else Color(0x990D1520),
                shape = CircleShape
            )
            .border(
                width = if (pressed) 3.dp else 2.dp,
                color = color,
                shape = CircleShape
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downTime = System.currentTimeMillis()
                    pressed = true
                    onPressStart()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) {
                            pressed = false
                            onPressEnd()
                            if (System.currentTimeMillis() - downTime < 220L) {
                                onTap()
                            }
                            break
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary
        )
    }
}

@Composable
fun SinglePlayerTouchOverlay(
    p1HasBall: Boolean,
    controlState: MutableControlState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp)) {
        // Left side: Virtual Joystick
        VirtualJoystickPad(
            accentColor = PlayerCyan,
            size = 116.dp,
            onMove = { mx, my ->
                controlState.moveX = mx
                controlState.moveY = my
            },
            modifier = Modifier.align(Alignment.BottomStart),
            testTag = "p1_joystick"
        )

        // Right side: 4-button arcade cluster (Pass/Switch, Through, Shoot/Tackle, Sprint)
        Column(
            modifier = Modifier.align(Alignment.BottomEnd),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ArcadeActionButton(
                    label = if (p1HasBall) "THRU" else "SWCH",
                    color = PlayerCyan,
                    size = 52.dp,
                    testTag = "p1_btn_through_switch",
                    onTap = {
                        if (p1HasBall) controlState.queueThrough() else controlState.queueSwitch()
                    }
                )
                ArcadeActionButton(
                    label = "SPRT",
                    color = ArcadeEmerald,
                    size = 52.dp,
                    testTag = "p1_btn_sprint",
                    onPressStart = { controlState.sprint = true },
                    onPressEnd = { controlState.sprint = false }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ArcadeActionButton(
                    label = if (p1HasBall) "PASS" else "INTC",
                    color = ArcadeEmerald,
                    size = 58.dp,
                    testTag = "p1_btn_pass",
                    onTap = {
                        if (p1HasBall) controlState.queuePass() else controlState.queueSwitch()
                    }
                )
                ArcadeActionButton(
                    label = if (p1HasBall) "SHOT" else "SLID",
                    color = if (p1HasBall) ChampionshipGold else FoulCrimson,
                    size = 62.dp,
                    testTag = "p1_btn_shoot_tackle",
                    onPressStart = {
                        if (p1HasBall) controlState.shootHolding = true else controlState.queueTackle()
                    },
                    onPressEnd = {
                        if (controlState.shootHolding) {
                            controlState.shootHolding = false
                            controlState.queueShootRelease()
                        }
                    },
                    onTap = {
                        if (p1HasBall) controlState.queueShootTap() else controlState.queueTackle()
                    }
                )
            }
        }
    }
}

@Composable
fun DualPadTouchOverlay(
    p1HasBall: Boolean,
    p2HasBall: Boolean,
    p1State: MutableControlState,
    p2State: MutableControlState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp)) {
        // P1 Left Edge Cluster
        Row(
            modifier = Modifier.align(Alignment.BottomStart),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VirtualJoystickPad(
                accentColor = PlayerCyan,
                size = 96.dp,
                onMove = { mx, my -> p1State.moveX = mx; p1State.moveY = my },
                testTag = "dual_p1_joystick"
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ArcadeActionButton(
                    label = if (p1HasBall) "PASS" else "SW",
                    color = ArcadeEmerald,
                    size = 48.dp,
                    testTag = "dual_p1_pass",
                    onTap = { if (p1HasBall) p1State.queuePass() else p1State.queueSwitch() }
                )
                ArcadeActionButton(
                    label = if (p1HasBall) "SHOT" else "TKL",
                    color = ChampionshipGold,
                    size = 48.dp,
                    testTag = "dual_p1_shoot",
                    onPressStart = { if (p1HasBall) p1State.shootHolding = true else p1State.queueTackle() },
                    onPressEnd = {
                        if (p1State.shootHolding) {
                            p1State.shootHolding = false
                            p1State.queueShootRelease()
                        }
                    },
                    onTap = { if (p1HasBall) p1State.queueShootTap() else p1State.queueTackle() }
                )
            }
        }

        // P2 Right Edge Cluster
        Row(
            modifier = Modifier.align(Alignment.BottomEnd),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ArcadeActionButton(
                    label = if (p2HasBall) "PASS" else "SW",
                    color = ArcadeEmerald,
                    size = 48.dp,
                    testTag = "dual_p2_pass",
                    onTap = { if (p2HasBall) p2State.queuePass() else p2State.queueSwitch() }
                )
                ArcadeActionButton(
                    label = if (p2HasBall) "SHOT" else "TKL",
                    color = PlayerOrange,
                    size = 48.dp,
                    testTag = "dual_p2_shoot",
                    onPressStart = { if (p2HasBall) p2State.shootHolding = true else p2State.queueTackle() },
                    onPressEnd = {
                        if (p2State.shootHolding) {
                            p2State.shootHolding = false
                            p2State.queueShootRelease()
                        }
                    },
                    onTap = { if (p2HasBall) p2State.queueShootTap() else p2State.queueTackle() }
                )
            }
            VirtualJoystickPad(
                accentColor = PlayerOrange,
                size = 96.dp,
                onMove = { mx, my -> p2State.moveX = mx; p2State.moveY = my },
                testTag = "dual_p2_joystick"
            )
        }
    }
}
