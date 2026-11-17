package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clubs")
data class ClubEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val shortCode: String,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val shortsColorHex: Long,
    val budget: Long,
    val wageBill: Long,
    val formationName: String,
    val teamRating: Int,
    val played: Int = 0,
    val won: Int = 0,
    val drawn: Int = 0,
    val lost: Int = 0,
    val goalsFor: Int = 0,
    val goalsAgainst: Int = 0,
    val points: Int = 0,
    val isPlayerClub: Boolean = false
) {
    val goalDifference: Int get() = goalsFor - goalsAgainst
    val formation: Formation get() = Formation.entries.firstOrNull { it.name == formationName } ?: Formation.F_442
}

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clubId: Int,
    val name: String,
    val age: Int,
    val positionName: String,
    val ovr: Int,
    val pace: Int,
    val shooting: Int,
    val passing: Int,
    val dribbling: Int,
    val defending: Int,
    val staminaMax: Int,
    val weeklyWage: Long,
    val contractYears: Int,
    val marketValue: Long,
    val goals: Int = 0,
    val assists: Int = 0,
    val appearances: Int = 0,
    val yellowCards: Int = 0,
    val skinToneIndex: Int = 0,
    val hairStyleIndex: Int = 0,
    val isUserCareerPro: Boolean = false,
    val xp: Int = 0,
    val isStarter: Boolean = true
) {
    val position: Position get() = Position.entries.firstOrNull { it.name == positionName } ?: Position.MID
}

@Entity(tableName = "career_saves")
data class CareerSaveEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val careerRoleName: String,
    val clubId: Int,
    val userPlayerId: Int? = null,
    val seasonNumber: Int = 1,
    val currentMatchday: Int = 1,
    val totalMatchdays: Int = 11,
    val clubMorale: Int = 75,
    val trainingPoints: Int = 3,
    val isActive: Boolean = true
) {
    val role: CareerRole get() = CareerRole.entries.firstOrNull { it.name == careerRoleName } ?: CareerRole.MANAGER
}

@Entity(tableName = "fixtures")
data class FixtureEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val careerId: Int,
    val matchday: Int,
    val homeClubId: Int,
    val awayClubId: Int,
    val homeGoals: Int = 0,
    val awayGoals: Int = 0,
    val isPlayed: Boolean = false,
    val scorersSummary: String = ""
)

@Entity(tableName = "trophies")
data class TrophyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val careerId: Int,
    val seasonNumber: Int,
    val title: String,
    val clubName: String,
    val subtitle: String
)
