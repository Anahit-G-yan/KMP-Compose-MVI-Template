package org.example.project.data.remote.datasource

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import org.example.project.data.remote.NetworkResult
import org.example.project.data.remote.dto.CharacterResponseDto

class RickAndMortyRemoteDataSourceImpl(
    private val httpClient: HttpClient,
) : RickAndMortyRemoteDataSource {

    override suspend fun getCharacters(page: Int): NetworkResult<CharacterResponseDto> {
        return try {
            val response = httpClient.get(CHARACTERS_ENDPOINT) {
                parameter(PAGE_QUERY_PARAM, page)
            }

            if (response.status.isSuccess()) {
                NetworkResult.Success(response.body())
            } else {
                NetworkResult.Error(
                    exception = IllegalStateException("Request failed with ${response.status}"),
                    code = response.status.value,
                )
            }
        } catch (e: CancellationException) {
            // Never swallow coroutine cancellation - rethrow so structured
            // concurrency (e.g. viewModelScope cancellation) works correctly.
            throw e
        } catch (e: Exception) {
            NetworkResult.Error(exception = e)
        }
    }

    private companion object {
        // The client isn't configured with a base URL yet (that lands in the
        // Koin wiring step), so this data source stays self-contained for now.
        const val CHARACTERS_ENDPOINT = "https://rickandmortyapi.com/api/character"
        const val PAGE_QUERY_PARAM = "page"
    }
}
