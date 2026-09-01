package org.example.project.data.remote

/**
 * Contract-level wrapper returned by the remote data source. Keeps Ktor-specific
 * exceptions and HTTP status codes out of the domain/presentation layers -
 * [org.example.project.data.repository.RickAndMortyRepositoryImpl] (Step 3) is the
 * only place that unwraps this into a domain-level result.
 */
sealed interface NetworkResult<out T> {

    data class Success<T>(val data: T) : NetworkResult<T>

    data class Error(
        val exception: Throwable,
        val code: Int? = null,
        val message: String? = exception.message,
    ) : NetworkResult<Nothing>
}

/**
 * Convenience inline mapper so callers can transform a successful payload
 * without unwrapping the sealed type manually.
 */
inline fun <T, R> NetworkResult<T>.map(transform: (T) -> R): NetworkResult<R> =
    when (this) {
        is NetworkResult.Success -> NetworkResult.Success(transform(data))
        is NetworkResult.Error -> this
    }
