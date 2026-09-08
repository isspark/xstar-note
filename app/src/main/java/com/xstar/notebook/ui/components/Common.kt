package com.xstar.notebook.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xstar.notebook.ui.theme.appGradientColors

/** 统一的实心主按钮：全主题色、大圆角、统一高度。 */
@Composable
fun FilledActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        content = content,
    )
}

/** 统一的描边次按钮：与主按钮同高度同圆角。 */
@Composable
fun OutlinedActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        content = content,
    )
}

/** 统一的圆形悬浮按钮，颜色取自当前主题配色。 */
@Composable
fun XStarFab(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        containerColor = containerColor,
        contentColor = contentColor,
    ) {
        Icon(icon, contentDescription)
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    hint: String,
    modifier: Modifier = Modifier,
) {
    val gradient = appGradientColors()
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(96.dp)
                .background(
                    brush = Brush.linearGradient(gradient),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 20.dp),
            textAlign = TextAlign.Center,
        )
        Text(
            hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 6.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onConfirm()
            }) {
                Text(confirmText, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        shape = MaterialTheme.shapes.large,
    )
}

@Composable
fun StatusChip(label: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.16f),
        shape = CircleShape,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

private val kindColors = mapOf(
    FileKind.MD to Color(0xFF6B4CE0),
    FileKind.DRAWIO to Color(0xFFFF6B6B),
    FileKind.IMAGE to Color(0xFF00A896),
    FileKind.CODE to Color(0xFF3E8ED0),
    FileKind.TEXT to Color(0xFF8A93A6),
    FileKind.OTHER to Color(0xFF8A93A6),
)

fun fileKindColor(name: String): Color = kindColors[FileTypes.kind(name)] ?: kindColors.getValue(FileKind.OTHER)

private fun kindIcon(isDirectory: Boolean, name: String): ImageVector = when {
    isDirectory -> Icons.Rounded.Folder
    else -> when (FileTypes.kind(name)) {
        FileKind.MD -> Icons.Rounded.Description
        FileKind.DRAWIO -> Icons.Rounded.AccountTree
        FileKind.IMAGE -> Icons.Rounded.Image
        FileKind.CODE -> Icons.Rounded.Code
        FileKind.TEXT -> Icons.Rounded.Notes
        FileKind.OTHER -> Icons.Rounded.InsertDriveFile
    }
}

@Composable
fun FileTypeIcon(isDirectory: Boolean, name: String, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    val color = if (isDirectory) Color(0xFFF2A33C) else fileKindColor(name)
    Box(
        modifier = modifier.size(size).background(color.copy(alpha = 0.16f), RoundedCornerShape(size / 3)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            kindIcon(isDirectory, name),
            contentDescription = name,
            tint = color,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}

private val hostColors = mapOf(
    "GITHUB" to Color(0xFF24292E),
    "GITEE" to Color(0xFFC71D23),
    "GITLAB" to Color(0xFFFC6D26),
)

private fun hostMark(hostType: String): String = when (hostType) {
    "GITHUB" -> "GH"
    "GITEE" -> "G"
    "GITLAB" -> "GL"
    else -> "R"
}

@Composable
fun HostAvatar(hostType: String, size: Dp, modifier: Modifier = Modifier) {
    val base = hostColors[hostType] ?: Color(0xFF6B4CE0)
    Box(
        modifier = modifier.size(size).background(
            brush = Brush.linearGradient(listOf(base, base.copy(alpha = 0.7f))),
            shape = RoundedCornerShape(size / 3),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.Person,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.35f),
            modifier = Modifier.size(size * 0.6f),
        )
        Text(
            hostMark(hostType),
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}
