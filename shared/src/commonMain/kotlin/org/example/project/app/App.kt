package org.example.project.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import io.ktor.client.HttpClient
import org.example.project.presentation.characters.CharactersScreen
import org.koin.compose.koinInject

@Composable
@Preview
fun App() {
    // Reuses the same HttpClient the API layer uses (single connection pool,
    // one Ktor engine, one place that configures timeouts/logging) instead
    // of letting Coil spin up a client of its own.
    val httpClient = koinInject<HttpClient>()
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory(httpClient)) }
            .build()
    }

    MaterialTheme {
        CharactersScreen()
    }
}
