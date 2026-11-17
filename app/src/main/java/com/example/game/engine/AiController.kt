package com.example.game.engine

import com.example.data.Formation
import com.example.data.MatchDifficulty
import com.example.data.PlayerFsmState
import kotlin.random.Random

data class AiDecision(
    val moveX: Float = 0f,
    val moveY: Float = 0f,
    val sprint: Boolean = false,
    val fsmState: PlayerFsmState = PlayerFsmState.IDLE,
    val wantPass: Boolean = false,
    val wantThrough: Boolean = false,
    val wantShoot: Boolean = false,
    val shootPower: Float = 0.7f,
    val wantTackle: Boolean = false,
    val aimX: Float = 1f,
    val aimY: Float = 0f,
    val diveAnim: Boolean = false
)

/**
 * Coordinates per-player [PlayerStateMachine] instances across the 11v11 pitch.
 */
class AiController(private val difficulty: MatchDifficulty) {
    private val rng = Random(System.currentTimeMillis())
    private val stateMachines = HashMap<Int, PlayerStateMachine>()

    fun getStateMachine(simId: Int, isGk: Boolean): PlayerStateMachine {
        return stateMachines.getOrPut(simId) {
            PlayerStateMachine(initialState = if (isGk) GoalkeepingState else PositioningState)
        }
    }

    fun computeFormationTarget(
        player: SimPlayer,
        formation: Formation,
        ball: SimBall,
        teamHasBall: Boolean
    ): Pair<Float, Float> {
        return FsmTacticalMath.computeFormationTarget(player, formation, ball, teamHasBall)
    }

    fun decide(
        player: SimPlayer,
        allPlayers: List<SimPlayer>,
        ball: SimBall,
        formation: Formation,
        isPresser: Boolean,
        hasBall: Boolean,
        teamHasBall: Boolean,
        dt: Float = 1f / 60f
    ): AiDecision {
        val fsm = getStateMachine(player.simId, player.isGk)
        val ctx = PlayerFsmContext(
            player = player,
            allPlayers = allPlayers,
            ball = ball,
            formation = formation,
            difficulty = difficulty,
            isPresser = isPresser,
            hasBall = hasBall,
            teamHasBall = teamHasBall,
            rng = rng
        )
        return fsm.update(ctx, dt)
    }
}
