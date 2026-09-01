package org.example.project.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.example.project.data.remote.datasource.RickAndMortyRemoteDataSource
import org.example.project.data.remote.datasource.RickAndMortyRemoteDataSourceImpl
import org.example.project.data.repository.RickAndMortyRepositoryImpl
import org.example.project.domain.repository.RickAndMortyRepository
import org.example.project.domain.usecase.GetCharactersUseCase
import org.example.project.presentation.characters.CharactersViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * Only the HttpClient's *engine* is platform-specific (OkHttp on Android,
 * Darwin on iOS) - it's provided by [platformModule]. Everything else about
 * the client (plugins, JSON config) is identical on every platform and
 * belongs here.
 */
val networkModule = module {
    single {
        HttpClient(get<HttpClientEngine>()) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    },
                )
            }
            install(Logging) {
                level = LogLevel.INFO
            }
        }
    }
}

/**
 * Data -> Domain -> Presentation wiring. Bound against interfaces
 * (`RickAndMortyRemoteDataSource`, `RickAndMortyRepository`) so nothing
 * above the data layer ever resolves a concrete `*Impl` type from Koin.
 */
val appModule = module {
    single<RickAndMortyRemoteDataSource> { RickAndMortyRemoteDataSourceImpl(get()) }
    single<RickAndMortyRepository> { RickAndMortyRepositoryImpl(get()) }
    factory { GetCharactersUseCase(get()) }

    viewModelOf(::CharactersViewModel)
}

/**
 * Supplies the one thing that genuinely differs per platform: the Ktor
 * engine. Implemented as `actual val` in androidMain/iosMain.
 */
expect val platformModule: Module

/**
 * Single entry point for wiring the whole dependency graph. `appDeclaration`
 * lets each platform contribute platform-only Koin configuration (e.g.
 * `androidContext(...)` on Android) without this function needing to know
 * platforms exist.
 */
fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(platformModule, networkModule, appModule)
    }
}
