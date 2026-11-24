package com.dota.analyzer.data

import kotlin.math.roundToInt

data class PlayerImpact(
    val player: Player,
    val score: Int,
    val roleName: String,
    val combat: Int,
    val economy: Int,
    val experience: Int,
    val heroDamage: Int,
    val objectives: Int,
    val lastHits: Int,
    val survivability: Int,
    val utility: Int,
    val strengths: List<String>,
    val weaknesses: List<String>
)

data class TeamSummary(
    val isRadiant: Boolean,
    val playerCount: Int,
    val totalKills: Int,
    val totalDeaths: Int,
    val totalAssists: Int,
    val totalHeroDamage: Long,
    val totalTowerDamage: Long,
    val totalNetWorth: Long,
    val averageGpm: Int,
    val averageXpm: Int,
    val averageImpact: Int
)

data class TimelineEvent(
    val timeSeconds: Int,
    val heroId: Int,
    val isRadiant: Boolean,
    val description: String
)

object MatchAnalytics {
    private data class Weights(
        val combat: Double = 0.20,
        val economy: Double = 0.20,
        val experience: Double = 0.15,
        val heroDamage: Double = 0.20,
        val objectives: Double = 0.10,
        val lastHits: Double = 0.05,
        val survivability: Double = 0.10
    )

    fun analyze(players: List<Player>): List<PlayerImpact> {
        if (players.isEmpty()) return emptyList()

        val combat = normalize(players) {
            it.kills * 2.5 + it.assists * 1.2 + (it.teamfightParticipation ?: 0.0) * 20.0
        }
        val economy = normalize(players) { it.gpm.toDouble() }
        val experience = normalize(players) { it.xpm.toDouble() }
        val heroDamage = normalize(players) { it.heroDamage.toDouble() }
        val objectives = normalize(players) {
            it.towerDamage.toDouble() +
                (it.towerKills ?: 0) * 2000.0 +
                (it.roshanKills ?: 0) * 5000.0
        }
        val lastHits = normalize(players) { it.lastHits.toDouble() }
        val survivability = normalize(players) { -it.deaths.toDouble() }
        val utility = normalizeNullable(players) { player ->
            val values = mutableListOf<Double>()
            values += player.assists * 20.0
            values += player.heroHealing.toDouble()
            player.stuns?.let { values += it * 100.0 }
            player.observerWardsPlaced?.let { values += it * 250.0 }
            player.sentryWardsPlaced?.let { values += it * 150.0 }
            player.campsStacked?.let { values += it * 300.0 }
            player.teamfightParticipation?.let { values += it * 1000.0 }
            if (values.isEmpty()) null else values.sum()
        }

        return players.mapIndexed { index, player ->
            val components = listOf(
                combat[index] to Weights().combat,
                economy[index] to Weights().economy,
                experience[index] to Weights().experience,
                heroDamage[index] to Weights().heroDamage,
                objectives[index] to Weights().objectives,
                lastHits[index] to Weights().lastHits,
                survivability[index] to Weights().survivability
            )
            val availableWeight = components.sumOf { it.second }
            val score = if (availableWeight == 0.0) 50 else
                (components.sumOf { it.first * it.second } / availableWeight).roundToInt().coerceIn(0, 100)

            val categories = listOf(
                "Бой" to combat[index],
                "Экономика" to economy[index],
                "Опыт" to experience[index],
                "Урон героям" to heroDamage[index],
                "Объекты" to objectives[index],
                "Добивания" to lastHits[index],
                "Выживаемость" to survivability[index]
            )
            val strengths = categories.filter { it.second >= 75 }.map { it.first }
            val weaknesses = categories.filter { it.second <= 25 }.map { it.first }

            PlayerImpact(
                player = player,
                score = score,
                roleName = laneName(player.laneRole),
                combat = combat[index],
                economy = economy[index],
                experience = experience[index],
                heroDamage = heroDamage[index],
                objectives = objectives[index],
                lastHits = lastHits[index],
                survivability = survivability[index],
                utility = utility[index] ?: 50,
                strengths = strengths,
                weaknesses = weaknesses
            )
        }
    }

    fun teams(players: List<Player>, impacts: List<PlayerImpact>): List<TeamSummary> =
        listOf(true, false).map { radiant ->
            val team = players.filter { it.isRadiant == radiant }
            val teamImpacts = impacts.filter { it.player.isRadiant == radiant }
            TeamSummary(
                isRadiant = radiant,
                playerCount = team.size,
                totalKills = team.sumOf { it.kills },
                totalDeaths = team.sumOf { it.deaths },
                totalAssists = team.sumOf { it.assists },
                totalHeroDamage = team.sumOf { it.heroDamage.toLong() },
                totalTowerDamage = team.sumOf { it.towerDamage.toLong() },
                totalNetWorth = team.sumOf { it.netWorth.toLong() },
                averageGpm = team.averageOf { it.gpm },
                averageXpm = team.averageOf { it.xpm },
                averageImpact = teamImpacts.averageOf { it.score }
            )
        }

    fun timeline(players: List<Player>): List<TimelineEvent> =
        players.flatMap { player ->
            player.killsLog.orEmpty().map { event ->
                TimelineEvent(
                    timeSeconds = event.time,
                    heroId = player.heroId,
                    isRadiant = player.isRadiant,
                    description = if (event.key.isBlank()) "Убийство" else "Убийство: ${event.key}"
                )
            }
        }.filter { it.timeSeconds >= 0 }.sortedBy { it.timeSeconds }

    fun laneName(laneRole: Int): String = when (laneRole) {
        1 -> "Безопасная линия"
        2 -> "Центральная линия"
        3 -> "Сложная линия"
        4 -> "Лес / неосновная линия"
        else -> "Линия не определена"
    }

    private fun normalize(players: List<Player>, metric: (Player) -> Double): List<Int> =
        normalizeValues(players.map { metric(it) })

    private fun normalizeNullable(players: List<Player>, metric: (Player) -> Double?): List<Int?> {
        val values = players.map(metric)
        val available = values.filterNotNull()
        if (available.isEmpty()) return players.map { null }
        val normalized = normalizeValues(available)
        var index = 0
        return values.map { if (it == null) null else normalized[index++] }
    }

    private fun normalizeValues(values: List<Double>): List<Int> {
        if (values.isEmpty()) return emptyList()
        val min = values.minOrNull() ?: return emptyList()
        val max = values.maxOrNull() ?: return emptyList()
        if (max - min < 0.000001) return values.map { 50 }
        return values.map { (((it - min) / (max - min)) * 100.0).roundToInt().coerceIn(0, 100) }
    }

    private fun List<Player>.averageOf(selector: (Player) -> Int): Int =
        if (isEmpty()) 0 else (sumOf(selector).toDouble() / size).roundToInt()

    private fun List<PlayerImpact>.averageOf(selector: (PlayerImpact) -> Int): Int =
        if (isEmpty()) 0 else (sumOf(selector).toDouble() / size).roundToInt()
}
