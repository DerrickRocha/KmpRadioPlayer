package com.radio.agilesouthwest.kmpradioplayer.di

import com.radio.agilesouthwest.kmpradioplayer.data.network.KtorRadioApiService
import com.radio.agilesouthwest.kmpradioplayer.data.network.RadioApiService
import com.radio.agilesouthwest.kmpradioplayer.data.repository.RadioRepository
import com.radio.agilesouthwest.kmpradioplayer.ui.screens.stations.StationsViewModel
import com.radio.agilesouthwest.kmpradioplayer.ui.screens.tags.TagsViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.qualifier.named
import org.koin.dsl.module

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

val networkModule = module {
    single(named("ioDispatcher")) { Dispatchers.IO }

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
    single { RadioRepository(get(), get(named("ioDispatcher"))) }
    
    factory { TagsViewModel(get()) }
    factory { (tagName: String?) -> StationsViewModel(get(), tagName) }
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(networkModule)
    }
