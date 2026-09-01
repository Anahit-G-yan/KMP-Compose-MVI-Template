package org.example.project.domain.model

/**
 * Named result of a single paginated fetch. Preferred over
 * `Pair<List<Character>, PageInfo>` so call sites read `page.characters` /
 * `page.pageInfo` instead of `.first` / `.second`.
 */
data class CharacterPage(
    val characters: List<Character>,
    val pageInfo: PageInfo,
)
