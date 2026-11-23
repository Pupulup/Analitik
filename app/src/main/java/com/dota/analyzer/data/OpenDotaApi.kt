package com.dota.analyzer.data

import retrofit2.http.GET
import retrofit2.http.Path

interface OpenDotaApi {
    @GET("api/matches/{matchId}")
    suspend fun getMatch(@Path("matchId") matchId: Long): Match

    @GET("api/heroes")
    suspend fun getHeroes(): List<Hero>
}
