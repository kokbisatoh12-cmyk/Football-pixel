package com.example.data

import kotlin.math.roundToInt
import kotlin.random.Random

object SeedDataGenerator {

    private data class ClubSeed(
        val name: String,
        val code: String,
        val primary: Long,
        val secondary: Long,
        val shorts: Long,
        val baseOvr: Int,
        val budget: Long,
        val formation: Formation
    )

    private val clubSeeds = listOf(
        ClubSeed("Nusantara FC", "NUS", 0xFFD92626, 0xFFFFFFFF, 0xFFFFFFFF, 81, 42_000_000L, Formation.F_433),
        ClubSeed("Garuda United", "GAR", 0xFF1A56DB, 0xFFF5B700, 0xFF11296B, 83, 55_000_000L, Formation.F_442),
        ClubSeed("Batavia City", "BAT", 0xFF29B6F6, 0xFFFFFFFF, 0xFF143C5E, 84, 64_000_000L, Formation.F_4231),
        ClubSeed("Bandung Rovers", "BDR", 0xFF1E40AF, 0xFF93C5FD, 0xFF1E3A8A, 79, 34_000_000L, Formation.F_442),
        ClubSeed("Surabaya Athletic", "SBY", 0xFF059669, 0xFFFDE047, 0xFF064E3B, 80, 38_000_000L, Formation.F_433),
        ClubSeed("Bali Star", "BAL", 0xFFDC2626, 0xFF111827, 0xFF111827, 78, 31_000_000L, Formation.F_352),
        ClubSeed("Malang Titans", "MLG", 0xFF1D4ED8, 0xFFEF4444, 0xFF111827, 77, 28_000_000L, Formation.F_442),
        ClubSeed("Makassar Royals", "MKS", 0xFF991B1B, 0xFFF59E0B, 0xFF7F1D1D, 79, 33_000_000L, Formation.F_4231),
        ClubSeed("Jogja Classic", "JOG", 0xFF0F766E, 0xFFFFFFFF, 0xFF134E4A, 76, 25_000_000L, Formation.F_442),
        ClubSeed("Borneo Eagles", "BRN", 0xFFEA580C, 0xFF111827, 0xFF111827, 78, 30_000_000L, Formation.F_433),
        ClubSeed("Medan Strikers", "MDN", 0xFF15803D, 0xFFFFFFFF, 0xFF14532D, 75, 22_000_000L, Formation.F_352),
        ClubSeed("Semarang Knights", "SMG", 0xFF6D28D9, 0xFFFBBF24, 0xFF4C1D95, 76, 24_000_000L, Formation.F_442)
    )

    private val firstNames = listOf(
        "Rizky", "Pratama", "Bagus", "Dimas", "Arhan", "Witan", "Marselino", "Rafael",
        "Hokky", "Nadeo", "Jordi", "Elkan", "Asnawi", "Yance", "Fajar", "Saddil",
        "Lucas", "Mateo", "Kenji", "Sora", "Diego", "Marco", "Tariq", "Irfan",
        "Adam", "Zaki", "Ilham", "Bima", "Galih", "Bayu", "Damar", "Reza"
    )

    private val lastNames = listOf(
        "Saputra", "Wijaya", "Santoso", "Kusuma", "Maulana", "Prasetyo", "Hidayat",
        "Nugroho", "Ferdinan", "Struick", "Arifin", "Hakim", "Putra", "Kurniawan",
        "Silva", "Costa", "Tanaka", "Vega", "Moreno", "Rossi", "Alvarez", "Susanto",
        "Ramadhan", "Hartono", "Surya", "Purnama", "Syahputra", "Gunawan"
    )

    fun buildInitialClubs(): List<ClubEntity> {
        return clubSeeds.mapIndexed { idx, seed ->
            ClubEntity(
                name = seed.name,
                shortCode = seed.code,
                primaryColorHex = seed.primary,
                secondaryColorHex = seed.secondary,
                shortsColorHex = seed.shorts,
                budget = seed.budget,
                wageBill = 0L,
                formationName = seed.formation.name,
                teamRating = seed.baseOvr,
                isPlayerClub = idx == 0
            )
        }
    }

