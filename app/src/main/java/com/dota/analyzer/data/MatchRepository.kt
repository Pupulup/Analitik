package com.dota.analyzer.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MatchRepository {
    private val api: OpenDotaApi = Retrofit.Builder()
        .baseUrl("https://api.opendota.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenDotaApi::class.java)

    suspend fun getMatch(id: Long): Match {
        require(id > 0) { "Введите корректный ID матча." }
        return api.getMatch(id)
    }
}
