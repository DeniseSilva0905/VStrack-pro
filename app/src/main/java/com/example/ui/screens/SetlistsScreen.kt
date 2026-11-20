package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.model.SetlistItemEntity
import com.example.data.model.SongTitleFontSizeOption
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SetlistsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val setlists by viewModel.allSetlists.collectAsState()
    val activeSetlist by viewModel.activeSetlist.collectAsState()
    val liveItems by viewModel.liveItems.collectAsState()
    val songs by viewModel.allSongs.collectAsState()
    val playedItemIds by viewModel.playedItemIds.collectAsState()
    val playedSongIds by viewModel.playedSongIds.collectAsState()
    val isShowActive by viewModel.isShowActive.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val titleFontSizeSp = SongTitleFontSizeOption.fromLevel(userSettings.songTitleFontSize).sizeSp
    val scope = rememberCoroutineScope()

    var isCreateSetlistOpen by remember { mutableStateOf(false) }
    var isAddBlockOpen by remember { mutableStateOf(false) }
    var isAddPauseOpen by remember { mutableStateOf(false) }
    var isAddSongOpen by remember { mutableStateOf(false) }

    // Persistent viewport-level Drag-to-Reorder + Auto-Scroll state
    val listState = rememberLazyListState()
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var draggingItemSnapshot by remember { mutableStateOf<SetlistItemEntity?>(null) }
    var targetDropIndex by remember { mutableStateOf<Int?>(null) }
    var pointerYInViewport by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    val currentLiveItems by rememberUpdatedState(liveItems)
    val currentActiveSetlist by rememberUpdatedState(activeSetlist)

    fun updateDropTargetFromPointer(y: Float) {
        val items = currentLiveItems
        if (items.isEmpty()) return
        val visible = listState.layoutInfo.visibleItemsInfo
        if (visible.isEmpty()) return

        val firstVis = visible.first()
        val lastVis = visible.last()
        val computed = when {
            y <= firstVis.offset + firstVis.size / 2f -> firstVis.index
            y >= lastVis.offset + lastVis.size / 2f -> lastVis.index
            else -> {
                visible.minByOrNull { info ->
                    abs((info.offset + info.size / 2f) - y)
                }?.index ?: (targetDropIndex ?: 0)
            }
        }
        targetDropIndex = computed.coerceIn(0, items.lastIndex)
    }

    // Smooth continuous auto-scroll when dragging near top or bottom of song list
    LaunchedEffect(draggingIndex != null) {
        if (draggingIndex == null) return@LaunchedEffect
        val edgeZonePx = with(density) { 88.dp.toPx() }
        val maxScrollStepPx = with(density) { 18.dp.toPx() }
        while (draggingIndex != null) {
            val viewportHeight = listState.layoutInfo.viewportSize.height.toFloat()
            if (viewportHeight > 0f) {
                val y = pointerYInViewport
                val scrollDelta = when {
                    y < edgeZonePx -> {
                        val ratio = ((edgeZonePx - y) / edgeZonePx).coerceIn(0.15f, 1f)
                        -maxScrollStepPx * ratio
                    }
                    y > (viewportHeight - edgeZonePx) -> {
                        val ratio = ((y - (viewportHeight - edgeZonePx)) / edgeZonePx).coerceIn(0.15f, 1f)
                        maxScrollStepPx * ratio
                    }
                    else -> 0f
                }
                if (scrollDelta != 0f) {
                    val consumed = listState.scrollBy(scrollDelta)
                    if (consumed != 0f) {
                        updateDropTargetFromPointer(pointerYInViewport)
                    }
                }
            }
            delay(16L)
        }
    }

    LaunchedEffect(setlists, activeSetlist) {
        if (activeSetlist == null && setlists.isNotEmpty()) {
            viewModel.selectSetlist(setlists.first())
        }
    }

    // Calculate total duration in minutes/seconds
    val totalDurationSeconds = remember(liveItems) {
        liveItems.sumOf { it.durationSeconds }
    }
    val totalDurationFormatted = remember(totalDurationSeconds) {
        val mins = totalDurationSeconds / 60
        val secs = totalDurationSeconds % 60
        "${mins}m ${secs}s"
    }

    val playbackState by viewModel.audioEngine.playbackState.collectAsState()
    val isPlaying = playbackState == com.example.audio.PlaybackState.PLAYING

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StageGraphite)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // FIXED Header & Active Repertoire Banner & Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Top Header: Slim & Grouped with "Trocar de Repertório" button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Repertórios",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SurfaceCardElevated
                    ) {
                        Text(
                            text = "${setlists.size}",
                            color = StageYellow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.navigateTo(AppScreen.SAVED_SETLISTS) },
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("switch_setlist_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StageYellow),
                        border = BorderStroke(1.dp, StageYellow)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = StageYellow,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Trocar de Repertório",
                            color = StageYellow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = (-0.3).sp
                        )
                    }

                    Button(
                        onClick = { isCreateSetlistOpen = true },
                        colors = ButtonDefaults.buttonColors(containerColor = StageYellow, contentColor = PitchBlack),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = PitchBlack, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Novo", color = PitchBlack, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = (-0.3).sp)
                    }

                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.TRASH) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Lixeira", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Active Repertoire Selector Banner: Slim & Compact
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceCardElevated,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (isShowActive) LilacPlayedBorder else SurfaceCardBorder)
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isShowActive) "SHOW AO VIVO EM ANDAMENTO" else "REPERTÓRIO SELECIONADO",
                                color = if (isShowActive) LilacPlayed else StageYellow,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.2).sp
                            )
                            Text(
                                text = activeSetlist?.name ?: "Nenhum selecionado",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (!isShowActive) {
                            Button(
                                onClick = {
                                    activeSetlist?.let { viewModel.startLiveShow(it, navigateToStage = true) }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = StageGreen, contentColor = PitchBlack),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp),
                                enabled = activeSetlist != null
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = PitchBlack, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Iniciar Show", color = PitchBlack, fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(
                                    onClick = {
                                        if (isPlaying) viewModel.pauseShow() else viewModel.resumeShow()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = StageYellow, contentColor = PitchBlack),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(if (isPlaying) "Pausar" else "Retomar", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }

                                Button(
                                    onClick = { viewModel.stopAndFinishShow() },
                                    colors = ButtonDefaults.buttonColors(containerColor = PanicRed, contentColor = TextPrimary),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Encerrar", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Itens: ${liveItems.size} • Duração: $totalDurationFormatted",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            letterSpacing = (-0.2).sp
                        )

                        TextButton(
                            onClick = {
                                val id = activeSetlist?.id ?: return@TextButton
                                scope.launch {
                                    val newId = viewModel.repository.duplicateSetlist(id)
                                    val dup = viewModel.repository.getSetlistById(newId)
                                    dup?.let { viewModel.selectSetlist(it) }
                                    viewModel.showToast("Repertório duplicado com sucesso!")
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            modifier = Modifier.height(22.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp), tint = StageYellow)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Duplicar", color = StageYellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Add Items Actions Row: Slim & Compact (isolated outside scrollable song list)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { isAddSongOpen = true },
                    modifier = Modifier.weight(1f).height(30.dp),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = BorderStroke(1.dp, StageYellow)
                ) {
                    Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(13.dp), tint = StageYellow)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("+ Música", fontSize = 10.sp, color = StageYellow, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { isAddBlockOpen = true },
                    modifier = Modifier.weight(1f).height(30.dp),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = BorderStroke(1.dp, SurfaceCardBorder)
                ) {
                    Icon(imageVector = Icons.Default.ViewAgenda, contentDescription = null, modifier = Modifier.size(13.dp), tint = CyanAccent)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("+ Bloco", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val target = activeSetlist ?: setlists.firstOrNull()
                            val targetId = if (target != null) {
                                target.id
                            } else {
                                val newId = viewModel.repository.createSetlist("Meu Repertório")
                                val created = viewModel.repository.getSetlistById(newId)
                                if (created != null) viewModel.selectSetlist(created)
                                newId
                            }
                            viewModel.repository.addPause(targetId, "Intervalo", 0, "")
                            viewModel.showToast("Intervalo inserido em '${target?.name ?: "Meu Repertório"}'! O show pausará ao alcançá-lo.")
                        }
                    },
                    modifier = Modifier.weight(1f).height(30.dp),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.7f))
                ) {
                    Icon(imageVector = Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(13.dp), tint = WarningAmber)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("+ Intervalo", fontSize = 10.sp, color = WarningAmber, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // DEDICATED SCROLLABLE ACERVO DE MÚSICA WITH AUTO-SCROLLING DRAG & DROP
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(Unit) {
                    val handleZoneWidthPx = 42.dp.toPx()
                    val quickHandleSlopPx = 3.dp.toPx()

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        val itemsSnapshot = currentLiveItems
                        if (itemsSnapshot.isEmpty()) return@awaitEachGesture

                        val hitInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
                            down.position.y >= info.offset && down.position.y <= (info.offset + info.size)
                        } ?: return@awaitEachGesture

                        val startIndex = hitInfo.index
                        val itemToDrag = itemsSnapshot.getOrNull(startIndex) ?: return@awaitEachGesture
                        val touchSlop = viewConfiguration.touchSlop
                        val isInHandleZone = down.position.x <= handleZoneWidthPx

                        var dragActivated = false
                        var activePointerId = down.id
                        var currentY = down.position.y

                        if (isInHandleZone) {
                            // Drag handle column: intercept before LazyColumn so vertical drag immediately picks up item
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == activePointerId }
                                    ?: event.changes.firstOrNull()
                                    ?: break
                                activePointerId = change.id
                                if (!change.pressed) break
                                val dy = abs(change.position.y - down.position.y)
                                val dx = abs(change.position.x - down.position.x)
                                if (dy >= quickHandleSlopPx) {
                                    change.consume()
                                    currentY = change.position.y
                                    dragActivated = true
                                    break
                                }
                                if (dx > touchSlop * 2f) {
                                    break
                                }
                            }
                        } else {
                            // Anywhere else on song row: long-press activates drag while normal swipes scroll and taps click
                            val abortedBeforeLongPress = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == activePointerId }
                                        ?: return@withTimeoutOrNull true
                                    if (!change.pressed) return@withTimeoutOrNull true
                                    val dist = (change.position - down.position).getDistance()
                                    if (dist > touchSlop) return@withTimeoutOrNull true
                                    currentY = change.position.y
                                }
                                @Suppress("UNREACHABLE_CODE")
                                true
                            }
                            if (abortedBeforeLongPress == null) {
                                dragActivated = true
                            }
                        }

                        if (dragActivated) {
                            pointerYInViewport = currentY
                            draggingIndex = startIndex
                            draggingItemSnapshot = itemToDrag
                            targetDropIndex = startIndex

                            try {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == activePointerId }
                                        ?: event.changes.firstOrNull()
                                        ?: break
                                    activePointerId = change.id
                                    change.consume()
                                    if (!change.pressed) {
                                        break
                                    }
                                    pointerYInViewport = change.position.y
                                    updateDropTargetFromPointer(pointerYInViewport)
                                }

                                val from = draggingIndex
                                val to = targetDropIndex
                                val setlistId = currentActiveSetlist?.id
                                val movedTitle = draggingItemSnapshot?.customTitle ?: "Item"
                                draggingIndex = null
                                draggingItemSnapshot = null
                                targetDropIndex = null

                                if (from != null && to != null && from != to && setlistId != null) {
                                    scope.launch {
                                        viewModel.repository.moveItem(setlistId, from, to)
                                        viewModel.showToast("'$movedTitle' reposicionado na posição ${to + 1}!")
                                    }
                                }
                            } finally {
                                draggingIndex = null
                                draggingItemSnapshot = null
                                targetDropIndex = null
                            }
                        }
                    }
                }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = PaddingValues(bottom = 76.dp)
            ) {
                if (liveItems.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            color = SurfaceCard,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(imageVector = Icons.Default.PlaylistRemove, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Este repertório ainda está vazio", color = TextSecondary, fontWeight = FontWeight.Bold)
                                Text("Use os botões acima para adicionar músicas, blocos ou intervalos", color = TextMuted, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    itemsIndexed(liveItems, key = { _, item -> item.id }) { index, item ->
                        val isPlayed = isShowActive && (playedItemIds.contains(item.id) || (item.songId != null && playedSongIds.contains(item.songId)))
                        val isThisItemBeingDragged = draggingIndex == index

                        // Drop target slot indicator BEFORE item when moving upwards
                        if (draggingIndex != null && targetDropIndex == index && targetDropIndex!! < draggingIndex!!) {
                            DropTargetSlotIndicator(targetPosition = index)
                        }

                        SetlistItemRow(
                            item = item,
                            index = index,
                            totalItems = liveItems.size,
                            titleFontSizeSp = titleFontSizeSp,
                            isPlayed = isPlayed,
                            isDragging = isThisItemBeingDragged,
                            onPlay = { viewModel.requestPlayLiveItemAtIndex(index) },
                            onMoveUp = {
                                val setlistId = activeSetlist?.id ?: return@SetlistItemRow
                                scope.launch { viewModel.repository.moveItem(setlistId, index, index - 1) }
                            },
                            onMoveDown = {
                                val setlistId = activeSetlist?.id ?: return@SetlistItemRow
                                scope.launch { viewModel.repository.moveItem(setlistId, index, index + 1) }
                            },
                            onDelete = {
                                viewModel.moveItemToTrash(item)
                            }
                        )

                        // Drop target slot indicator AFTER item when moving downwards
                        if (draggingIndex != null && targetDropIndex == index && targetDropIndex!! > draggingIndex!!) {
                            DropTargetSlotIndicator(targetPosition = index)
                        }
                    }
                }
            }

            // Floating PowerPoint-style Ghost Card following finger while scrolling the entire repertoire
            val activeDragItem = draggingItemSnapshot
            if (draggingIndex != null && activeDragItem != null) {
                val ghostHeightPx = with(density) { 46.dp.toPx() }
                val viewportHeightPx = listState.layoutInfo.viewportSize.height.toFloat().coerceAtLeast(ghostHeightPx)
                val clampedGhostY = (pointerYInViewport - ghostHeightPx / 2f)
                    .coerceIn(0f, (viewportHeightPx - ghostHeightPx).coerceAtLeast(0f))

                GhostDraggingItemCard(
                    item = activeDragItem,
                    targetPosition = (targetDropIndex ?: draggingIndex ?: 0) + 1,
                    offsetYPx = clampedGhostY,
                    titleFontSizeSp = titleFontSizeSp
                )
            }
        }
    }

    // Modal: Create Setlist
    if (isCreateSetlistOpen) {
        var newName by remember { mutableStateOf("") }
        var newDesc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { isCreateSetlistOpen = false },
            title = { Text("Criar Novo Repertório", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nome do Repertório (ex: Show Arena 2026)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newDesc,
                        onValueChange = { newDesc = it },
                        label = { Text("Descrição / Turnê") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            scope.launch {
                                val id = viewModel.repository.createSetlist(newName, newDesc)
                                val created = viewModel.repository.getSetlistById(id)
                                created?.let { viewModel.selectSetlist(it) }
                                viewModel.showToast("Repertório '$newName' criado! Selecione as músicas abaixo.")
                                isAddSongOpen = true
                            }
                            isCreateSetlistOpen = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StageYellow, contentColor = PitchBlack)
                ) {
                    Text("Criar", color = PitchBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isCreateSetlistOpen = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = SurfaceCardElevated
        )
    }

    // Modal: Add Block Divider (isolated per active repertoire)
    if (isAddBlockOpen) {
        val nextBlockNumber = remember(activeSetlist?.id, liveItems) {
            liveItems.count { it.type == "BLOCK" } + 1
        }
        var blockTitle by remember(activeSetlist?.id) { mutableStateOf("BLOCO $nextBlockNumber: ") }
        val presets = listOf("ABERTURA", "POP ROCK", "SERTANEJO", "ANOS 80", "ADORAÇÃO", "ENCERRAMENTO")

        AlertDialog(
            onDismissRequest = { isAddBlockOpen = false },
            title = {
                Column {
                    Text("Inserir Divisor de Bloco", color = TextPrimary)
                    activeSetlist?.let {
                        Text(
                            text = "Repertório: ${it.name}",
                            color = StageYellow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Defina o nome do bloco musical:", color = TextSecondary)
                    OutlinedTextField(
                        value = blockTitle,
                        onValueChange = { blockTitle = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Sugestões rápidas:", color = TextMuted, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        presets.take(3).forEach { p ->
                            SuggestionChip(
                                onClick = { blockTitle = "BLOCO $nextBlockNumber: $p" },
                                label = { Text(p, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val target = activeSetlist ?: setlists.firstOrNull()
                            val targetId = if (target != null) {
                                target.id
                            } else {
                                val newId = viewModel.repository.createSetlist("Meu Repertório")
                                val created = viewModel.repository.getSetlistById(newId)
                                if (created != null) viewModel.selectSetlist(created)
                                newId
                            }
                            viewModel.repository.addBlockDivider(targetId, blockTitle)
                            viewModel.showToast("Bloco adicionado em '${target?.name ?: "Meu Repertório"}'!")
                        }
                        isAddBlockOpen = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StageYellow, contentColor = PitchBlack)
                ) {
                    Text("Adicionar", color = PitchBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddBlockOpen = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = SurfaceCardElevated
        )
    }

    // Modal: Add Pause
    if (isAddPauseOpen) {
        var pauseTitle by remember { mutableStateOf("INTERVALO - 15 MINUTOS") }
        var pauseMins by remember { mutableStateOf("15") }
        var pauseNotes by remember { mutableStateOf("Músicos em standby, manter iluminação ambiente") }

        AlertDialog(
            onDismissRequest = { isAddPauseOpen = false },
            title = { Text("Inserir Pausa / Intervalo", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pauseTitle,
                        onValueChange = { pauseTitle = it },
                        label = { Text("Identificação (ex: TROCA DE FIGURINO, FALA)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pauseMins,
                        onValueChange = { pauseMins = it },
                        label = { Text("Duração estimada em minutos") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pauseNotes,
                        onValueChange = { pauseNotes = it },
                        label = { Text("Observações") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mins = pauseMins.toIntOrNull() ?: 5
                        scope.launch {
                            val target = activeSetlist ?: setlists.firstOrNull()
                            val targetId = if (target != null) {
                                target.id
                            } else {
                                val newId = viewModel.repository.createSetlist("Meu Repertório")
                                val created = viewModel.repository.getSetlistById(newId)
                                if (created != null) viewModel.selectSetlist(created)
                                newId
                            }
                            viewModel.repository.addPause(targetId, pauseTitle, mins, pauseNotes)
                            viewModel.showToast("Pausa adicionada ao repertório!")
                        }
                        isAddPauseOpen = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarningAmber)
                ) {
                    Text("Adicionar Pausa", color = PitchBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddPauseOpen = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = SurfaceCardElevated
        )
    }

    // Modal: Add Songs to Repertoire (Multi-selection with OK button)
    if (isAddSongOpen) {
        var songSearchQuery by remember { mutableStateOf("") }
        val selectedSongIds = remember { mutableStateListOf<Long>() }
        val candidateSongs = remember(songs, songSearchQuery) {
            if (songSearchQuery.isBlank()) songs
            else songs.filter {
                it.title.contains(songSearchQuery, ignoreCase = true) ||
                it.artist.contains(songSearchQuery, ignoreCase = true)
            }
        }
        val favoriteSongs = remember(candidateSongs) {
            candidateSongs.filter { it.isFavorite }.sortedBy { it.title.lowercase() }
        }
        val otherSongs = remember(candidateSongs) {
            candidateSongs.filter { !it.isFavorite }.sortedBy { it.title.lowercase() }
        }
        val targetSetlistName = (activeSetlist ?: setlists.firstOrNull())?.name ?: "Repertório Principal"
        val songsById = remember(songs) { songs.associateBy { it.id } }

        AlertDialog(
            onDismissRequest = { isAddSongOpen = false },
            title = {
                Column {
                    Text("Adicionar Músicas ao Repertório", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(
                        text = "Destino: $targetSetlistName • ${selectedSongIds.size} selecionada(s) (ordem de seleção)",
                        color = StageYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (songs.isNotEmpty()) {
                        OutlinedTextField(
                            value = songSearchQuery,
                            onValueChange = { songSearchQuery = it },
                            placeholder = { Text("Buscar música...", color = TextMuted, fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                if (songSearchQuery.isNotBlank()) {
                                    IconButton(onClick = { songSearchQuery = "" }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Limpar", tint = TextMuted)
                                    }
                                }
                            }
                        )

                        // Select All / Clear Selection Helper
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedSongIds.size} de ${songs.size} selecionadas",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Row {
                                TextButton(
                                    onClick = {
                                        (favoriteSongs + otherSongs).forEach {
                                            if (!selectedSongIds.contains(it.id)) selectedSongIds.add(it.id)
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("Marcar Todas", color = StageYellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                if (selectedSongIds.isNotEmpty()) {
                                    TextButton(
                                        onClick = { selectedSongIds.clear() },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("Limpar", color = PanicRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (selectedSongIds.isNotEmpty()) {
                            val sequencePreview = selectedSongIds.mapIndexedNotNull { idx, id ->
                                songsById[id]?.let { "${idx + 1}. ${it.title}" }
                            }.joinToString(" → ")
                            Surface(
                                color = SurfaceCard,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, StageYellow.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Ordem: $sequencePreview",
                                    color = StageYellowLight,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (songs.isEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            color = SurfaceCard,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(imageVector = Icons.Default.LibraryMusic, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Nenhuma música cadastrada na biblioteca", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Acesse a Biblioteca para cadastrar ou importar arquivos de VS.", color = TextSecondary, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        isAddSongOpen = false
                                        viewModel.navigateTo(AppScreen.LIBRARY)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = StageYellow, contentColor = PitchBlack)
                                ) {
                                    Text("Ir para a Biblioteca", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    } else if (candidateSongs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            Text("Nenhuma música corresponde a '$songSearchQuery'", color = TextSecondary, fontSize = 12.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // 1. Favorites section (alphabetical)
                            if (favoriteSongs.isNotEmpty()) {
                                item {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = StageYellow,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "FAVORITAS (${favoriteSongs.size})",
                                            color = StageYellow,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }

                                itemsIndexed(favoriteSongs) { _, song ->
                                    val selectionPos = selectedSongIds.indexOf(song.id)
                                    val isSelected = selectionPos >= 0
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (isSelected) selectedSongIds.remove(song.id)
                                                else selectedSongIds.add(song.id)
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) SurfaceCardElevated else SurfaceCard,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) StageYellow else SurfaceCardBorder
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 7.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = {
                                                    if (isSelected) selectedSongIds.remove(song.id)
                                                    else selectedSongIds.add(song.id)
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = StageYellow,
                                                    checkmarkColor = PitchBlack,
                                                    uncheckedColor = TextMuted
                                                )
                                            )

                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = "Favorita",
                                                tint = StageYellow,
                                                modifier = Modifier.size(16.dp)
                                            )

                                            Spacer(modifier = Modifier.width(6.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = song.title,
                                                    color = if (isSelected) StageYellow else TextPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${song.artist.ifBlank { "Artista não informado" }} • ${if (song.audioFileName.isNotBlank()) "VS Pronto" else "Sem áudio"}",
                                                    color = TextSecondary,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            if (isSelected) {
                                                Surface(
                                                    color = StageYellow,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "#${selectionPos + 1}",
                                                        color = PitchBlack,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Black,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. Lilac divider separating favorites from the rest of the songs
                            if (favoriteSongs.isNotEmpty() && otherSongs.isNotEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                    ) {
                                        HorizontalDivider(thickness = 2.dp, color = LilacPlayed)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.LibraryMusic,
                                                contentDescription = null,
                                                tint = LilacPlayed,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "DEMAIS MÚSICAS DA BIBLIOTECA (A-Z) (${otherSongs.size})",
                                                color = LilacPlayed,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                }
                            }

                            // 3. Other songs section (alphabetical)
                            if (otherSongs.isNotEmpty()) {
                                if (favoriteSongs.isEmpty()) {
                                    item {
                                        Text(
                                            text = "MÚSICAS DA BIBLIOTECA (A-Z) (${otherSongs.size})",
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }

                                itemsIndexed(otherSongs) { _, song ->
                                    val selectionPos = selectedSongIds.indexOf(song.id)
                                    val isSelected = selectionPos >= 0
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (isSelected) selectedSongIds.remove(song.id)
                                                else selectedSongIds.add(song.id)
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) SurfaceCardElevated else SurfaceCard,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) StageYellow else SurfaceCardBorder
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 7.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = {
                                                    if (isSelected) selectedSongIds.remove(song.id)
                                                    else selectedSongIds.add(song.id)
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = StageYellow,
                                                    checkmarkColor = PitchBlack,
                                                    uncheckedColor = TextMuted
                                                )
                                            )

                                            Icon(
                                                imageVector = Icons.Default.StarBorder,
                                                contentDescription = null,
                                                tint = TextMuted.copy(alpha = 0.5f),
                                                modifier = Modifier.size(16.dp)
                                            )

                                            Spacer(modifier = Modifier.width(6.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = song.title,
                                                    color = if (isSelected) StageYellow else TextPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${song.artist.ifBlank { "Artista não informado" }} • ${if (song.audioFileName.isNotBlank()) "VS Pronto" else "Sem áudio"}",
                                                    color = TextSecondary,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            if (isSelected) {
                                                Surface(
                                                    color = StageYellow,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "#${selectionPos + 1}",
                                                        color = PitchBlack,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Black,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedSongIds.isNotEmpty()) {
                            scope.launch {
                                val target = activeSetlist ?: setlists.firstOrNull()
                                val targetId = if (target != null) {
                                    target.id
                                } else {
                                    val newId = viewModel.repository.createSetlist("Meu Repertório")
                                    val created = viewModel.repository.getSetlistById(newId)
                                    if (created != null) viewModel.selectSetlist(created)
                                    newId
                                }
                                // Preserve exact selection order when adding songs to repertoire
                                val toAddInOrder = selectedSongIds.mapNotNull { songsById[it] }
                                viewModel.repository.addSongsToSetlist(targetId, toAddInOrder)
                                viewModel.showToast("${toAddInOrder.size} música(s) adicionada(s) na sequência selecionada!")
                                isAddSongOpen = false
                                selectedSongIds.clear()
                            }
                        }
                    },
                    enabled = selectedSongIds.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StageYellow,
                        contentColor = PitchBlack,
                        disabledContainerColor = SurfaceCardBorder,
                        disabledContentColor = TextMuted
                    )
                ) {
                    Text(
                        text = if (selectedSongIds.isEmpty()) "OK" else "OK (${selectedSongIds.size})",
                        fontWeight = FontWeight.Black
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddSongOpen = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = SurfaceCardElevated
        )
    }
}

@Composable
fun GhostDraggingItemCard(
    item: SetlistItemEntity,
    targetPosition: Int,
    offsetYPx: Float,
    titleFontSizeSp: Int = 16
) {
    val isPause = item.type == "PAUSE"
    val isBlock = item.type == "BLOCK"
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(25f)
            .graphicsLayer {
                translationY = offsetYPx
                scaleX = 1.03f
                scaleY = 1.03f
                shadowElevation = 28f
                alpha = 0.96f
            },
        shape = RoundedCornerShape(10.dp),
        color = SurfaceCardElevated,
        border = BorderStroke(2.dp, StageYellow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DragIndicator,
                    contentDescription = null,
                    tint = StageYellow,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isPause) "Intervalo" else item.customTitle,
                    color = if (isBlock) CyanAccent else if (isPause) WarningAmber else TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = titleFontSizeSp.sp,
                    letterSpacing = (-0.3).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                color = StageYellow,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "MOVER P/ POSIÇÃO $targetPosition",
                    color = PitchBlack,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun DropTargetSlotIndicator(targetPosition: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = StageYellow.copy(alpha = 0.15f),
        border = BorderStroke(1.5.dp, StageYellow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.MoveToInbox,
                contentDescription = null,
                tint = StageYellow,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "SOLTAR AQUI NA POSIÇÃO ${targetPosition + 1}",
                color = StageYellow,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun SetlistItemRow(
    item: SetlistItemEntity,
    index: Int,
    totalItems: Int,
    titleFontSizeSp: Int = 16,
    isPlayed: Boolean = false,
    isDragging: Boolean = false,
    onPlay: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    val isBlock = item.type == "BLOCK"
    val isPause = item.type == "PAUSE"

    val containerColor = when {
        isDragging -> SurfaceCardElevated
        isPlayed -> LilacPlayedBg
        isBlock -> SurfaceCardElevated
        isPause -> WarningAmber.copy(alpha = 0.15f)
        else -> SurfaceCard
    }
    val borderColor = when {
        isDragging -> StageYellow
        isPlayed -> LilacPlayedBorder
        isBlock -> SurfaceCardBorder
        isPause -> WarningAmber
        else -> SurfaceCardBorder
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = if (isDragging) 0.35f else 1f
            },
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = BorderStroke(if (isDragging || isPlayed) 1.5.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Dedicated Drag Handle visual zone (intercepted at viewport level for smooth auto-scroll)
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Arrastar para reordenar com o dedo",
                    tint = if (isDragging) StageYellowLight else StageYellow,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(3.dp))

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play Button or Number Badge
                if (!isBlock && !isPause) {
                    IconButton(
                        onClick = onPlay,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Tocar",
                            tint = if (isPlayed) LilacPlayedGlow else StageYellow,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isBlock) SurfaceCardBorder else WarningAmber
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = if (isPause) PitchBlack else TextPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isPause) "Intervalo" else item.customTitle,
                            color = if (isPlayed) LilacPlayedGlow else if (isBlock) CyanAccent else if (isPause) WarningAmber else TextPrimary,
                            fontWeight = if (isBlock) FontWeight.ExtraBold else FontWeight.SemiBold,
                            fontSize = titleFontSizeSp.sp,
                            letterSpacing = (-0.4).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isDragging) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                color = StageYellow,
                                shape = RoundedCornerShape(3.dp)
                            ) {
                                Text(
                                    text = "PEGANDO",
                                    color = PitchBlack,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                )
                            }
                        } else if (isPlayed) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "(TOCADA)",
                                color = LilacPlayed,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.3).sp
                            )
                        }
                    }
                    if (isPause) {
                        Text(text = "Intervalo • Pausa automática no show", color = WarningAmber, fontSize = 10.sp, letterSpacing = (-0.3).sp, fontWeight = FontWeight.Medium)
                    } else if (!isBlock && item.durationSeconds > 0) {
                        val mins = item.durationSeconds / 60
                        val secs = item.durationSeconds % 60
                        Text(text = "Duração: ${mins}m ${secs}s", color = TextSecondary, fontSize = 10.sp, letterSpacing = (-0.3).sp)
                    }
                }
            }

            // Ordering and Delete Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = index > 0,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "Subir", tint = if (index > 0) TextPrimary else TextMuted, modifier = Modifier.size(15.dp))
                }

                IconButton(
                    onClick = onMoveDown,
                    enabled = index < totalItems - 1,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "Descer", tint = if (index < totalItems - 1) TextPrimary else TextMuted, modifier = Modifier.size(15.dp))
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Mover para Lixeira", tint = PanicRed.copy(alpha = 0.8f), modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}
