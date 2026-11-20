package com.sanden.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sanden.domain.model.FileCategory
import com.sanden.ui.theme.CategoryAudio
import com.sanden.ui.theme.CategoryDocument
import com.sanden.ui.theme.CategoryImage
import com.sanden.ui.theme.CategoryOther
import com.sanden.ui.theme.CategoryVideo
import com.sanden.ui.theme.GlassBorderBottomDark
import com.sanden.ui.theme.GlassBorderBottomLight
import com.sanden.ui.theme.GlassBorderTopDark
import com.sanden.ui.theme.GlassBorderTopLight
import com.sanden.ui.theme.GlassSurfaceDark
import com.sanden.ui.theme.GlassSurfaceLight

/**
 * Ambient aurora mesh background inspired by Android 16 / ColorOS 16 / Apple Liquid Glass.
 * Provides subtle depth behind translucent glass surfaces.
 */
@Composable
fun AuroraGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val baseColor = MaterialTheme.colorScheme.background
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Top-left luminous orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primary.copy(alpha = if (isDark) 0.22f else 0.16f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.15f, h * 0.12f),
                    radius = w * 0.85f
                ),
                center = Offset(w * 0.15f, h * 0.12f),
                radius = w * 0.85f
            )

            // Mid-right cyan/teal aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        secondary.copy(alpha = if (isDark) 0.16f else 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.9f, h * 0.45f),
                    radius = w * 0.75f
                ),
                center = Offset(w * 0.9f, h * 0.45f),
                radius = w * 0.75f
            )

            // Bottom-left warm/violet glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        tertiary.copy(alpha = if (isDark) 0.12f else 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.2f, h * 0.88f),
                    radius = w * 0.7f
                ),
                center = Offset(w * 0.2f, h * 0.88f),
                radius = w * 0.7f
            )
        }

        content()
    }
}

/**
 * Translucent Liquid Glass card with specular top-edge highlight border,
 * spring-damped press physics, and subtle haptic feedback.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    containerColor: Color? = null,
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.965f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "liquidGlassScale"
    )

    val bgColor = containerColor ?: if (isDark) GlassSurfaceDark else GlassSurfaceLight
    val borderBrush = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(GlassBorderTopDark, GlassBorderBottomDark)
        } else {
            listOf(GlassBorderTopLight, GlassBorderBottomLight)
        }
    )

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(bgColor)
            .border(width = borderWidth, brush = borderBrush, shape = shape)
            .then(clickableModifier)
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            content()
        }
    }
}

/**
 * Frosted glass capsule pill for status indicators and counts.
 */
@Composable
fun LiquidGlassPill(
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(accentColor.copy(alpha = if (isDark) 0.18f else 0.12f))
            .border(
                width = 0.8.dp,
                color = accentColor.copy(alpha = if (isDark) 0.35f else 0.25f),
                shape = CircleShape
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            content()
        }
    }
}

/**
 * Segmented storage breakdown bar across the 5 vault categories.
 */
@Composable
fun StorageBreakdownBar(
    categoryStats: Map<FileCategory, Pair<Int, Long>>,
    modifier: Modifier = Modifier
) {
    val totalBytes = categoryStats.values.sumOf { it.second }.coerceAtLeast(1L)
    val segments = listOf(
        FileCategory.IMAGE to CategoryImage,
        FileCategory.AUDIO to CategoryAudio,
        FileCategory.VIDEO to CategoryVideo,
        FileCategory.DOCUMENT to CategoryDocument,
        FileCategory.OTHER to CategoryOther
    ).mapNotNull { (cat, color) ->
        val bytes = categoryStats[cat]?.second ?: 0L
        if (bytes > 0L) color to (bytes.toFloat() / totalBytes.toFloat()).coerceAtLeast(0.04f) else null
    }

    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(trackColor),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for ((color, weight) in segments) {
            Box(
                modifier = Modifier
                    .weight(weight)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
