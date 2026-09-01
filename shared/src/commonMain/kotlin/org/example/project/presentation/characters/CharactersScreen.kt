package org.example.project.presentation.characters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.example.project.resources.Res
import org.example.project.resources.action_retry
import org.example.project.resources.error_generic
import org.example.project.presentation.characters.components.CharacterCard
import org.example.project.presentation.characters.components.CharacterCardPlaceholder
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val PAGINATION_TRIGGER_OFFSET = 5
private const val INITIAL_SKELETON_COUNT = 6

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharactersScreen(
    modifier: Modifier = Modifier,
    viewModel: CharactersViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Resolved here (composition, not the suspend collector below) because
    // stringResource() is a @Composable function - it can't be called from
    // inside LaunchedEffect's suspend block.
    val genericErrorMessage = stringResource(Res.string.error_generic)

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CharactersContract.Effect.ShowToast ->
                    snackbarHostState.showSnackbar(effect.message.ifBlank { genericErrorMessage })
            }
        }
    }

    // derivedStateOf collapses per-pixel scroll updates into a boolean that
    // only changes value when the "near the end" threshold is crossed, so
    // this doesn't recompute/recompose on every scroll frame.
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            totalItems > 0 && lastVisibleIndex >= totalItems - PAGINATION_TRIGGER_OFFSET
        }
    }

    // Fires setIntent exactly once per true-transition of shouldLoadMore.
    // The ViewModel's own hasNextPage/isPaginating guards make a stray
    // duplicate call harmless, but this keeps it from firing every frame.
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.setIntent(CharactersContract.Intent.LoadNextPage)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Box(
            modifier = Modifier.padding(paddingValues).fillMaxSize(),
        ) {
            when {
                state.isInitialLoading && state.characters.isEmpty() -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(INITIAL_SKELETON_COUNT) {
                            CharacterCardPlaceholder()
                        }
                    }
                }

                state.error != null && state.characters.isEmpty() -> {
                    ErrorState(
                        message = state.error.orEmpty().ifBlank { genericErrorMessage },
                        onRetry = { viewModel.setIntent(CharactersContract.Intent.Retry) },
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    )
                }

                else -> {
                    PullToRefreshBox(
                        isRefreshing = state.isInitialLoading,
                        onRefresh = { viewModel.setIntent(CharactersContract.Intent.Refresh) },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(
                                items = state.characters,
                                key = { it.id },
                            ) { character ->
                                CharacterCard(character = character)
                            }

                            if (state.isPaginating) {
                                item(key = "pagination_loader") {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onRetry) {
            Text(stringResource(Res.string.action_retry))
        }
    }
}
