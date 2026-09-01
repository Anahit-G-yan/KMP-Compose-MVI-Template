package org.example.project.domain.repository

import org.example.project.domain.model.CharacterPage

/**
 * Domain boundary. Only depends on domain models and kotlin.Result - no
 * Ktor, no DTOs, no coroutines-adjacent framework types.
 */
interface RickAndMortyRepository {

    suspend fun getCharacters(page: Int): Result<CharacterPage>
}
