package com.dota.analyzer.data

import com.google.gson.annotations.SerializedName

data class Match(
    @SerializedName("match_id") val matchId: Long,
    @SerializedName("duration") val duration: Int = 0,
    @SerializedName("start_time") val startTime: Long = 0,
    @SerializedName("radiant_win") val radiantWin: Boolean? = null,
    @SerializedName("radiant_score") val radiantScore: Int = 0,
    @SerializedName("dire_score") val direScore: Int = 0,
    @SerializedName("game_mode") val gameMode: Int = 0,
    @SerializedName("players") val players: List<Player> = emptyList(),
    @SerializedName("radiant_gold_adv") val radiantGoldAdv: List<Int> = emptyList(),
    @SerializedName("radiant_xp_adv") val radiantXpAdv: List<Int> = emptyList(),
    @SerializedName("objectives") val objectives: List<ObjectiveEvent> = emptyList(),
    @SerializedName("teamfights") val teamfights: List<Teamfight> = emptyList(),
    @SerializedName("obs_log") val observerWardEvents: List<WardEvent> = emptyList(),
    @SerializedName("sen_log") val sentryWardEvents: List<WardEvent> = emptyList()
)

data class Player(
    @SerializedName("account_id") val accountId: Long? = null,
    @SerializedName("personaname") val personaName: String? = null,
    @SerializedName("player_slot") val playerSlot: Int = 0,
    @SerializedName("hero_id") val heroId: Int = 0,
    @SerializedName("kills") val kills: Int = 0,
    @SerializedName("deaths") val deaths: Int = 0,
    @SerializedName("assists") val assists: Int = 0,
    @SerializedName("last_hits") val lastHits: Int = 0,
    @SerializedName("denies") val denies: Int = 0,
    @SerializedName("gold_per_min") val gpm: Int = 0,
    @SerializedName("xp_per_min") val xpm: Int = 0,
    @SerializedName("hero_damage") val heroDamage: Int = 0,
    @SerializedName("tower_damage") val towerDamage: Int = 0,
    @SerializedName("hero_healing") val heroHealing: Int = 0,
    @SerializedName("net_worth") val netWorth: Int = 0,
    @SerializedName("level") val level: Int = 0,
    @SerializedName("lane_role") val laneRole: Int = 0,
    @SerializedName("stuns") val stuns: Double? = null,
    @SerializedName("teamfight_participation") val teamfightParticipation: Double? = null,
    @SerializedName("lane_efficiency") val laneEfficiency: Double? = null,
    @SerializedName("lane_efficiency_pct") val laneEfficiencyPct: Double? = null,
    @SerializedName("camps_stacked") val campsStacked: Int? = null,
    @SerializedName("rune_pickups") val runePickups: Int? = null,
    @SerializedName("tower_kills") val towerKills: Int? = null,
    @SerializedName("roshan_kills") val roshanKills: Int? = null,
    @SerializedName("buyback_count") val buybackCount: Int? = null,
    @SerializedName("obs_placed") val observerWardsPlaced: Int? = null,
    @SerializedName("sen_placed") val sentryWardsPlaced: Int? = null,
    @SerializedName("observer_kills") val observerWardsKilled: Int? = null,
    @SerializedName("sentry_kills") val sentryWardsKilled: Int? = null,
    @SerializedName("courier_kills") val courierKills: Int? = null,
    @SerializedName("neutral_kills") val neutralKills: Int? = null,
    @SerializedName("hero_kills") val heroKills: Int? = null,
    @SerializedName("actions_per_min") val actionsPerMinute: Int? = null,
    @SerializedName("gold_t") val goldTimeline: List<Int>? = null,
    @SerializedName("xp_t") val xpTimeline: List<Int>? = null,
    @SerializedName("lh_t") val lastHitsTimeline: List<Int>? = null,
    @SerializedName("purchase_log") val purchaseLog: List<PurchaseEvent>? = null,
    @SerializedName("kills_log") val killsLog: List<KillEvent>? = null
) {
    val isRadiant: Boolean get() = playerSlot < 128
    val kda: Double get() = (kills + assists).toDouble() / maxOf(1, deaths)
}

data class PurchaseEvent(
    @SerializedName("time") val time: Int = 0,
    @SerializedName("key") val key: String = ""
)

data class KillEvent(
    @SerializedName("time") val time: Int = 0,
    @SerializedName("key") val key: String = ""
)


data class ObjectiveEvent(
    @SerializedName("time") val time: Int = 0,
    @SerializedName("type") val type: String = "",
    @SerializedName("key") val key: String = "",
    @SerializedName("slot") val slot: Int? = null,
    @SerializedName("player_slot") val playerSlot: Int? = null
)

data class Teamfight(
    @SerializedName("start") val start: Int = 0,
    @SerializedName("end") val end: Int = 0,
    @SerializedName("last_death") val lastDeath: Int = 0,
    @SerializedName("deaths") val deaths: Int = 0,
    @SerializedName("players") val players: List<TeamfightPlayer> = emptyList()
)

data class TeamfightPlayer(
    @SerializedName("player_slot") val playerSlot: Int = -1,
    @SerializedName("buyback") val buyback: Boolean? = null,
    @SerializedName("deaths") val deaths: Int? = null,
    @SerializedName("gold_delta") val goldDelta: Int? = null,
    @SerializedName("xp_delta") val xpDelta: Int? = null,
    @SerializedName("damage") val damage: Int? = null,
    @SerializedName("healing") val healing: Int? = null
)

data class WardEvent(
    @SerializedName("time") val time: Int = 0,
    @SerializedName("player_slot") val playerSlot: Int? = null,
    @SerializedName("x") val x: Int? = null,
    @SerializedName("y") val y: Int? = null,
    @SerializedName("key") val key: String = ""
)
