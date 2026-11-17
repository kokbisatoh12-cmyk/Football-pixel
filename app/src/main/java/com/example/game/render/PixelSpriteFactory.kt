package com.example.game.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.data.PlayerAnim
import java.util.concurrent.ConcurrentHashMap

/**
 * Generates crisp 16x22 pixel-art player sprite bitmaps inspired by classic retro soccer sprite sheets.
 * Sprites are cached by palette + animation + frame so 60 FPS rendering incurs zero per-frame allocations.
 */
class PixelSpriteFactory {

    private data class SpriteKey(
        val primary: Int,
        val secondary: Int,
        val shorts: Int,
        val skin: Int,
        val hair: Int,
        val anim: PlayerAnim,
        val frame: Int,
        val facingRight: Boolean
    )

    private val spriteCache = ConcurrentHashMap<SpriteKey, ImageBitmap>()
    private var ballFrames: List<ImageBitmap>? = null
    private var crowdSprites: List<ImageBitmap>? = null

    private val skinPalette = intArrayOf(
        0xFFF2C29B.toInt(),
        0xFFD99B6C.toInt(),
        0xFF9F633E.toInt(),
        0xFF6B3E26.toInt()
    )

    private val hairPalette = intArrayOf(
        0xFF1F1A17.toInt(),
        0xFF4A2E1B.toInt(),
        0xFFD49A37.toInt(),
        0xFF8C3B2B.toInt(),
        0xFF111111.toInt()
    )

    fun getPlayerSprite(
        primaryHex: Long,
        secondaryHex: Long,
        shortsHex: Long,
        skinIdx: Int,
        hairIdx: Int,
        isGk: Boolean,
        anim: PlayerAnim,
        animTimer: Float,
        facingRight: Boolean
    ): ImageBitmap {
        val jersey = if (isGk) 0xFF10B981.toInt() else primaryHex.toInt()
        val trim = if (isGk) 0xFFFEF08A.toInt() else secondaryHex.toInt()
        val shorts = if (isGk) 0xFF111827.toInt() else shortsHex.toInt()
        val skin = skinPalette[skinIdx.coerceIn(0, skinPalette.lastIndex)]
        val hair = hairPalette[hairIdx.coerceIn(0, hairPalette.lastIndex)]

        val frameCount = when (anim) {
            PlayerAnim.IDLE -> 2
            PlayerAnim.WALK, PlayerAnim.RUN -> 4
            PlayerAnim.CELEBRATE -> 2
            PlayerAnim.KICK, PlayerAnim.SLIDE, PlayerAnim.GK_DIVE -> 2
            else -> 2
        }
        val fps = when (anim) {
            PlayerAnim.RUN -> 10f
            PlayerAnim.WALK -> 6f
            PlayerAnim.CELEBRATE -> 7f
            PlayerAnim.KICK, PlayerAnim.SLIDE -> 8f
            else -> 3f
        }
        val frame = ((animTimer * fps).toInt().coerceAtLeast(0)) % frameCount

        val key = SpriteKey(jersey, trim, shorts, skin, hair, anim, frame, facingRight)
        return spriteCache.getOrPut(key) {
            buildPlayerBitmap(jersey, trim, shorts, skin, hair, anim, frame, facingRight)
        }
    }

    fun getBallSprite(rotationAngle: Float): ImageBitmap {
        val list = ballFrames ?: buildBallFrames().also { ballFrames = it }
        val idx = ((rotationAngle / 1.57f).toInt().mod(list.size))
        return list[idx]
    }

    fun getCrowdSprite(variant: Int): ImageBitmap {
        val list = crowdSprites ?: buildCrowdSprites().also { crowdSprites = it }
        return list[variant.mod(list.size)]
    }

