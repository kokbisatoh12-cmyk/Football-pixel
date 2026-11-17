package com.example.game.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.data.PlayerAnim
import com.example.data.TimeOfDay
import com.example.data.WeatherType
import com.example.game.engine.MatchPhase
import com.example.game.engine.MatchSnapshot
import com.example.game.engine.PitchConstants
import com.example.game.engine.SimPlayer
import com.example.ui.theme.CardYellow
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.PitchGrassDark
import com.example.ui.theme.PitchGrassLight
import com.example.ui.theme.PitchLineWhite
import com.example.ui.theme.PlayerCyan
import com.example.ui.theme.PlayerOrange
import com.example.ui.theme.StadiumStandDark
import com.example.ui.theme.StadiumWallBlue
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object Pitch25DRenderer {

    fun DrawScope.renderMatch(
        snapshot: MatchSnapshot,
        sprites: PixelSpriteFactory
    ) {
        val sw = size.width
        val sh = size.height
        val zoom = (sw / 760f).coerceIn(0.85f, 2.4f)
        val camX = snapshot.cameraX
        val camY = snapshot.cameraY

        // Helper: 2.5D slightly diagonal perspective projection
        fun project(wx: Float, wy: Float, wz: Float = 0f): Offset {
            val dx = wx - camX
            val dy = wy - camY
            val persp = 1f + (dy / 1400f) * 0.16f
            val sx = sw * 0.5f + dx * zoom * persp
            val sy = sh * 0.53f + dy * zoom * 0.72f - wz * zoom
            return Offset(sx, sy)
        }

        // 1. Stadium apron & sky background
        val bgColor = if (snapshot.timeOfDay == TimeOfDay.NIGHT) Color(0xFF0C1421) else Color(0xFF1F4F35)
        drawRect(bgColor)

        // Draw stadium outer surround & turf border
        val apronTL = project(-130f, -150f)
        val apronTR = project(PitchConstants.PITCH_W + 130f, -150f)
        val apronBR = project(PitchConstants.PITCH_W + 130f, PitchConstants.PITCH_H + 80f)
        val apronBL = project(-130f, PitchConstants.PITCH_H + 80f)
        drawQuad(apronTL, apronTR, apronBR, apronBL, Color(0xFF183B28))

        // Upper stadium stands (above top touchline)
        val standTopLeft = project(-90f, -165f)
        val standTopRight = project(PitchConstants.PITCH_W + 90f, -165f)
        val standBotRight = project(PitchConstants.PITCH_W + 65f, -34f)
        val standBotLeft = project(-65f, -34f)
        drawQuad(standTopLeft, standTopRight, standBotRight, standBotLeft, StadiumStandDark)

        // Advertising boards along top touchline
        val boardTL = project(-40f, -34f)
        val boardTR = project(PitchConstants.PITCH_W + 40f, -34f)
        val boardBR = project(PitchConstants.PITCH_W + 40f, -14f)
        val boardBL = project(-40f, -14f)
        drawQuad(boardTL, boardTR, boardBR, boardBL, StadiumWallBlue)
        drawLine(Color(0xFF7DD3FC), boardTL, boardTR, strokeWidth = 2f * zoom)

        // Animated pixel spectators in 3 rows along top stand
        val animTime = snapshot.tickCount / 60f
        val jumpSpeed = if (snapshot.phase == MatchPhase.GOAL_CELEBRATION) 14f else 4f
        val jumpAmp = if (snapshot.phase == MatchPhase.GOAL_CELEBRATION) 6f else 1.5f + snapshot.crowdExcitement * 3f
        for (row in 0..2) {
            val wy = -125f + row * 28f
            var wx = 10f
            var idx = row * 17
            while (wx <= PitchConstants.PITCH_W - 10f) {
                val p = project(wx, wy)
                if (p.x in -30f..(sw + 30f)) {
                    val bounce = abs(sin(animTime * jumpSpeed + idx * 0.7f)) * jumpAmp * zoom
                    val cSprite = sprites.getCrowdSprite(idx)
                    val cw = (12f * zoom).toInt().coerceAtLeast(6)
                    val ch = (15f * zoom).toInt().coerceAtLeast(8)
                    drawImage(
                        image = cSprite,
                        dstOffset = IntOffset((p.x - cw / 2f).toInt(), (p.y - ch - bounce).toInt()),
                        dstSize = IntSize(cw, ch),
                        filterQuality = FilterQuality.None
                    )
                }
                wx += 30f
                idx++
            }
        }

        // 2. Checkered green pixel grass pitch
        val stripeCols = 14
        val stripeRows = 8
        val colW = PitchConstants.PITCH_W / stripeCols
        val rowH = PitchConstants.PITCH_H / stripeRows
        for (c in 0 until stripeCols) {
            for (r in 0 until stripeRows) {
                val x0 = c * colW
                val y0 = r * rowH
                val checkerColor = if ((c + r) % 2 == 0) PitchGrassDark else PitchGrassLight
                drawQuad(
                    project(x0, y0),
                    project(x0 + colW, y0),
                    project(x0 + colW, y0 + rowH),
                    project(x0, y0 + rowH),
                    checkerColor
                )
            }
        }

        // 3. White chalk pitch markings
        val lineW = (2.4f * zoom).coerceAtLeast(1.5f)
        fun drawPitchLine(x1: Float, y1: Float, x2: Float, y2: Float) {
            drawLine(PitchLineWhite, project(x1, y1), project(x2, y2), strokeWidth = lineW)
        }
        // Outer boundary
        drawPitchLine(0f, 0f, PitchConstants.PITCH_W, 0f)
        drawPitchLine(PitchConstants.PITCH_W, 0f, PitchConstants.PITCH_W, PitchConstants.PITCH_H)
        drawPitchLine(PitchConstants.PITCH_W, PitchConstants.PITCH_H, 0f, PitchConstants.PITCH_H)
        drawPitchLine(0f, PitchConstants.PITCH_H, 0f, 0f)
        // Halfway line & center circle
        drawPitchLine(PitchConstants.CENTER_X, 0f, PitchConstants.CENTER_X, PitchConstants.PITCH_H)
        drawProjectedCircle(PitchConstants.CENTER_X, PitchConstants.CENTER_Y, 88f, ::project, PitchLineWhite, lineW)
        drawCircle(PitchLineWhite, radius = 3.5f * zoom, center = project(PitchConstants.CENTER_X, PitchConstants.CENTER_Y))

        // Left & Right penalty boxes + 6-yard boxes + penalty spots
        val pbTop = PitchConstants.CENTER_Y - PitchConstants.PENALTY_BOX_HALF_H
        val pbBot = PitchConstants.CENTER_Y + PitchConstants.PENALTY_BOX_HALF_H
        val syTop = PitchConstants.CENTER_Y - PitchConstants.SIX_YARD_HALF_H
        val syBot = PitchConstants.CENTER_Y + PitchConstants.SIX_YARD_HALF_H

        // Left boxes
        drawPitchLine(0f, pbTop, PitchConstants.PENALTY_BOX_W, pbTop)
        drawPitchLine(PitchConstants.PENALTY_BOX_W, pbTop, PitchConstants.PENALTY_BOX_W, pbBot)
        drawPitchLine(PitchConstants.PENALTY_BOX_W, pbBot, 0f, pbBot)
        drawPitchLine(0f, syTop, PitchConstants.SIX_YARD_W, syTop)
        drawPitchLine(PitchConstants.SIX_YARD_W, syTop, PitchConstants.SIX_YARD_W, syBot)
        drawPitchLine(PitchConstants.SIX_YARD_W, syBot, 0f, syBot)
        drawCircle(PitchLineWhite, radius = 3f * zoom, center = project(PitchConstants.PENALTY_SPOT_OFFSET, PitchConstants.CENTER_Y))

        // Right boxes
        val rPbX = PitchConstants.PITCH_W - PitchConstants.PENALTY_BOX_W
        val rSyX = PitchConstants.PITCH_W - PitchConstants.SIX_YARD_W
        drawPitchLine(PitchConstants.PITCH_W, pbTop, rPbX, pbTop)
        drawPitchLine(rPbX, pbTop, rPbX, pbBot)
        drawPitchLine(rPbX, pbBot, PitchConstants.PITCH_W, pbBot)
        drawPitchLine(PitchConstants.PITCH_W, syTop, rSyX, syTop)
        drawPitchLine(rSyX, syTop, rSyX, syBot)
        drawPitchLine(rSyX, syBot, PitchConstants.PITCH_W, syBot)
        drawCircle(PitchLineWhite, radius = 3f * zoom, center = project(PitchConstants.PITCH_W - PitchConstants.PENALTY_SPOT_OFFSET, PitchConstants.CENTER_Y))

        // Corner flags
        drawCornerFlag(project(0f, 0f), zoom, Color(0xFFEF4444))
        drawCornerFlag(project(PitchConstants.PITCH_W, 0f), zoom, Color(0xFFEF4444))
        drawCornerFlag(project(0f, PitchConstants.PITCH_H), zoom, Color(0xFFEF4444))
        drawCornerFlag(project(PitchConstants.PITCH_W, PitchConstants.PITCH_H), zoom, Color(0xFFEF4444))

        // Training targets / cones if in Training mode
        if (snapshot.isTrainingMode) {
            snapshot.trainingTargets.forEach { target ->
                val tp = project(target.x, target.y)
                if (target.isCone) {
                    val col = if (target.hit) Color(0xFF00D084) else Color(0xFFFF8A2B)
                    drawCircle(Color(0x66000000), radius = 8f * zoom, center = Offset(tp.x, tp.y + 3f * zoom))
                    val conePath = Path().apply {
                        moveTo(tp.x, tp.y - 14f * zoom)
                        lineTo(tp.x - 7f * zoom, tp.y + 3f * zoom)
                        lineTo(tp.x + 7f * zoom, tp.y + 3f * zoom)
                        close()
                    }
                    drawPath(conePath, col)
                } else {
                    val ringCol = if (target.hit) Color(0xFF00D084) else ChampionshipGold
                    drawCircle(
                        color = ringCol.copy(alpha = 0.25f),
                        radius = target.radius * zoom * 0.75f,
                        center = tp
                    )
                    drawCircle(
                        color = ringCol,
                        radius = target.radius * zoom * 0.75f,
                        center = tp,
                        style = Stroke(width = 2.5f * zoom)
                    )
                }
            }
        }

        // 4. Depth-sorted Goals + Players + Ball
        drawGoal25D(isLeft = true, project = ::project, zoom = zoom)
        drawGoal25D(isLeft = false, project = ::project, zoom = zoom)

        val sortedPlayers = snapshot.players.filter { !it.sentOff }.sortedBy { it.y }
        var ballDrawn = false
        for (player in sortedPlayers) {
            if (!ballDrawn && snapshot.ball.y < player.y) {
                drawBall25D(snapshot, sprites, ::project, zoom)
                ballDrawn = true
            }
            drawPlayer25D(player, snapshot, sprites, ::project, zoom)
        }
        if (!ballDrawn) {
            drawBall25D(snapshot, sprites, ::project, zoom)
        }

        // 5. Weather overlay (Rain / Storm)
        if (snapshot.weather != WeatherType.CLEAR) {
            val dropCount = if (snapshot.weather == WeatherType.STORM) 70 else 36
            val t = snapshot.tickCount
            val rainColor = Color(0x66B8E6FE)
            for (i in 0 until dropCount) {
                val rx = ((i * 137 + t * 14) % sw.toInt().coerceAtLeast(1)).toFloat()
                val ry = ((i * 97 + t * 22) % sh.toInt().coerceAtLeast(1)).toFloat()
                drawLine(rainColor, Offset(rx, ry), Offset(rx - 5f, ry + 14f), strokeWidth = 1.6f)
            }
            if (snapshot.weather == WeatherType.STORM && (t % 240L) in 0L..4L) {
                drawRect(Color(0x33FFFFFF))
            }
        }

        // 6. Night floodlight vignette
        if (snapshot.timeOfDay == TimeOfDay.NIGHT) {
            drawRect(Color(0x14081224))
        }

        // 7. Bottom-Center Radar Minimap
        drawRadarMinimap(snapshot, sw, sh)
    }

    private fun DrawScope.drawPlayer25D(
        p: SimPlayer,
        snapshot: MatchSnapshot,
        sprites: PixelSpriteFactory,
        project: (Float, Float, Float) -> Offset,
        zoom: Float
    ) {
        val groundPos = project(p.x, p.y, 0f)
        val spritePos = project(p.x, p.y, p.z)

        // Shadow on turf
        drawOval(
            color = Color(0x55000000),
            topLeft = Offset(groundPos.x - 10f * zoom, groundPos.y - 4f * zoom),
            size = Size(20f * zoom, 8f * zoom)
        )

        // Active player indicator ring & triangle (like reference image #4)
        if (p.isControlledByP1 || p.isControlledByP2) {
            val ringColor = if (p.isControlledByP1) PlayerCyan else PlayerOrange
            drawOval(
                color = ringColor,
                topLeft = Offset(groundPos.x - 13f * zoom, groundPos.y - 6f * zoom),
                size = Size(26f * zoom, 12f * zoom),
                style = Stroke(width = 2.4f * zoom)
            )
            // Overhead indicator triangle
            val triTop = spritePos.y - 38f * zoom
            val tri = Path().apply {
                moveTo(spritePos.x - 5f * zoom, triTop - 6f * zoom)
                lineTo(spritePos.x + 5f * zoom, triTop - 6f * zoom)
                lineTo(spritePos.x, triTop)
                close()
            }
            drawPath(tri, ringColor)

            // Shot charge meter if holding shoot
            val charge = if (p.isControlledByP1) snapshot.p1ShootCharge else snapshot.p2ShootCharge
            if (charge > 0.02f) {
                val barW = 24f * zoom
                val barH = 4f * zoom
                val bx = spritePos.x - barW / 2f
                val by = triTop - 13f * zoom
                drawRect(Color(0xCC0D1520), Offset(bx, by), Size(barW, barH))
                val barColor = if (charge > 0.78f) Color(0xFFFF4757) else ChampionshipGold
                drawRect(barColor, Offset(bx, by), Size(barW * charge.coerceIn(0f, 1f), barH))
            }
        }

        // Pixel sprite bitmap
        val bmp = sprites.getPlayerSprite(
            primaryHex = p.primaryColor,
            secondaryHex = p.secondaryColor,
            shortsHex = p.shortsColor,
            skinIdx = p.skinToneIndex,
            hairIdx = p.hairStyleIndex,
            isGk = p.isGk,
            anim = p.anim,
            animTimer = p.animTimer,
            facingRight = p.facingRight
        )
        val spriteW = (24f * zoom).toInt().coerceAtLeast(12)
        val spriteH = (33f * zoom).toInt().coerceAtLeast(16)
        drawImage(
            image = bmp,
            dstOffset = IntOffset(
                (spritePos.x - spriteW * 0.5f).toInt(),
                (spritePos.y - spriteH + 3f * zoom).toInt()
            ),
            dstSize = IntSize(spriteW, spriteH),
            filterQuality = FilterQuality.None
        )

        // Yellow card micro badge if booked
        if (p.yellowCards > 0) {
            drawRect(
                color = CardYellow,
                topLeft = Offset(spritePos.x + 9f * zoom, spritePos.y - 31f * zoom),
                size = Size(4f * zoom, 6f * zoom)
            )
        }
    }

    private fun DrawScope.drawBall25D(
        snapshot: MatchSnapshot,
        sprites: PixelSpriteFactory,
        project: (Float, Float, Float) -> Offset,
        zoom: Float
    ) {
        val ball = snapshot.ball
        val shadowPos = project(ball.x, ball.y, 0f)
        val ballPos = project(ball.x, ball.y, ball.z)
        val shadowScale = (1f - ball.z / 160f).coerceIn(0.45f, 1f)

        drawOval(
            color = Color(0x66000000),
            topLeft = Offset(shadowPos.x - 6f * zoom * shadowScale, shadowPos.y - 3f * zoom * shadowScale),
            size = Size(12f * zoom * shadowScale, 6f * zoom * shadowScale)
        )

        val ballSize = (12f * zoom).toInt().coerceAtLeast(8)
        drawImage(
            image = sprites.getBallSprite(ball.rotationAngle),
            dstOffset = IntOffset(
                (ballPos.x - ballSize * 0.5f).toInt(),
                (ballPos.y - ballSize * 0.85f).toInt()
            ),
            dstSize = IntSize(ballSize, ballSize),
            filterQuality = FilterQuality.None
        )
    }

    private fun DrawScope.drawGoal25D(
        isLeft: Boolean,
        project: (Float, Float, Float) -> Offset,
        zoom: Float
    ) {
        val lineX = if (isLeft) 0f else PitchConstants.PITCH_W
        val backX = if (isLeft) -PitchConstants.GOAL_DEPTH else PitchConstants.PITCH_W + PitchConstants.GOAL_DEPTH
        val topY = PitchConstants.GOAL_TOP_Y
        val botY = PitchConstants.GOAL_BOTTOM_Y
        val gh = PitchConstants.GOAL_HEIGHT

        val fTopBase = project(lineX, topY, 0f)
        val fBotBase = project(lineX, botY, 0f)
        val fTopBar = project(lineX, topY, gh)
        val fBotBar = project(lineX, botY, gh)

        val bTopBase = project(backX, topY, 0f)
        val bBotBase = project(backX, botY, 0f)
        val bTopBar = project(backX, topY, gh * 0.88f)
        val bBotBar = project(backX, botY, gh * 0.88f)

        // Net translucent backdrop + grid mesh
        val netColor = Color(0x44FFFFFF)
        val meshColor = Color(0x88E2E8F0)
        drawQuad(fTopBar, bTopBar, bTopBase, fTopBase, netColor)
        drawQuad(bTopBar, bBotBar, bBotBase, bTopBase, netColor)
        drawQuad(fBotBar, bBotBar, bBotBase, fBotBase, netColor)

        // Vertical net lines
        for (i in 1..4) {
            val t = i / 5f
            val y = topY + (botY - topY) * t
            drawLine(meshColor, project(backX, y, 0f), project(backX, y, gh * 0.88f), strokeWidth = 1f * zoom)
        }
        // Horizontal net lines
        for (i in 1..3) {
            val h = gh * 0.88f * (i / 4f)
            drawLine(meshColor, project(backX, topY, h), project(backX, botY, h), strokeWidth = 1f * zoom)
        }

        // White 2.5D goal posts and crossbar
        val postStroke = (3.4f * zoom).coerceAtLeast(2f)
        val supportStroke = (2f * zoom).coerceAtLeast(1.2f)
        drawLine(Color(0xFFCBD5E1), fTopBar, bTopBar, strokeWidth = supportStroke)
        drawLine(Color(0xFFCBD5E1), fBotBar, bBotBar, strokeWidth = supportStroke)
        drawLine(Color(0xFFCBD5E1), bTopBar, bBotBar, strokeWidth = supportStroke)
        drawLine(Color(0xFFCBD5E1), bTopBase, bTopBar, strokeWidth = supportStroke)
        drawLine(Color(0xFFCBD5E1), bBotBase, bBotBar, strokeWidth = supportStroke)

        drawLine(Color.White, fTopBase, fTopBar, strokeWidth = postStroke)
        drawLine(Color.White, fBotBase, fBotBar, strokeWidth = postStroke)
        drawLine(Color.White, fTopBar, fBotBar, strokeWidth = postStroke)
    }

    private fun DrawScope.drawCornerFlag(pos: Offset, zoom: Float, flagColor: Color) {
        drawLine(Color.White, pos, Offset(pos.x, pos.y - 14f * zoom), strokeWidth = 1.6f * zoom)
        val flag = Path().apply {
            moveTo(pos.x, pos.y - 14f * zoom)
            lineTo(pos.x + 8f * zoom, pos.y - 11f * zoom)
            lineTo(pos.x, pos.y - 8f * zoom)
            close()
        }
        drawPath(flag, flagColor)
    }

    private fun DrawScope.drawProjectedCircle(
        cx: Float,
        cy: Float,
        radius: Float,
        project: (Float, Float, Float) -> Offset,
        color: Color,
        strokeWidth: Float
    ) {
        val steps = 32
        val path = Path()
        for (i in 0..steps) {
            val a = (i.toFloat() / steps) * 6.283185f
            val pt = project(cx + cos(a) * radius, cy + sin(a) * radius, 0f)
            if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
        }
        drawPath(path, color, style = Stroke(width = strokeWidth))
    }

    private fun DrawScope.drawQuad(
        p0: Offset,
        p1: Offset,
        p2: Offset,
        p3: Offset,
        color: Color
    ) {
        val path = Path().apply {
            moveTo(p0.x, p0.y)
            lineTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            lineTo(p3.x, p3.y)
            close()
        }
        drawPath(path, color)
    }

    private fun DrawScope.drawRadarMinimap(snapshot: MatchSnapshot, sw: Float, sh: Float) {
        val mapW = (sw * 0.21f).coerceIn(130f, 210f)
        val mapH = mapW * (PitchConstants.PITCH_H / PitchConstants.PITCH_W)
        val left = (sw - mapW) / 2f
        val top = sh - mapH - 12f

        // Background + crisp border
        drawRect(Color(0x990D1520), Offset(left, top), Size(mapW, mapH))
        drawRect(Color(0xCC5A7CA6), Offset(left, top), Size(mapW, mapH), style = Stroke(width = 2f))
        // Halfway line & center circle
        drawLine(
            Color(0x88FFFFFF),
            Offset(left + mapW * 0.5f, top),
            Offset(left + mapW * 0.5f, top + mapH),
            strokeWidth = 1.2f
        )
        drawCircle(
            Color(0x88FFFFFF),
            radius = mapH * 0.14f,
            center = Offset(left + mapW * 0.5f, top + mapH * 0.5f),
            style = Stroke(width = 1.2f)
        )

        val homeColor = Color(snapshot.homeClub.primaryColorHex)
        val awayColor = Color(snapshot.awayClub.primaryColorHex)
        snapshot.players.filter { !it.sentOff }.forEach { p ->
            val px = left + (p.x / PitchConstants.PITCH_W).coerceIn(0f, 1f) * mapW
            val py = top + (p.y / PitchConstants.PITCH_H).coerceIn(0f, 1f) * mapH
            val dotCol = if (p.isControlledByP1) PlayerCyan else if (p.isControlledByP2) PlayerOrange else if (p.isHome) homeColor else awayColor
            drawCircle(dotCol, radius = if (p.isControlledByP1 || p.isControlledByP2) 3.5f else 2.5f, center = Offset(px, py))
        }

        val bx = left + (snapshot.ball.x / PitchConstants.PITCH_W).coerceIn(0f, 1f) * mapW
        val by = top + (snapshot.ball.y / PitchConstants.PITCH_H).coerceIn(0f, 1f) * mapH
        drawCircle(Color.White, radius = 3.2f, center = Offset(bx, by))
    }
}
