package org.example.project.data.repository

import org.example.project.data.remote.NetworkResult
import org.example.project.data.remote.datasource.RickAndMortyRemoteDataSource
import org.example.project.data.remote.dto.CharacterDto
import org.example.project.data.remote.dto.CharacterResponseDto
import org.example.project.data.remote.map
import org.example.project.domain.model.Character
import org.example.project.domain.model.CharacterPage
import org.example.project.domain.model.CharacterStatus
import org.example.project.domain.model.PageInfo
import org.example.project.domain.repository.RickAndMortyRepository

class RickAndMortyRepositoryImpl(
    private val remoteDataSource: RickAndMortyRemoteDataSource,
) : RickAndMortyRepository {

    override suspend fun getCharacters(page: Int): Result<CharacterPage> {
        return when (val result = remoteDataSource.getCharacters(page).map { it.toDomain() }) {
            is NetworkResult.Success -> Result.success(result.data)
            is NetworkResult.Error -> Result.failure(result.exception)
        }
    }

    private fun CharacterResponseDto.toDomain(): CharacterPage = CharacterPage(
        characters = results.map { it.toDomain() },
        pageInfo = PageInfo(
            pages = info.pages,
            hasNextPage = info.next != null,
        ),
    )

    private fun CharacterDto.toDomain(): Character = Character(
        id = id,
        name = name,
        status = CharacterStatus.fromRaw(status),
        species = species,
        imageUrl = image,
    )
}
