package com.dota.analyzer.data

import kotlin.math.roundToInt

data class PlayerImpact(
    val player: Player,
    val score: Int,
    val roleName: String,
    val combat: Int,
    val economy: Int,
    val objectives: Int,
    val utility: Int
)

object MatchAnalytics {
    fun analyze(players: List<Player>): List<PlayerImpact> {
        if (players.isEmpty()) return emptyList()

        val combat = normalize(players) { it.heroDamage.toDouble() * 0.45 + it.kills * 120.0 + it.assists * 45.0 - it.deaths * 90.0 }
        val economy = normalize(players) { it.gpm * 0.40 + it.xpm * 0.25 + it.netWorth * 0.0008 + it.lastHits * 0.35 }
        val objectives = normalize(players) { it.towerDamage * 0.65 + it.denies * 35.0 + it.lastHits * 0.15 }
        val utility = normalize(players) { it.heroHealing * 0.50 + it.assists * 55.0 + it.denies * 20.0 }

        return players.mapIndexed { index, player ->
            val role = roleFor(player.laneRole)
            val weights = when (player.laneRole) {
                1, 2 -> doubleArrayOf(0.30, 0.38, 0.22, 0.10)
                3 -> doubleArrayOf(0.30, 0.28, 0.27, 0.15)
                4 -> doubleArrayOf(0.27, 0.30, 0.18, 0.25)
                5 -> doubleArrayOf(0.25, 0.12, 0.13, 0.50)
                else -> doubleArrayOf(0.28, 0.30, 0.22, 0.20)
            }
            val c = combat[index]
            val e = economy[index]
            val o = objectives[index]
            val u = utility[index]
            val score = (c * weights[0] + e * weights[1] + o * weights[2] + u * weights[3]).roundToInt().coerceIn(0, 100)
            PlayerImpact(player, score, role, c, e, o, u)
        }
    }

    private fun normalize(players: List<Player>, metric: (Player) -> Double): List<Int> {
        val values = players.map(metric)
        val min = values.minOrNull() ?: return emptyList()
        val max = values.maxOrNull() ?: return emptyList()
        if (max - min < 0.000001) return players.map { 50 }
        return values.map { (((it - min) / (max - min)) * 100.0).roundToInt().coerceIn(0, 100) }
    }

    fun roleFor(laneRole: Int): String = when (laneRole) {
        1 -> "Керри"
        2 -> "Мид"
        3 -> "Оффлейн"
        4 -> "Лес"
        5 -> "Саппорт"
        else -> "Роль не определена"
    }
}
