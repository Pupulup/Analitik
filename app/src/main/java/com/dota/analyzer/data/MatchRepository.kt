package com.dota.analyzer.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MatchRepository {
    private val api: OpenDotaApi = Retrofit.Builder()
        .baseUrl("https://api.opendota.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenDotaApi::class.java)

    @Volatile
    private var cachedHeroes: Map<Int, String> = emptyMap()

    suspend fun getMatch(id: Long): Match {
        require(id > 0) { "Введите корректный ID матча." }
        return api.getMatch(id)
    }

    suspend fun getHeroNames(): Map<Int, String> {
        if (cachedHeroes.isEmpty()) {
            val loaded = api.getHeroes().associate { it.id to it.localizedName }
            if (loaded.isNotEmpty()) cachedHeroes = loaded
        }
        return cachedHeroes
    }
}
