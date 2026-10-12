package com.example.aqisensor.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Factory that builds a Retrofit instance pointed at a specific device IP.
 *
 * A new instance is created whenever the user changes the IP address in Settings,
 * so no singleton pattern is used here — the ViewModel/Repository holds the reference.
 */
object NetworkModule {

    private const val CONNECT_TIMEOUT_S = 3L
    private const val READ_TIMEOUT_S    = 3L

    /**
     * Create an [AqiApiService] targeting the given [ipAddress].
     * The base URL is plain HTTP — cleartext is permitted for private LAN IPs
     * via the network_security_config.xml and usesCleartextTraffic manifest flag.
     */
    fun createApiService(ipAddress: String): AqiApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_S, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_S, TimeUnit.SECONDS)
            .writeTimeout(CONNECT_TIMEOUT_S, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()

        // Normalize the IP — strip scheme if user accidentally included it
        val cleanIp = ipAddress
            .removePrefix("http://")
            .removePrefix("https://")
            .trim()

        val baseUrl = "http://$cleanIp/"

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AqiApiService::class.java)
    }
}
