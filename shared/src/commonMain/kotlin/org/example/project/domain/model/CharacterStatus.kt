package org.example.project.domain.model

/**
 * Domain-level representation of a character's vital status. Keeping this as
 * an enum (rather than the raw API string) pushes the "unknown status" case
 * into the type system instead of leaking free-form strings into the UI.
 */
enum class CharacterStatus {
    ALIVE,
    DEAD,
    UNKNOWN;

    companion object {
        fun fromRaw(raw: String): CharacterStatus = when (raw.lowercase()) {
            "alive" -> ALIVE
            "dead" -> DEAD
            else -> UNKNOWN
        }
    }
}
