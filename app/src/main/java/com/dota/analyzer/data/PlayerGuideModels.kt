package com.dota.analyzer.data

import com.google.gson.annotations.SerializedName

data class PlayerProfileResponse(
    @SerializedName("profile") val profile: PlayerProfile? = null,
    @SerializedName("rank_tier") val rankTier: Int? = null,
    @SerializedName("leaderboard_rank") val leaderboardRank: Int? = null
)

data class PlayerProfile(
    @SerializedName("account_id") val accountId: Long? = null,
    @SerializedName("personaname") val personaname: String? = null,
    @SerializedName("avatarfull") val avatarFull: String? = null
)

data class PlayerMatchSummary(
    @SerializedName("match_id") val matchId: Long = 0,
    @SerializedName("player_slot") val playerSlot: Int = 0,
    @SerializedName("radiant_win") val radiantWin: Boolean? = null,
    @SerializedName("duration") val duration: Int = 0,
    @SerializedName("start_time") val startTime: Long = 0,
    @SerializedName("game_mode") val gameMode: Int = 0,
    @SerializedName("hero_id") val heroId: Int = 0,
    @SerializedName("kills") val kills: Int = 0,
    @SerializedName("deaths") val deaths: Int = 0,
    @SerializedName("assists") val assists: Int = 0,
    @SerializedName("gold_per_min") val gpm: Int = 0,
    @SerializedName("xp_per_min") val xpm: Int = 0,
    @SerializedName("last_hits") val lastHits: Int = 0,
    @SerializedName("hero_damage") val heroDamage: Int = 0,
    @SerializedName("lane_role") val laneRole: Int? = null
) {
    val didWin: Boolean?
        get() = radiantWin?.let { it == (playerSlot < 128) }
}

data class HeroStat(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("localized_name") val localizedName: String = "",
    @SerializedName("img") val img: String? = null,
    @SerializedName("1_pick") val pickCount: Int? = null
)

data class ItemConstant(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("dname") val displayName: String? = null
)

data class HeroItemPopularity(
    @SerializedName("start_game_items") val startGameItems: Map<String, Int> = emptyMap(),
    @SerializedName("early_game_items") val earlyGameItems: Map<String, Int> = emptyMap(),
    @SerializedName("mid_game_items") val midGameItems: Map<String, Int> = emptyMap(),
    @SerializedName("late_game_items") val lateGameItems: Map<String, Int> = emptyMap()
)

data class HeroMatchup(
    @SerializedName("hero_id") val heroId: Int = 0,
    @SerializedName("games_played") val gamesPlayed: Int = 0,
    @SerializedName("wins") val wins: Int = 0
)

data class GuideItem(val key: String, val name: String, val matches: Int)

data class GuideMatchup(val heroName: String, val games: Int, val winRate: Double)

data class HeroGuideData(
    val hero: HeroStat,
    val startItems: List<GuideItem>,
    val earlyItems: List<GuideItem>,
    val midItems: List<GuideItem>,
    val lateItems: List<GuideItem>,
    val hardMatchups: List<GuideMatchup>
)
