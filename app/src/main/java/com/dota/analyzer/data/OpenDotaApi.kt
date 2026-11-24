package com.dota.analyzer.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenDotaApi {
    @GET("api/matches/{matchId}")
    suspend fun getMatch(@Path("matchId") matchId: Long): Match

    @GET("api/heroes")
    suspend fun getHeroes(): List<Hero>

    @GET("api/players/{accountId}")
    suspend fun getPlayerProfile(@Path("accountId") accountId: Long): PlayerProfileResponse

    @GET("api/players/{accountId}/matches")
    suspend fun getPlayerMatches(
        @Path("accountId") accountId: Long,
        @Query("limit") limit: Int = 30
    ): List<PlayerMatchSummary>

    @GET("api/heroStats")
    suspend fun getHeroStats(): List<HeroStat>

    @GET("api/heroes/{heroId}/itemPopularity")
    suspend fun getHeroItemPopularity(@Path("heroId") heroId: Int): HeroItemPopularity

    @GET("api/heroes/{heroId}/matchups")
    suspend fun getHeroMatchups(@Path("heroId") heroId: Int): List<HeroMatchup>

    @GET("api/constants/items")
    suspend fun getItemConstants(): Map<String, ItemConstant>
}
