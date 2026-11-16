package com.sanden.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanden.domain.model.FileCategory
import com.sanden.ui.theme.CategoryAudio
import com.sanden.ui.theme.CategoryAudioContainer
import com.sanden.ui.theme.CategoryDocument
import com.sanden.ui.theme.CategoryDocumentContainer
import com.sanden.ui.theme.CategoryImage
import com.sanden.ui.theme.CategoryImageContainer
import com.sanden.ui.theme.CategoryOther
import com.sanden.ui.theme.CategoryOtherContainer
import com.sanden.ui.theme.CategoryVideo
import com.sanden.ui.theme.CategoryVideoContainer

@Composable
fun CategoryCard(
    category: FileCategory,
    count: Int,
    totalSizeBytes: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "categoryCardScale"
    )

    val (icon, accentColor, containerColor, title) = when (category) {
        FileCategory.IMAGE -> Quadruple(Icons.Rounded.Image, CategoryImage, CategoryImageContainer, "Images")
        FileCategory.AUDIO -> Quadruple(Icons.Rounded.Audiotrack, CategoryAudio, CategoryAudioContainer, "Audio")
        FileCategory.VIDEO -> Quadruple(Icons.Rounded.Videocam, CategoryVideo, CategoryVideoContainer, "Videos")
        FileCategory.DOCUMENT -> Quadruple(Icons.Rounded.Description, CategoryDocument, CategoryDocumentContainer, "Documents")
        FileCategory.OTHER -> Quadruple(Icons.Rounded.FolderZip, CategoryOther, CategoryOtherContainer, "Others")
    }

    Card(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            containerColor.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(containerColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = accentColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = formatFileSize(totalSizeBytes),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val clampedGroup = digitGroups.coerceIn(0, units.size - 1)
    val value = bytes / Math.pow(1024.0, clampedGroup.toDouble())
    return "%.1f %s".format(value, units[clampedGroup])
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
