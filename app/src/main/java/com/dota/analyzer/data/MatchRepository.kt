package com.dota.analyzer.data

import okhttp3.Call
import okhttp3.EventListener
import okhttp3.Handshake
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.Locale
import java.util.concurrent.TimeUnit

class MatchRepository {
    @Volatile
    var lastFailureDiagnostic: String? = null
        private set

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .eventListenerFactory {
            object : EventListener() {
                private var stage = "подготовка запроса"
                private var startedAtNanos = 0L

                private fun mark(value: String) {
                    stage = value
                }

                override fun callStart(call: Call) {
                    startedAtNanos = System.nanoTime()
                    lastFailureDiagnostic = null
                    mark("подготовка запроса")
                }

                override fun dnsStart(call: Call, domainName: String) {
                    mark("DNS-разрешение имени")
                }

                override fun dnsEnd(
                    call: Call,
                    domainName: String,
                    inetAddressList: List<InetAddress>
                ) {
                    mark("DNS завершён")
                }

                override fun connectStart(
                    call: Call,
                    inetSocketAddress: InetSocketAddress,
                    proxy: Proxy
                ) {
                    mark("TCP-подключение")
                }

                override fun secureConnectStart(call: Call) {
                    mark("TLS-рукопожатие")
                }

                override fun secureConnectEnd(call: Call, handshake: Handshake?) {
                    mark("TLS завершён")
                }

                override fun requestHeadersStart(call: Call) {
                    mark("отправка HTTP-заголовков")
                }

                override fun requestBodyStart(call: Call) {
                    mark("отправка тела запроса")
                }

                override fun responseHeadersStart(call: Call) {
                    mark("ожидание HTTP-заголовков ответа")
                }

                override fun responseBodyStart(call: Call) {
                    mark("чтение тела ответа")
                }

                override fun callFailed(call: Call, ioe: IOException) {
                    val elapsedSeconds = if (startedAtNanos == 0L) {
                        0.0
                    } else {
                        (System.nanoTime() - startedAtNanos) / 1_000_000_000.0
                    }
                    lastFailureDiagnostic = "$stage, прошло ${"%.1f".format(Locale.US, elapsedSeconds)} с"
                }
            }
        }
        .build()

    private val api: OpenDotaApi = Retrofit.Builder()
        .baseUrl("https://api.opendota.com/")
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenDotaApi::class.java)

    @Volatile
    private var cachedHeroData: List<Hero> = emptyList()

    private suspend fun loadHeroes(): List<Hero> {
        if (cachedHeroData.isEmpty()) {
            val loaded = api.getHeroes()
            if (loaded.isNotEmpty()) cachedHeroData = loaded
        }
        return cachedHeroData
    }

    suspend fun getMatch(id: Long): Match {
        require(id > 0) { "Введите корректный ID матча." }
        return api.getMatch(id)
    }

    suspend fun getHeroNames(): Map<Int, String> =
        loadHeroes().associate { it.id to it.localizedName }

    suspend fun getHeroImageNames(): Map<Int, String> =
        loadHeroes().associate { it.id to it.internalName.removePrefix("npc_dota_hero_") }

    private fun normalizeAccountId(accountId: Long): Long {
        require(accountId > 0) { "Введите корректный Steam Account ID." }
        val steam64Base = 76561197960265728L
        return if (accountId >= steam64Base) accountId - steam64Base else accountId
    }

    suspend fun getPlayerProfile(accountId: Long): PlayerProfileResponse =
        api.getPlayerProfile(normalizeAccountId(accountId))

    suspend fun getPlayerMatches(accountId: Long, limit: Int = 30): List<PlayerMatchSummary> =
        api.getPlayerMatches(normalizeAccountId(accountId), limit.coerceIn(1, 100))

    suspend fun getGuideHeroes(): List<HeroStat> =
        api.getHeroStats().filter { it.id > 0 && it.localizedName.isNotBlank() }
            .sortedBy { it.localizedName }

    suspend fun getItemConstants(): Map<String, ItemConstant> = api.getItemConstants()

    suspend fun getHeroGuide(
        heroId: Int,
        heroes: List<HeroStat>,
        itemConstants: Map<String, ItemConstant>
    ): HeroGuideData {
        require(heroId > 0) { "Некорректный ID героя." }
        val hero = heroes.firstOrNull { it.id == heroId }
            ?: api.getHeroStats().firstOrNull { it.id == heroId }
            ?: throw IllegalArgumentException("Герой не найден.")

        val popularity = api.getHeroItemPopularity(heroId)
        val matchups = api.getHeroMatchups(heroId)
        val heroNames = heroes.associate { it.id to it.localizedName }

        fun buildItems(values: Map<String, Int>): List<GuideItem> =
            values.entries
                .filter { it.value > 0 }
                .sortedByDescending { it.value }
                .take(10)
                .map { entry ->
                    val name = itemConstants[entry.key]?.displayName
                        ?.takeIf { it.isNotBlank() }
                        ?: entry.key.replace('_', ' ').replaceFirstChar { it.uppercase() }
                    GuideItem(entry.key, name, entry.value)
                }

        val difficult = matchups
            .filter { it.gamesPlayed >= 20 }
            .mapNotNull { row ->
                val name = heroNames[row.heroId] ?: return@mapNotNull null
                GuideMatchup(
                    heroName = name,
                    games = row.gamesPlayed,
                    winRate = row.wins * 100.0 / row.gamesPlayed
                )
            }
            .sortedBy { it.winRate }
            .take(10)

        return HeroGuideData(
            hero = hero,
            startItems = buildItems(popularity.startGameItems),
            earlyItems = buildItems(popularity.earlyGameItems),
            midItems = buildItems(popularity.midGameItems),
            lateItems = buildItems(popularity.lateGameItems),
            hardMatchups = difficult
        )
    }
}
