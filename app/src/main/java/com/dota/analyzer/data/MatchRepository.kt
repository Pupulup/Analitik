package com.dota.analyzer.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class MatchRepository {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val api: OpenDotaApi = Retrofit.Builder()
        .baseUrl("https://api.opendota.com/")
        .client(httpClient)
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
