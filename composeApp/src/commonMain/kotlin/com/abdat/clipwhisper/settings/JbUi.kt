package com.abdat.clipwhisper.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

object JbBrand {
    val Purple = Color(0xFF7F52FF)
    val Pink   = Color(0xFFFF4DD8)
    val Cyan   = Color(0xFF3DCEFF)
    val Lime   = Color(0xFF00E676)

    fun heroBrush(): Brush = Brush.linearGradient(
        listOf(Purple, Pink, Cyan, Lime)
    )

    fun strokeBrush(alpha: Float = 0.55f): Brush = Brush.linearGradient(
        listOf(
            Pink.copy(alpha = alpha),
            Cyan.copy(alpha = alpha),
            Purple.copy(alpha = alpha)
        )
    )

    fun subtleBgBrush(surface: Color): Brush = Brush.verticalGradient(
        listOf(
            surface,
            surface.copy(alpha = 0.98f),
            surface.copy(alpha = 0.94f)
        )
    )
}



@Composable
fun JbScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val surface = MaterialTheme.colorScheme.surface
    val bg = remember(surface) { JbBrand.subtleBgBrush(surface) }

    Box(
        modifier
            .fillMaxSize()
            .background(bg),
        content = content
    )
}

@Composable
fun JbSectionCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    val stroke = remember { JbBrand.strokeBrush(alpha = 0.45f) }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, stroke, shape),
        shape = shape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                JbIconBadge(icon)
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            content()
        }
    }
}

@Composable
fun JbIconBadge(icon: ImageVector, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    val brush = remember { JbBrand.strokeBrush(alpha = 0.35f) }

    Surface(
        modifier = modifier.size(36.dp),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .border(1.dp, brush, shape)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun JbPreferenceRow(
    title: String,
    subtitle: String? = null,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                AnimatedVisibility(visible = subtitle != null) {
                    if (subtitle != null) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            trailing()
        }
    }
}

@Composable
fun JbColorDot(brush: Brush?, fallback: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(brush ?: Brush.linearGradient(listOf(fallback, fallback)))
            .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), CircleShape)
    )
}
