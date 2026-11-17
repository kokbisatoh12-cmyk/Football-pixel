package com.example.game.engine

import com.example.data.ClubEntity
import com.example.data.PlayerAnim
import com.example.data.PlayerEntity
import com.example.data.PlayerFsmState
import com.example.data.Position
import com.example.data.SetPieceType
import com.example.data.TimeOfDay
import com.example.data.TrainingDrillType
import com.example.data.WeatherType

object PitchConstants {
    const val PITCH_W = 1200f
    const val PITCH_H = 740f
    const val CENTER_X = PITCH_W / 2f
    const val CENTER_Y = PITCH_H / 2f
    const val GOAL_HALF_H = 68f
    const val GOAL_TOP_Y = CENTER_Y - GOAL_HALF_H
    const val GOAL_BOTTOM_Y = CENTER_Y + GOAL_HALF_H
    const val GOAL_DEPTH = 34f
    const val GOAL_HEIGHT = 44f
    const val PENALTY_BOX_W = 172f
    const val PENALTY_BOX_HALF_H = 152f
    const val SIX_YARD_W = 60f
    const val SIX_YARD_HALF_H = 82f
    const val PENALTY_SPOT_OFFSET = 122f
}

data class SimPlayer(
    val simId: Int,
    val entityId: Int,
    val name: String,
    val shirtNumber: Int,
    val isHome: Boolean,
    val isGk: Boolean,
    val slotIndex: Int,
    val role: Position,
    val ovr: Int,
    val pace: Int,
    val shooting: Int,
    val passing: Int,
    val dribbling: Int,
    val defending: Int,
    val staminaMax: Int,
    val skinToneIndex: Int,
    val hairStyleIndex: Int,
    val primaryColor: Long,
    val secondaryColor: Long,
    val shortsColor: Long,
    val x: Float,
    val y: Float,
    val z: Float = 0f,
    val vx: Float = 0f,
    val vy: Float = 0f,
    val vz: Float = 0f,
    val facingRight: Boolean = true,
    val anim: PlayerAnim = PlayerAnim.IDLE,
    val animTimer: Float = 0f,
    val fsmState: PlayerFsmState = PlayerFsmState.IDLE,
    val stamina: Float = 100f,
    val actionCooldown: Float = 0f,
    val slideTimer: Float = 0f,
    val kickTimer: Float = 0f,
    val celebrateTimer: Float = 0f,
    val yellowCards: Int = 0,
    val sentOff: Boolean = false,
    val isControlledByP1: Boolean = false,
    val isControlledByP2: Boolean = false,
    val homeAnchorX: Float = x,
    val homeAnchorY: Float = y
)

data class SimBall(
    val x: Float = PitchConstants.CENTER_X,
    val y: Float = PitchConstants.CENTER_Y,
    val z: Float = 0f,
    val vx: Float = 0f,
    val vy: Float = 0f,
    val vz: Float = 0f,
    val ownerSimId: Int? = null,
    val lastTouchHome: Boolean = true,
    val lastTouchPlayerId: Int? = null,
    val prevTouchPlayerId: Int? = null,
    val rotationAngle: Float = 0f,
    val passPickupLockTimer: Float = 0f,
    val kickedForwardInOppHalf: Boolean = false
)

enum class MatchPhase {
    KICKOFF,
    PLAYING,
    SET_PIECE_WAIT,
    GOAL_CELEBRATION,
    HALFTIME,
    FULLTIME
}

data class ControlInput(
    val moveX: Float = 0f,
    val moveY: Float = 0f,
    val sprint: Boolean = false,
    val passJustPressed: Boolean = false,
    val throughJustPressed: Boolean = false,
    val shootHolding: Boolean = false,
    val shootJustReleased: Boolean = false,
    val shootQuickTap: Boolean = false,
    val switchJustPressed: Boolean = false,
    val tackleJustPressed: Boolean = false
)

data class TrainingTarget(
    val id: Int,
    val x: Float,
    val y: Float,
    val radius: Float,
    val isCone: Boolean = false,
    val hit: Boolean = false,
    val label: String = ""
)

data class MatchEventBanner(
    val title: String,
    val subtitle: String,
    val accentHex: Long = 0xFFF5B700
)

data class MatchSnapshot(
    val homeClub: ClubEntity,
    val awayClub: ClubEntity,
    val players: List<SimPlayer>,
    val ball: SimBall,
    val homeScore: Int = 0,
    val awayScore: Int = 0,
    val matchSecondsElapsed: Float = 0f,
    val totalMatchSeconds: Float = 180f,
    val half: Int = 1,
    val phase: MatchPhase = MatchPhase.KICKOFF,
    val setPieceType: SetPieceType = SetPieceType.KICKOFF,
    val setPieceTeamHome: Boolean = true,
    val banner: MatchEventBanner? = null,
    val weather: WeatherType = WeatherType.CLEAR,
    val timeOfDay: TimeOfDay = TimeOfDay.NIGHT,
    val p1ShootCharge: Float = 0f,
    val p2ShootCharge: Float = 0f,
    val homePossessionTicks: Int = 1,
    val awayPossessionTicks: Int = 1,
    val homeShots: Int = 0,
    val awayShots: Int = 0,
    val homeFouls: Int = 0,
    val awayFouls: Int = 0,
    val scorerLogs: List<String> = emptyList(),
    val scorerEntityIds: List<Int> = emptyList(),
    val assistEntityIds: List<Int> = emptyList(),
    val homeBench: List<PlayerEntity> = emptyList(),
    val awayBench: List<PlayerEntity> = emptyList(),
    val isTrainingMode: Boolean = false,
    val trainingDrill: TrainingDrillType? = null,
    val trainingScore: Int = 0,
    val trainingTimeLeft: Float = 60f,
    val trainingTargets: List<TrainingTarget> = emptyList(),
    val crowdExcitement: Float = 0f,
    val cameraX: Float = PitchConstants.CENTER_X,
    val cameraY: Float = PitchConstants.CENTER_Y,
    val tickCount: Long = 0L
) {
    val displayMinute: Int
        get() = ((matchSecondsElapsed / totalMatchSeconds) * 90f).toInt().coerceIn(0, 90)
}
