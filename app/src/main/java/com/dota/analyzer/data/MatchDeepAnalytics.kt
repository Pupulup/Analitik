package com.dota.analyzer.data

data class TeamfightReport(
    val index: Int, val start: Int, val end: Int,
    val radiantGoldDelta: Int, val direGoldDelta: Int,
    val radiantDeaths: Int, val direDeaths: Int,
    val radiantDamage: Long, val direDamage: Long,
    val topDamageSlot: Int?, val participants: Int, val buybacks: Int
) {
    val radiantSwing: Int get() = radiantGoldDelta - direGoldDelta
}

data class ObjectiveReport(
    val time: Int, val title: String, val detail: String,
    val playerSlot: Int?, val isRadiant: Boolean?
)

object MatchDeepAnalytics {
    fun teamfights(match: Match): List<TeamfightReport> =
        match.teamfights.mapIndexed { index, fight ->
            val radiant = fight.players.filter { it.playerSlot in 0..127 }
            val dire = fight.players.filter { it.playerSlot >= 128 }
            TeamfightReport(
                index + 1, fight.start.coerceAtLeast(0), fight.end.coerceAtLeast(fight.start),
                radiant.sumOf { it.goldDelta }, dire.sumOf { it.goldDelta },
                radiant.sumOf { it.deaths }, dire.sumOf { it.deaths },
                radiant.sumOf { it.damage.toLong() }, dire.sumOf { it.damage.toLong() },
                fight.players.maxByOrNull { it.damage }?.playerSlot,
                fight.players.count { it.playerSlot >= 0 },
                fight.players.count { it.buyback }
            )
        }.sortedBy { it.start }

    fun objectives(match: Match): List<ObjectiveReport> =
        match.objectives.mapNotNull { event ->
            val type = event.type.uppercase()
            val key = event.key.uppercase()
            val title = when {
                "ROSHAN" in type || "ROSHAN" in key -> "Рошан"
                "TOWER" in type || "TOWER" in key -> "Башня"
                "BARRACK" in type || "BARRACK" in key -> "Бараки"
                "FORT" in type || "FORT" in key -> "Трон"
                "OUTPOST" in type || "OUTPOST" in key -> "Аванпост"
                else -> return@mapNotNull null
            }
            val slot = event.playerSlot
            ObjectiveReport(
                event.time.coerceAtLeast(0), title,
                event.key.replace('_', ' ').trim().ifBlank { event.type },
                slot, slot?.let { it in 0..127 }
            )
        }.sortedBy { it.time }

    fun turningPointSummary(match: Match): String {
        val fights = teamfights(match)
        if (fights.isEmpty()) return "OpenDota не вернул подробные данные командных боёв для этого матча."
        val biggest = fights.maxByOrNull { kotlin.math.abs(it.radiantSwing) } ?: return ""
        val side = when {
            biggest.radiantSwing > 0 -> "Свет"
            biggest.radiantSwing < 0 -> "Тьма"
            else -> "ни одна команда"
        }
        val time = formatTime(biggest.start)
        val endTime = formatTime(biggest.end)
        val swing = kotlin.math.abs(biggest.radiantSwing)
        return "Самый значимый по изменению золота бой — $time–$endTime: $side получила относительный сдвиг $swing золота."
    }

    fun formatTime(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
}
