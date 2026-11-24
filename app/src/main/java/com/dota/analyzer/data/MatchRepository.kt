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
