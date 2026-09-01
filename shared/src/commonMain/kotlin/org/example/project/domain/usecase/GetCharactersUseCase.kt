package org.example.project.domain.usecase

import org.example.project.domain.model.CharacterPage
import org.example.project.domain.repository.RickAndMortyRepository

/**
 * Single-responsibility use case exposed to the presentation layer. Thin by
 * design for now - it exists as the seam where paging/business rules (e.g.
 * caching, retry policy) can be added later without touching the ViewModel.
 */
class GetCharactersUseCase(
    private val repository: RickAndMortyRepository,
) {
    suspend fun execute(page: Int): Result<CharacterPage> = repository.getCharacters(page)
}
