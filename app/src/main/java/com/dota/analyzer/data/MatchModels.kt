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
    @SerializedName("players") val players: List<Player> = emptyList()
)

data class Player(
    @SerializedName("account_id") val accountId: Long? = null,
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
    @SerializedName("level") val level: Int = 0
) {
    val isRadiant: Boolean get() = playerSlot < 128
    val kda: Double get() = (kills + assists).toDouble() / maxOf(1, deaths)
}
