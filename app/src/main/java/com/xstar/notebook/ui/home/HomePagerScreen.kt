@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.xstar.notebook.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import com.xstar.notebook.ui.browse.BrowseScreen
import com.xstar.notebook.ui.components.FileKind
import com.xstar.notebook.ui.todoHub.TodoHubScreen

@Composable
fun HomePagerScreen(
    repoId: Long,
    todoFirst: Boolean,
    drawerOpen: Boolean,
    onOpenDrawer: () -> Unit,
    onOpenNode: (FileKind, String) -> Unit,
    onOpenClassify: () -> Unit,
    onCreateTodo: () -> Unit,
    onEditTodo: (Long) -> Unit,
) {
    val pager = rememberPagerState(initialPage = 0, pageCount = { 2 })
    Box(
        Modifier.fillMaxSize().pointerInput(onOpenDrawer) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                if (down.position.x > 28.dp.toPx()) return@awaitEachGesture
                var dragged = 0f
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    dragged += change.positionChange().x
                    if (dragged > 40.dp.toPx()) {
                        change.consume()
                        onOpenDrawer()
                        break
                    }
                    if (dragged < -20.dp.toPx()) break
                }
            }
        },
    ) {
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize(),
            key = { it },
            userScrollEnabled = !drawerOpen,
        ) { page ->
            val showTodo = if (todoFirst) page == 0 else page == 1
            if (showTodo) {
                TodoHubScreen(
                    onOpenDrawer = onOpenDrawer,
                    onOpenClassify = onOpenClassify,
                    onCreateTodo = onCreateTodo,
                    onEditTodo = onEditTodo,
                )
            } else {
                BrowseScreen(
                    repoId = repoId,
                    onOpenDrawer = onOpenDrawer,
                    onOpenNode = onOpenNode,
                )
            }
        }
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 6.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f), CircleShape)
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(2) { page ->
                Box(
                    Modifier
                        .size(if (pager.currentPage == page) 8.dp else 6.dp)
                        .background(
                            if (pager.currentPage == page) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                            CircleShape,
                        ),
                )
            }
        }
    }
}
