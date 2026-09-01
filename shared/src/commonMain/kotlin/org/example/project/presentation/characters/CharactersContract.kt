package org.example.project.presentation.characters

import org.example.project.domain.model.Character

/**
 * MVI contract for the character list screen. UI only ever talks to the
 * ViewModel through [Intent] and only ever observes [State] / [Effect] -
 * no direct function calls into the ViewModel's internals.
 */
object CharactersContract {

    /**
     * A single, self-sufficient snapshot of the screen. Modeled as one data
     * class rather than a `Loading/Success/Error` sealed hierarchy because
     * pagination needs states a sealed split can't express cleanly (e.g.
     * "showing 40 already-loaded characters while page 3 is in flight" is
     * both "has data" and "is loading" at once).
     */
    data class State(
        val characters: List<Character> = emptyList(),
        val isInitialLoading: Boolean = false,
        val isPaginating: Boolean = false,
        val error: String? = null,
    ) {
        val isEmpty: Boolean
            get() = characters.isEmpty() && !isInitialLoading && error == null
    }

    sealed interface Intent {
        /** Triggered when the list scroll position nears the end. */
        data object LoadNextPage : Intent

        /** Triggered by pull-to-refresh; resets pagination back to page 1. */
        data object Refresh : Intent

        /** Triggered by a "Retry" button shown alongside [State.error]. */
        data object Retry : Intent
    }

    sealed interface Effect {
        data class ShowToast(val message: String) : Effect
    }
}