    fun buildRosterForClub(club: ClubEntity, seedIndex: Int): List<PlayerEntity> {
        val rng = Random(seedIndex * 997 + 13)
        val slots = listOf(
            Position.GK to true,
            Position.DEF to true, Position.DEF to true, Position.DEF to true, Position.DEF to true,
            Position.MID to true, Position.MID to true, Position.MID to true, Position.MID to true,
            Position.FWD to true, Position.FWD to true,
            Position.GK to false, Position.DEF to false, Position.MID to false, Position.FWD to false
        )

        val baseOvr = club.teamRating
        return slots.mapIndexed { idx, (pos, starter) ->
            val ovrOffset = if (starter) rng.nextInt(-3, 5) else rng.nextInt(-8, -1)
            val ovr = (baseOvr + ovrOffset).coerceIn(64, 92)
            val pace = statFor(pos, ovr, Position.FWD, Position.MID, rng)
            val shoot = statFor(pos, ovr, Position.FWD, Position.MID, rng)
            val pass = statFor(pos, ovr, Position.MID, Position.DEF, rng)
            val dribble = statFor(pos, ovr, Position.FWD, Position.MID, rng)
            val defend = statFor(pos, ovr, Position.DEF, Position.GK, rng)
            val stamina = (ovr + rng.nextInt(-4, 8)).coerceIn(68, 97)
            val age = rng.nextInt(18, 33)
            val marketValue = ((ovr - 55).coerceAtLeast(5) * 320_000L * (if (age < 24) 1.3 else 1.0)).roundToInt().toLong()
            val wage = (marketValue / 140L).coerceAtLeast(12_000L)
            val name = "${firstNames[(seedIndex * 15 + idx * 3 + rng.nextInt(firstNames.size)) % firstNames.size]} " +
                "${lastNames[(seedIndex * 11 + idx * 5 + rng.nextInt(lastNames.size)) % lastNames.size]}"

            PlayerEntity(
                clubId = club.id,
                name = name,
                age = age,
                positionName = pos.name,
                ovr = ovr,
                pace = pace,
                shooting = shoot,
                passing = pass,
                dribbling = dribble,
                defending = defend,
                staminaMax = stamina,
                weeklyWage = wage,
                contractYears = rng.nextInt(1, 5),
                marketValue = marketValue,
                skinToneIndex = rng.nextInt(0, 4),
                hairStyleIndex = rng.nextInt(0, 5),
                isStarter = starter
            )
        }
    }

    private fun statFor(pos: Position, ovr: Int, primaryPos: Position, secondaryPos: Position, rng: Random): Int {
        val bias = when (pos) {
            primaryPos -> 4
            secondaryPos -> 1
            Position.GK -> -12
            else -> -4
        }
        return (ovr + bias + rng.nextInt(-4, 5)).coerceIn(45, 96)
    }

    fun generateRoundRobinFixtures(careerId: Int, clubIds: List<Int>): List<FixtureEntity> {
        if (clubIds.size < 2) return emptyList()
        val teams = clubIds.toMutableList()
        if (teams.size % 2 != 0) teams.add(-1)
        val n = teams.size
        val rounds = n - 1
        val matchesPerRound = n / 2
        val fixtures = mutableListOf<FixtureEntity>()

        val rotating = teams.drop(1).toMutableList()
        for (round in 0 until rounds) {
            val roundTeams = listOf(teams.first()) + rotating
            for (m in 0 until matchesPerRound) {
                val t1 = roundTeams[m]
                val t2 = roundTeams[n - 1 - m]
                if (t1 != -1 && t2 != -1) {
                    val home = if ((round + m) % 2 == 0) t1 else t2
                    val away = if (home == t1) t2 else t1
                    fixtures.add(
                        FixtureEntity(
                            careerId = careerId,
                            matchday = round + 1,
                            homeClubId = home,
                            awayClubId = away
                        )
                    )
                }
            }
            val last = rotating.removeAt(rotating.lastIndex)
            rotating.add(0, last)
        }
        return fixtures
    }
}
