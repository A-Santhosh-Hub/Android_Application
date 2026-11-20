package com.sanden.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanden.domain.model.FileCategory
import com.sanden.ui.theme.CategoryAudio
import com.sanden.ui.theme.CategoryDocument
import com.sanden.ui.theme.CategoryImage
import com.sanden.ui.theme.CategoryOther
import com.sanden.ui.theme.CategoryVideo
import kotlin.math.log10
import kotlin.math.pow

@Composable
fun CategoryCard(
    category: FileCategory,
    count: Int,
    totalSizeBytes: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val (icon, accentColor, title) = when (category) {
        FileCategory.IMAGE -> Triple(Icons.Rounded.Image, CategoryImage, "Images")
        FileCategory.AUDIO -> Triple(Icons.Rounded.Audiotrack, CategoryAudio, "Audio")
        FileCategory.VIDEO -> Triple(Icons.Rounded.Videocam, CategoryVideo, "Videos")
        FileCategory.DOCUMENT -> Triple(Icons.Rounded.Description, CategoryDocument, "Documents")
        FileCategory.OTHER -> Triple(Icons.Rounded.FolderZip, CategoryOther, "Others")
    }

    LiquidGlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = if (isDark) 0.16f else 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(0f, 0f),
                        radius = 420f
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
                            .background(accentColor.copy(alpha = if (isDark) 0.22f else 0.16f))
                            .border(
                                width = 1.dp,
                                color = accentColor.copy(alpha = 0.35f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            letterSpacing = (-0.5).sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = formatFileSize(totalSizeBytes),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
    val clampedGroup = digitGroups.coerceIn(0, units.size - 1)
    val value = bytes / 1024.0.pow(clampedGroup.toDouble())
    return "%.1f %s".format(value, units[clampedGroup])
}
