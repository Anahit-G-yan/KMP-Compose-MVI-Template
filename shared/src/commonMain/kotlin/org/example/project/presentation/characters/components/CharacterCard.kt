package org.example.project.presentation.characters.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import org.example.project.resources.Res
import org.example.project.resources.status_alive
import org.example.project.resources.status_dead
import org.example.project.resources.status_unknown
import org.example.project.domain.model.Character
import org.example.project.domain.model.CharacterStatus
import org.example.project.presentation.common.shimmer
import org.jetbrains.compose.resources.stringResource

private val CardShape = RoundedCornerShape(16.dp)
private val AvatarShape = RoundedCornerShape(12.dp)
private val AvatarSize = 72.dp

@Composable
fun CharacterCard(
    character: Character,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CharacterAvatar(
                imageUrl = character.imageUrl,
                modifier = Modifier.size(AvatarSize).clip(AvatarShape),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = character.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = character.species,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                StatusBadge(status = character.status)
            }
        }
    }
}

/**
 * Skeleton twin of [CharacterCard] - same shape, same spacing - shown while
 * [org.example.project.presentation.characters.CharactersContract.State.isInitialLoading]
 * is true and no data has arrived yet.
 */
@Composable
fun CharacterCardPlaceholder(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(AvatarSize).clip(AvatarShape).shimmer())
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(Modifier.fillMaxWidth(0.6f).height(16.dp).clip(RoundedCornerShape(4.dp)).shimmer())
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth(0.4f).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmer())
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth(0.3f).height(12.dp).clip(RoundedCornerShape(4.dp)).shimmer())
            }
        }
    }
}

@Composable
private fun CharacterAvatar(
    imageUrl: String,
    modifier: Modifier = Modifier,
) {
    SubcomposeAsyncImage(
        model = imageUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    ) {
        // painter.state is a StateFlow<State>, not the State itself - must be
        // collected to read the current value inside this Composable scope.
        when (painter.state.collectAsState().value) {
            is AsyncImagePainter.State.Loading -> Box(Modifier.fillMaxSize().shimmer())
            is AsyncImagePainter.State.Error -> Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
            )
            else -> SubcomposeAsyncImageContent()
        }
    }
}

@Composable
private fun StatusBadge(
    status: CharacterStatus,
    modifier: Modifier = Modifier,
) {
    val (color, labelRes) = when (status) {
        CharacterStatus.ALIVE -> AliveColor to Res.string.status_alive
        CharacterStatus.DEAD -> DeadColor to Res.string.status_dead
        CharacterStatus.UNKNOWN -> UnknownColor to Res.string.status_unknown
    }
    val label = stringResource(labelRes)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Deliberately fixed traffic-light colors rather than MaterialTheme roles:
// alive/dead/unknown is universal domain semantics, not a themeable choice.
private val AliveColor = Color(0xFF2E7D32)
private val DeadColor = Color(0xFFC62828)
private val UnknownColor = Color(0xFF9E9E9E)
