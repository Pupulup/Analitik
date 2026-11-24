package com.dota.analyzer.data

data class TeamfightReport(
    val index: Int, val start: Int, val end: Int,
    val radiantGoldDelta: Int?, val direGoldDelta: Int?,
    val radiantDeaths: Int?, val direDeaths: Int?,
    val radiantDamage: Long?, val direDamage: Long?,
    val topDamageSlot: Int?, val participants: Int, val buybacks: Int?
) {
    val radiantSwing: Int?
        get() = if (radiantGoldDelta != null && direGoldDelta != null) radiantGoldDelta - direGoldDelta else null
}

data class ObjectiveReport(
    val time: Int, val title: String, val detail: String,
    val playerSlot: Int?, val isRadiant: Boolean?
)

data class WardPlacement(
    val time: Int, val playerSlot: Int?, val x: Int, val y: Int, val isObserver: Boolean
)

object MatchDeepAnalytics {
    private fun List<Int?>.sumIfPresent(): Int? =
        if (any { it != null }) filterNotNull().sum() else null

    private fun List<Int?>.sumLongIfPresent(): Long? =
        if (any { it != null }) filterNotNull().sumOf { it.toLong() } else null

    fun teamfights(match: Match): List<TeamfightReport> =
        match.teamfights.mapIndexed { index, fight ->
            val radiant = fight.players.filter { it.playerSlot in 0..127 }
            val dire = fight.players.filter { it.playerSlot >= 128 }
            val damagePlayers = fight.players.filter { it.damage != null }
            val buybackValues = fight.players.mapNotNull { it.buyback }
            TeamfightReport(
                index = index + 1,
                start = fight.start.coerceAtLeast(0),
                end = fight.end.coerceAtLeast(fight.start),
                radiantGoldDelta = radiant.map { it.goldDelta }.sumIfPresent(),
                direGoldDelta = dire.map { it.goldDelta }.sumIfPresent(),
                radiantDeaths = radiant.map { it.deaths }.sumIfPresent(),
                direDeaths = dire.map { it.deaths }.sumIfPresent(),
                radiantDamage = radiant.map { it.damage }.sumLongIfPresent(),
                direDamage = dire.map { it.damage }.sumLongIfPresent(),
                topDamageSlot = damagePlayers.maxByOrNull { it.damage ?: Int.MIN_VALUE }?.playerSlot,
                participants = fight.players.count { it.playerSlot >= 0 },
                buybacks = if (buybackValues.isEmpty()) null else buybackValues.count { it }
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
            val slot = event.playerSlot ?: event.slot
            ObjectiveReport(
                event.time.coerceAtLeast(0), title,
                event.key.replace('_', ' ').trim().ifBlank { event.type },
                slot, slot?.let { it in 0..127 }
            )
        }.sortedBy { it.time }

    fun wardPlacements(match: Match): List<WardPlacement> =
        buildList {
            match.observerWardEvents.forEach { event ->
                if (event.x != null && event.y != null) add(WardPlacement(event.time, event.playerSlot, event.x, event.y, true))
            }
            match.sentryWardEvents.forEach { event ->
                if (event.x != null && event.y != null) add(WardPlacement(event.time, event.playerSlot, event.x, event.y, false))
            }
        }.sortedBy { it.time }

    fun turningPointSummary(match: Match): String {
        val fights = teamfights(match).filter { it.radiantSwing != null }
        if (fights.isEmpty()) return "Нет данных об изменении золота по отдельным боям. OpenDota возвращает эти показатели не для каждого матча."
        val biggest = fights.maxByOrNull { kotlin.math.abs(it.radiantSwing ?: 0) } ?: return ""
        val swing = biggest.radiantSwing ?: return ""
        val side = when {
            swing > 0 -> "Свет"
            swing < 0 -> "Тьма"
            else -> "ни одна команда"
        }
        return "Наибольший сдвиг по золоту пришёлся на бой " +
            formatTime(biggest.start) + "–" + formatTime(biggest.end) +
            ". Относительный перевес по сумме изменений — " + side + " (" + kotlin.math.abs(swing) + ")."
    }

    fun formatTime(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
}
