package org.example.project.data.remote.datasource

import org.example.project.data.remote.NetworkResult
import org.example.project.data.remote.dto.CharacterResponseDto

interface RickAndMortyRemoteDataSource {

    /**
     * Fetches one page of characters from the Rick and Morty API.
     *
     * @param page 1-based page index, as expected by the API's `page` query parameter.
     */
    suspend fun getCharacters(page: Int): NetworkResult<CharacterResponseDto>
}
