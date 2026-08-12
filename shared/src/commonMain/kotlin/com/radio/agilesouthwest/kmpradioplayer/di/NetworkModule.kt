package com.radio.agilesouthwest.kmpradioplayer.di

import com.radio.agilesouthwest.kmpradioplayer.data.network.KtorRadioApiService
import com.radio.agilesouthwest.kmpradioplayer.data.network.RadioApiService
import com.radio.agilesouthwest.kmpradioplayer.data.repository.RadioRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val networkModule = module {
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    isLenient = true
                })
            }
            install(Logging) {
                level = LogLevel.INFO
            }
            defaultRequest {
                url("https://radio.agilesouthwest.com/")
            }
        }
    }

    single<RadioApiService> { KtorRadioApiService(get()) }
    single { RadioRepository(get()) }
}
