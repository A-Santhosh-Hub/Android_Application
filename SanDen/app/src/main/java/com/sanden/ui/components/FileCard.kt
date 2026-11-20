package com.sanden.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sanden.domain.model.FileCategory
import com.sanden.domain.model.VaultFile
import com.sanden.ui.theme.CategoryAudio
import com.sanden.ui.theme.CategoryAudioContainer
import com.sanden.ui.theme.CategoryDocument
import com.sanden.ui.theme.CategoryDocumentContainer
import com.sanden.ui.theme.CategoryOther
import com.sanden.ui.theme.CategoryOtherContainer
import com.sanden.ui.theme.GlassBorderBottomDark
import com.sanden.ui.theme.GlassBorderBottomLight
import com.sanden.ui.theme.GlassBorderTopDark
import com.sanden.ui.theme.GlassBorderTopLight
import com.sanden.ui.theme.GlassSurfaceDark
import com.sanden.ui.theme.GlassSurfaceLight
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileCard(
    file: VaultFile,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    isGridView: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "fileCardBorder"
    )

    if (isGridView) {
        GridFileCard(
            file = file,
            isSelected = isSelected,
            isSelectionMode = isSelectionMode,
            borderColor = borderColor,
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = modifier
        )
    } else {
        ListFileCard(
            file = file,
            isSelected = isSelected,
            isSelectionMode = isSelectionMode,
            borderColor = borderColor,
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridFileCard(
    file: VaultFile,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    borderColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "gridCardScale"
    )
    val glassBg = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    } else if (isDark) {
        GlassSurfaceDark
    } else {
        GlassSurfaceLight
    }
    val shape = RoundedCornerShape(22.dp)
    val defaultBorderBrush = Brush.verticalGradient(
        if (isDark) listOf(GlassBorderTopDark, GlassBorderBottomDark)
        else listOf(GlassBorderTopLight, GlassBorderBottomLight)
    )

    Box(
        modifier = modifier
            .scale(scale)
            .fillMaxWidth()
            .clip(shape)
            .background(glassBg)
            .then(
                if (isSelected) Modifier.border(2.dp, borderColor, shape)
                else Modifier.border(1.dp, defaultBorderBrush, shape)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                FileThumbnail(file = file, modifier = Modifier.fillMaxSize())

                if (isSelectionMode) {
                    SelectionBadge(
                        isSelected = isSelected,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (file.isDirectory) "Folder" else file.formattedSize,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListFileCard(
    file: VaultFile,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    borderColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "listCardScale"
    )
    val glassBg = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    } else if (isDark) {
        GlassSurfaceDark
    } else {
        GlassSurfaceLight
    }
    val shape = RoundedCornerShape(20.dp)
    val defaultBorderBrush = Brush.verticalGradient(
        if (isDark) listOf(GlassBorderTopDark, GlassBorderBottomDark)
        else listOf(GlassBorderTopLight, GlassBorderBottomLight)
    )

    Box(
        modifier = modifier
            .scale(scale)
            .fillMaxWidth()
            .clip(shape)
            .background(glassBg)
            .then(
                if (isSelected) Modifier.border(2.dp, borderColor, shape)
                else Modifier.border(1.dp, defaultBorderBrush, shape)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                FileThumbnail(file = file, modifier = Modifier.fillMaxSize())
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (file.isDirectory) "Folder" else file.formattedSize,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = formatDate(file.lastModified),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            if (isSelectionMode) {
                SelectionBadge(
                    isSelected = isSelected,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SelectionBadge(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) primaryColor
                else Color.Black.copy(alpha = 0.4f)
            )
            .border(1.5.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun FileThumbnail(
    file: VaultFile,
    modifier: Modifier = Modifier
) {
    if (file.isDirectory) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Folder,
                contentDescription = "Folder",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
        return
    }

    if (file.category == FileCategory.IMAGE || file.category == FileCategory.VIDEO) {
        val context = LocalContext.current
        val imageRequest = ImageRequest.Builder(context)
            .data(File(file.path))
            .crossfade(true)
            .build()

        AsyncImage(
            model = imageRequest,
            contentDescription = file.name,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        val (icon, tint, bg) = when (file.category) {
            FileCategory.AUDIO -> Triple(Icons.Rounded.Audiotrack, CategoryAudio, CategoryAudioContainer)
            FileCategory.DOCUMENT -> Triple(Icons.Rounded.Description, CategoryDocument, CategoryDocumentContainer)
            FileCategory.OTHER -> Triple(Icons.Rounded.FolderZip, CategoryOther, CategoryOtherContainer)
            else -> Triple(Icons.Rounded.InsertDriveFile, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
        }

        Box(
            modifier = modifier.background(bg.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = file.category.name,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    if (timestamp <= 0) return ""
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
