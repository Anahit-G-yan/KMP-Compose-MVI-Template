package org.example.project.presentation.characters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import org.example.project.domain.usecase.GetCharactersUseCase

/**
 * The public Rick and Morty API rate-limits bursts of requests (HTTP 429).
 * A fast fling can cross the "near the end of the list" pagination trigger
 * several times in quick succession - the intent queue in this ViewModel
 * already guarantees requests never overlap, but without this it would
 * still fire them back-to-back with ~0ms between them.
 */
private val MIN_REQUEST_INTERVAL = 400.milliseconds

class CharactersViewModel(
    private val getCharactersUseCase: GetCharactersUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(CharactersContract.State())
    val state: StateFlow<CharactersContract.State> = _state.asStateFlow()

    // extraBufferCapacity + DROP_OLDEST ensures emit() never suspends: an
    // Effect emitted while no screen is collecting (e.g. process death,
    // config change gap) is simply dropped instead of blocking the intent
    // processing loop below forever.
    private val _effect = MutableSharedFlow<CharactersContract.Effect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val effect: SharedFlow<CharactersContract.Effect> = _effect.asSharedFlow()

    // All intents funnel through this channel and are drained by a single
    // coroutine (see init). That gives us a strictly serial intent queue for
    // free: no matter how fast the UI fires LoadNextPage while scrolling,
    // handleIntent() calls can never run concurrently with each other, so
    // currentPage/hasNextPage can never be read-modified-written by two
    // in-flight requests at once.
    private val intents = Channel<CharactersContract.Intent>(Channel.UNLIMITED)

    private var currentPage = 0
    private var hasNextPage = true
    private var lastFetchMark: TimeMark? = null

    init {
        viewModelScope.launch {
            for (intent in intents) {
                handleIntent(intent)
            }
        }
        setIntent(CharactersContract.Intent.LoadNextPage)
    }

    fun setIntent(intent: CharactersContract.Intent) {
        intents.trySend(intent)
    }

    private suspend fun handleIntent(intent: CharactersContract.Intent) {
        when (intent) {
            CharactersContract.Intent.LoadNextPage -> onLoadNextPage()
            CharactersContract.Intent.Refresh -> onRefresh()
            CharactersContract.Intent.Retry -> onRetry()
        }
    }

    private suspend fun onLoadNextPage() {
        val current = _state.value
        if (!hasNextPage || current.isInitialLoading || current.isPaginating) return
        fetchPage(nextPage = currentPage + 1)
    }

    private suspend fun onRefresh() {
        currentPage = 0
        hasNextPage = true
        fetchPage(nextPage = 1)
    }

    private suspend fun onRetry() {
        // Whatever page failed - the initial one (currentPage == 0) or the
        // next one during pagination - is exactly currentPage + 1, since
        // currentPage is only advanced on success.
        fetchPage(nextPage = currentPage + 1)
    }

    private suspend fun fetchPage(nextPage: Int) {
        // Throttle: never issue two requests closer together than
        // MinRequestInterval, even if several page-load intents were queued
        // up back-to-back by a fast fling.
        lastFetchMark?.elapsedNow()?.let { elapsed ->
            if (elapsed < MIN_REQUEST_INTERVAL) delay(MIN_REQUEST_INTERVAL - elapsed)
        }
        lastFetchMark = TimeSource.Monotonic.markNow()

        val isInitialLoad = currentPage == 0

        _state.update {
            it.copy(
                isInitialLoading = isInitialLoad,
                isPaginating = !isInitialLoad,
                error = null,
            )
        }

        getCharactersUseCase.execute(nextPage)
            .onSuccess { page ->
                currentPage = nextPage
                hasNextPage = page.pageInfo.hasNextPage
                _state.update {
                    it.copy(
                        characters = if (isInitialLoad) page.characters else it.characters + page.characters,
                        isInitialLoading = false,
                        isPaginating = false,
                        error = null,
                    )
                }
            }
            .onFailure { throwable ->
                // No English fallback text here on purpose: the ViewModel has
                // no @Composable context to call stringResource() from, so it
                // hands up whatever the exception says (possibly blank) and
                // leaves picking a localized, user-facing fallback to the UI.
                val message = throwable.message.orEmpty()
                _state.update {
                    it.copy(
                        isInitialLoading = false,
                        isPaginating = false,
                        error = message,
                    )
                }
                _effect.emit(CharactersContract.Effect.ShowToast(message))
            }
    }
}