    private fun buildPlayerBitmap(
        jersey: Int,
        trim: Int,
        shorts: Int,
        skin: Int,
        hair: Int,
        anim: PlayerAnim,
        frame: Int,
        facingRight: Boolean
    ): ImageBitmap {
        val w = 16
        val h = 22
        val grid = Array(h) { IntArray(w) { 0 } }
        val outline = 0xFF111318.toInt()
        val eye = 0xFF111318.toInt()
        val boot = 0xFF1D2129.toInt()
        val sock = trim

        fun px(x: Int, y: Int, col: Int) {
            val rx = if (facingRight) x else (w - 1 - x)
            if (rx in 0 until w && y in 0 until h) grid[y][rx] = col
        }

        fun rect(x0: Int, y0: Int, x1: Int, y1: Int, col: Int) {
            for (y in y0..y1) for (x in x0..x1) px(x, y, col)
        }

        val bobY = when {
            anim == PlayerAnim.RUN && (frame == 1 || frame == 3) -> -1
            anim == PlayerAnim.IDLE && frame == 1 -> 0
            anim == PlayerAnim.CELEBRATE && frame == 1 -> -2
            anim == PlayerAnim.HEADER -> -2
            else -> 0
        }

        if (anim == PlayerAnim.SLIDE) {
            // Sliding horizontal pose
            rect(2, 14, 5, 18, hair)
            rect(3, 15, 6, 19, skin)
            px(5, 16, eye)
            rect(6, 15, 10, 19, jersey)
            px(8, 16, trim)
            rect(10, 16, 12, 19, shorts)
            rect(12, 17, 14, 18, sock)
            rect(14, 16, 15, 18, boot)
            return addOutlineAndConvert(grid, w, h, outline)
        }

        if (anim == PlayerAnim.GK_DIVE) {
            // Stretched diving goalkeeper
            rect(1, 10, 4, 13, skin) // gloved hand
            px(1, 10, 0xFFFFFFFF.toInt())
            rect(4, 11, 8, 15, jersey)
            rect(7, 8, 10, 11, skin)
            rect(7, 7, 10, 8, hair)
            rect(9, 12, 12, 15, shorts)
            rect(12, 13, 14, 15, sock)
            rect(14, 13, 15, 15, boot)
            return addOutlineAndConvert(grid, w, h, outline)
        }

        // Head (rows 2..7)
        val hy = 2 + bobY
        rect(6, hy, 10, hy + 1, hair)
        rect(5, hy + 1, 6, hy + 3, hair)
        rect(6, hy + 2, 10, hy + 5, skin)
        px(9, hy + 3, eye)
        px(7, hy + 3, eye)

        // Torso / Jersey (rows 8..13)
        val ty = hy + 6
        rect(5, ty, 10, ty + 4, jersey)
        // Collar / stripe trim
        px(7, ty, trim)
        px(8, ty, trim)
        rect(6, ty + 2, 9, ty + 2, trim)

        // Arms
        when (anim) {
            PlayerAnim.CELEBRATE -> {
                rect(4, ty - 3, 5, ty + 1, jersey)
                px(4, ty - 4, skin)
                rect(10, ty - 3, 11, ty + 1, jersey)
                px(11, ty - 4, skin)
            }
            PlayerAnim.RUN -> {
                val swing = if (frame % 2 == 0) 1 else -1
                rect(4, ty + 1 + swing, 4, ty + 3 + swing, skin)
                rect(11, ty + 1 - swing, 11, ty + 3 - swing, skin)
            }
            else -> {
                rect(4, ty + 1, 4, ty + 3, skin)
                rect(11, ty + 1, 11, ty + 3, skin)
            }
        }

        // Shorts (rows 13..15)
        val sy = ty + 5
        rect(5, sy, 10, sy + 2, shorts)

        // Legs + Socks + Boots
        val ly = sy + 3
        when (anim) {
            PlayerAnim.KICK -> {
                // Planted left leg, extended right kick leg
                rect(5, ly, 6, ly + 2, sock)
                rect(5, ly + 3, 7, ly + 3, boot)
                rect(9, ly - 1, 12, ly, sock)
                rect(12, ly - 1, 13, ly, boot)
            }
            PlayerAnim.RUN, PlayerAnim.WALK -> {
                when (frame % 4) {
                    0 -> {
                        rect(5, ly, 6, ly + 1, sock); rect(5, ly + 2, 6, ly + 2, boot)
                        rect(9, ly, 10, ly + 2, sock); rect(9, ly + 3, 11, ly + 3, boot)
                    }
                    1 -> {
                        rect(6, ly, 7, ly + 2, sock); rect(6, ly + 3, 8, ly + 3, boot)
                        rect(8, ly, 9, ly + 1, sock); rect(9, ly + 1, 10, ly + 2, boot)
                    }
                    2 -> {
                        rect(5, ly, 6, ly + 2, sock); rect(5, ly + 3, 7, ly + 3, boot)
                        rect(9, ly, 10, ly + 1, sock); rect(10, ly + 1, 11, ly + 2, boot)
                    }
                    else -> {
                        rect(4, ly, 5, ly + 1, sock); rect(4, ly + 2, 6, ly + 2, boot)
                        rect(9, ly, 10, ly + 2, sock); rect(10, ly + 3, 11, ly + 3, boot)
                    }
                }
            }
            else -> {
                rect(5, ly, 6, ly + 2, sock)
                rect(5, ly + 3, 7, ly + 3, boot)
                rect(9, ly, 10, ly + 2, sock)
                rect(9, ly + 3, 11, ly + 3, boot)
            }
        }

        return addOutlineAndConvert(grid, w, h, outline)
    }

