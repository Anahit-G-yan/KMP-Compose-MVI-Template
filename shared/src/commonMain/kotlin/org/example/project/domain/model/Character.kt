package org.example.project.domain.model

/**
 * Pure domain model. No serialization annotations, no knowledge of the
 * network layer - safe to pass all the way up to Compose.
 */
data class Character(
    val id: Int,
    val name: String,
    val status: CharacterStatus,
    val species: String,
    val imageUrl: String,
)
