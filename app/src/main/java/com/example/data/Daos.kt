package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClubDao {
    @Query("SELECT * FROM clubs ORDER BY points DESC, (goalsFor - goalsAgainst) DESC, goalsFor DESC, name ASC")
    fun observeStandings(): Flow<List<ClubEntity>>

    @Query("SELECT * FROM clubs ORDER BY id ASC")
    fun observeAllClubs(): Flow<List<ClubEntity>>

    @Query("SELECT * FROM clubs ORDER BY id ASC")
    suspend fun getAllClubsSync(): List<ClubEntity>

    @Query("SELECT * FROM clubs WHERE id = :id LIMIT 1")
    suspend fun getClubById(id: Int): ClubEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(clubs: List<ClubEntity>): List<Long>

    @Update
    suspend fun updateClub(club: ClubEntity)

    @Query("DELETE FROM clubs")
    suspend fun clearAll()
}

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players WHERE clubId = :clubId ORDER BY isStarter DESC, ovr DESC")
    fun observePlayersByClub(clubId: Int): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE clubId != :excludeClubId ORDER BY ovr DESC")
    fun observeTransferMarketPlayers(excludeClubId: Int): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players ORDER BY goals DESC, assists DESC, ovr DESC LIMIT 20")
    fun observeTopScorers(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players ORDER BY assists DESC, goals DESC, ovr DESC LIMIT 20")
    fun observeTopAssists(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE clubId = :clubId ORDER BY isStarter DESC, ovr DESC")
    suspend fun getPlayersByClubSync(clubId: Int): List<PlayerEntity>

    @Query("SELECT * FROM players WHERE id = :id LIMIT 1")
    suspend fun getPlayerById(id: Int): PlayerEntity?

    @Query("SELECT * FROM players")
    suspend fun getAllPlayersSync(): List<PlayerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(players: List<PlayerEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(player: PlayerEntity): Long

    @Update
    suspend fun updatePlayer(player: PlayerEntity)

    @Update
    suspend fun updatePlayers(players: List<PlayerEntity>)

    @Query("DELETE FROM players")
    suspend fun clearAll()
}

@Dao
interface CareerDao {
    @Query("SELECT * FROM career_saves WHERE isActive = 1 ORDER BY id DESC LIMIT 1")
    fun observeActiveCareer(): Flow<CareerSaveEntity?>

    @Query("SELECT * FROM career_saves WHERE isActive = 1 ORDER BY id DESC LIMIT 1")
    suspend fun getActiveCareerSync(): CareerSaveEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCareer(career: CareerSaveEntity): Long

    @Update
    suspend fun updateCareer(career: CareerSaveEntity)

    @Query("DELETE FROM career_saves")
    suspend fun clearCareers()

    @Query("SELECT * FROM fixtures WHERE careerId = :careerId ORDER BY matchday ASC, id ASC")
    fun observeFixtures(careerId: Int): Flow<List<FixtureEntity>>

    @Query("SELECT * FROM fixtures WHERE careerId = :careerId AND matchday = :matchday")
    suspend fun getFixturesByMatchday(careerId: Int, matchday: Int): List<FixtureEntity>

    @Query("SELECT * FROM fixtures WHERE id = :fixtureId LIMIT 1")
    suspend fun getFixtureById(fixtureId: Int): FixtureEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFixtures(fixtures: List<FixtureEntity>)

    @Update
    suspend fun updateFixture(fixture: FixtureEntity)

    @Query("DELETE FROM fixtures")
    suspend fun clearFixtures()

    @Query("SELECT * FROM trophies WHERE careerId = :careerId ORDER BY seasonNumber DESC, id DESC")
    fun observeTrophies(careerId: Int): Flow<List<TrophyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrophy(trophy: TrophyEntity)

    @Query("DELETE FROM trophies")
    suspend fun clearTrophies()
}