    private fun addOutlineAndConvert(
        grid: Array<IntArray>,
        w: Int,
        h: Int,
        outlineColor: Int
    ): ImageBitmap {
        val out = Array(h) { y -> grid[y].copyOf() }
        val dirs = arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1))
        for (y in 0 until h) {
            for (x in 0 until w) {
                if (grid[y][x] == 0) {
                    var touches = false
                    for (d in dirs) {
                        val nx = x + d[0]
                        val ny = y + d[1]
                        if (nx in 0 until w && ny in 0 until h && grid[ny][nx] != 0) {
                            touches = true
                            break
                        }
                    }
                    if (touches) out[y][x] = outlineColor
                }
            }
        }
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        for (y in 0 until h) {
            for (x in 0 until w) {
                bmp.setPixel(x, y, out[y][x])
            }
        }
        return bmp.asImageBitmap()
    }

    private fun buildBallFrames(): List<ImageBitmap> {
        val size = 10
        val white = 0xFFFFFFFF.toInt()
        val shade = 0xFFC8D2DC.toInt()
        val black = 0xFF151922.toInt()
        return (0 until 4).map { phase ->
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            val p = Paint().apply { isAntiAlias = false }
            for (y in 0 until size) {
                for (x in 0 until size) {
                    val dx = x - 4.5f
                    val dy = y - 4.5f
                    val d2 = dx * dx + dy * dy
                    if (d2 <= 21f) {
                        val col = when {
                            d2 > 15.5f -> black
                            ((x + phase) % 4 <= 1 && (y + phase) % 4 <= 1) -> black
                            dy > 1.5f -> shade
                            else -> white
                        }
                        p.color = col
                        c.drawPoint(x.toFloat(), y.toFloat(), p)
                    }
                }
            }
            bmp.asImageBitmap()
        }
    }

    private fun buildCrowdSprites(): List<ImageBitmap> {
        val shirtColors = intArrayOf(
            0xFFE11D48.toInt(),
            0xFF2563EB.toInt(),
            0xFFF59E0B.toInt(),
            0xFF10B981.toInt(),
            0xFFF8FAFC.toInt(),
            0xFF8B5CF6.toInt()
        )
        return shirtColors.indices.map { i ->
            val w = 8
            val h = 10
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val skin = skinPalette[i % skinPalette.size]
            val hair = hairPalette[i % hairPalette.size]
            val shirt = shirtColors[i]
            val dark = 0xFF111318.toInt()
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val col = when {
                        y == 0 && x in 2..5 -> dark
                        y == 1 && x in 2..5 -> hair
                        y in 2..4 && x in 2..5 -> skin
                        y in 5..8 && x in 1..6 -> shirt
                        y == 9 && x in 1..6 -> dark
                        else -> 0
                    }
                    bmp.setPixel(x, y, col)
                }
            }
            bmp.asImageBitmap()
        }
    }
}
