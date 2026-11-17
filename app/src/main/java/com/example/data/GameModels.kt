package com.example.data

enum class Position(val label: String) {
    GK("GK"),
    DEF("DEF"),
    MID("MID"),
    FWD("FWD")
}

enum class Formation(val code: String, val defCount: Int, val midCount: Int, val fwdCount: Int) {
    F_442("4-4-2", 4, 4, 2),
    F_433("4-3-3", 4, 3, 3),
    F_352("3-5-2", 3, 5, 2),
    F_4231("4-2-3-1", 4, 5, 1)
}

enum class WeatherType(val label: String) {
    CLEAR("Clear"),
    RAIN("Rain"),
    STORM("Storm")
}

enum class TimeOfDay(val label: String) {
    DAY("Day"),
    NIGHT("Night")
}

enum class MatchDifficulty(val label: String, val aiReactionScale: Float) {
    ROOKIE("Rookie", 0.72f),
    PRO("Pro", 1.0f),
    LEGEND("Legend", 1.25f)
}

enum class CareerRole(val label: String) {
    MANAGER("Club Manager"),
    PLAYER("Star Player")
}

enum class MatchSides(val label: String) {
    P1_VS_AI("1P vs AI"),
    P1_VS_P2("1P vs 2P Local")
}

enum class TrainingDrillType(val title: String, val subtitle: String, val focusStat: String) {
    PASSING("Passing Targets", "Hit glowing teammate zones cleanly", "Passing"),
    SHOOTING("Shooting vs GK", "Beat the keeper from varied angles", "Shooting"),
    DRIBBLING("Slalom Dribbling", "Weave through cones at pace", "Dribbling")
}

enum class PlayerAnim {
    IDLE,
    WALK,
    RUN,
    SLIDE,
    KICK,
    HEADER,
    BICYCLE,
    RECEIVE,
    GK_DIVE,
    CELEBRATE
}

enum class PlayerFsmState {
    IDLE,
    SUPPORT_ATTACK,
    MAKE_RUN,
    DRIBBLE,
    PASS_DECISION,
    SHOOT_DECISION,
    PRESS_BALL,
    MARK_OPPONENT,
    RECOVER_SHAPE,
    SLIDE_TACKLE,
    GK_TRACK_ANGLE,
    GK_DIVE_SAVE,
    CELEBRATE
}

enum class SetPieceType {
    NONE,
    KICKOFF,
    FREE_KICK,
    CORNER,
    GOAL_KICK,
    PENALTY,
    THROW_IN,
    OFFSIDE
}

data class FormationAnchor(
    val slotIndex: Int,
    val role: Position,
    val normX: Float, // 0f = own goal line, 1f = opp goal line
    val normY: Float  // 0f = top touchline, 1f = bottom touchline
)

object FormationTemplates {
    fun getAnchors(formation: Formation): List<FormationAnchor> {
        val gk = FormationAnchor(0, Position.GK, 0.04f, 0.50f)
        return when (formation) {
            Formation.F_442 -> listOf(
                gk,
                FormationAnchor(1, Position.DEF, 0.20f, 0.14f),
                FormationAnchor(2, Position.DEF, 0.18f, 0.38f),
                FormationAnchor(3, Position.DEF, 0.18f, 0.62f),
                FormationAnchor(4, Position.DEF, 0.20f, 0.86f),
                FormationAnchor(5, Position.MID, 0.44f, 0.16f),
                FormationAnchor(6, Position.MID, 0.42f, 0.39f),
                FormationAnchor(7, Position.MID, 0.42f, 0.61f),
                FormationAnchor(8, Position.MID, 0.44f, 0.84f),
                FormationAnchor(9, Position.FWD, 0.68f, 0.36f),
                FormationAnchor(10, Position.FWD, 0.68f, 0.64f)
            )
            Formation.F_433 -> listOf(
                gk,
                FormationAnchor(1, Position.DEF, 0.20f, 0.14f),
                FormationAnchor(2, Position.DEF, 0.18f, 0.38f),
                FormationAnchor(3, Position.DEF, 0.18f, 0.62f),
                FormationAnchor(4, Position.DEF, 0.20f, 0.86f),
                FormationAnchor(5, Position.MID, 0.43f, 0.25f),
                FormationAnchor(6, Position.MID, 0.39f, 0.50f),
                FormationAnchor(7, Position.MID, 0.43f, 0.75f),
                FormationAnchor(8, Position.FWD, 0.68f, 0.18f),
                FormationAnchor(9, Position.FWD, 0.72f, 0.50f),
                FormationAnchor(10, Position.FWD, 0.68f, 0.82f)
            )
            Formation.F_352 -> listOf(
                gk,
                FormationAnchor(1, Position.DEF, 0.18f, 0.24f),
                FormationAnchor(2, Position.DEF, 0.17f, 0.50f),
                FormationAnchor(3, Position.DEF, 0.18f, 0.76f),
                FormationAnchor(4, Position.MID, 0.43f, 0.12f),
                FormationAnchor(5, Position.MID, 0.40f, 0.34f),
                FormationAnchor(6, Position.MID, 0.45f, 0.50f),
                FormationAnchor(7, Position.MID, 0.40f, 0.66f),
                FormationAnchor(8, Position.MID, 0.43f, 0.88f),
                FormationAnchor(9, Position.FWD, 0.69f, 0.36f),
                FormationAnchor(10, Position.FWD, 0.69f, 0.64f)
            )
            Formation.F_4231 -> listOf(
                gk,
                FormationAnchor(1, Position.DEF, 0.20f, 0.14f),
                FormationAnchor(2, Position.DEF, 0.18f, 0.38f),
                FormationAnchor(3, Position.DEF, 0.18f, 0.62f),
                FormationAnchor(4, Position.DEF, 0.20f, 0.86f),
                FormationAnchor(5, Position.MID, 0.36f, 0.36f),
                FormationAnchor(6, Position.MID, 0.36f, 0.64f),
                FormationAnchor(7, Position.MID, 0.56f, 0.18f),
                FormationAnchor(8, Position.MID, 0.57f, 0.50f),
                FormationAnchor(9, Position.MID, 0.56f, 0.82f),
                FormationAnchor(10, Position.FWD, 0.73f, 0.50f)
            )
        }
    }
}

data class MatchLaunchConfig(
    val homeClubId: Int,
    val awayClubId: Int,
    val homeFormation: Formation = Formation.F_442,
    val awayFormation: Formation = Formation.F_433,
    val sides: MatchSides = MatchSides.P1_VS_AI,
    val weather: WeatherType = WeatherType.CLEAR,
    val timeOfDay: TimeOfDay = TimeOfDay.NIGHT,
    val durationMinutes: Int = 3,
    val gameSpeed: Float = 1.0f,
    val difficulty: MatchDifficulty = MatchDifficulty.PRO,
    val careerFixtureId: Int? = null,
    val tournamentMatchId: Int? = null,
    val lockedUserPlayerId: Int? = null
)

data class AppSettings(
    val sfxEnabled: Boolean = true,
    val sfxVolume: Float = 0.75f,
    val defaultDurationMin: Int = 3,
    val defaultSpeed: Float = 1.0f,
    val difficulty: MatchDifficulty = MatchDifficulty.PRO,
    val dualPadEnabled: Boolean = false
)
