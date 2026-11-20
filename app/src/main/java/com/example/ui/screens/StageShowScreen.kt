package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.PlaybackState
import com.example.data.model.SetlistItemEntity
import com.example.data.model.SongTitleFontSizeOption
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@Composable
fun StageShowScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val activeSetlist by viewModel.activeSetlist.collectAsState()
    val liveItems by viewModel.liveItems.collectAsState()
    val currentIndex by viewModel.currentLiveItemIndex.collectAsState()
    val playedItemIds by viewModel.playedItemIds.collectAsState()
    val playedSongIds by viewModel.playedSongIds.collectAsState()
    val playbackState by viewModel.audioEngine.playbackState.collectAsState()
    val currentPositionMs by viewModel.audioEngine.currentPositionMs.collectAsState()
    val durationMs by viewModel.audioEngine.durationMs.collectAsState()
    val activeSong by viewModel.audioEngine.activeSong.collectAsState()
    val isShowActive by viewModel.isShowActive.collectAsState()
    val isShowPausedAtCue by viewModel.isShowPausedAtCue.collectAsState()
    val isPausedAfterLibrarySong by viewModel.isPausedAfterLibrarySong.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val countdownSec by viewModel.nextSongCountdownSeconds.collectAsState()
    val titleFontSizeSp = SongTitleFontSizeOption.fromLevel(userSettings.songTitleFontSize).sizeSp

    val isPlaying = playbackState == PlaybackState.PLAYING
    val isPaused = playbackState == PlaybackState.PAUSED
    val showPaused = isPaused || isShowPausedAtCue
    val progress = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    val currentItem: SetlistItemEntity? = liveItems.getOrNull(currentIndex)
    val listState = rememberLazyListState()

    // Formatted times
    val elapsedMin = (currentPositionMs / 1000) / 60
    val elapsedSec = (currentPositionMs / 1000) % 60
    val remainingMs = (durationMs - currentPositionMs).coerceAtLeast(0L)
    val remMin = (remainingMs / 1000) / 60
    val remSec = (remainingMs / 1000) % 60

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PitchBlack)
            .padding(horizontal = 10.dp, vertical = 2.dp)
    ) {
        // TOP STAGE CONTROL BAR: Ultra-compact, slim, matching reference design
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = SurfaceCardElevated,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                when {
                    isPlaying -> StageYellow
                    isShowPausedAtCue -> WarningAmber
                    else -> SurfaceCardBorder
                }
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                // Row 1: Status & Setlist Title on left, Red Panic Square on right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isPlaying -> StageGreen
                                            showPaused -> WarningAmber
                                            else -> TextMuted
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    isPlaying -> "SHOW EM ANDAMENTO • TOCANDO"
                                    isPausedAfterLibrarySong -> "SHOW PAUSADO • MÚSICA DA BIBLIOTECA"
                                    isShowPausedAtCue && currentItem?.type == "BLOCK" -> "SHOW PAUSADO NO BLOCO • RETOMAR MANUAL"
                                    isShowPausedAtCue && currentItem?.type == "PAUSE" -> "SHOW PAUSADO NO INTERVALO • RETOMAR MANUAL"
                                    showPaused -> "SHOW PAUSADO • STANDBY"
                                    else -> "SHOW EM STANDBY"
                                },
                                color = when {
                                    isPlaying -> StageGreen
                                    showPaused -> WarningAmber
                                    else -> TextSecondary
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Text(
                            text = activeSetlist?.name ?: "Repertório ao Vivo",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // PANIC BUTTON (Compact Red Square)
                    IconButton(
                        onClick = { viewModel.triggerPanic() },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(PanicRed)
                            .testTag("stage_panic_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Pânico",
                            tint = TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Single Row: All 4 buttons (CONTÍNUO, UMA A UMA, RETOMAR/PAUSAR, ENCERRAR) on the same line!
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Contínuo
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .clickable { viewModel.updateContinuousMode(true) },
                        shape = RoundedCornerShape(4.dp),
                        color = if (userSettings.isContinuousMode) StageYellow else SurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (userSettings.isContinuousMode) StageYellow else SurfaceCardBorder
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "CONTÍNUO",
                                color = if (userSettings.isContinuousMode) PitchBlack else TextPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.4).sp
                            )
                        }
                    }

                    // 2. Uma a uma
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .clickable { viewModel.updateContinuousMode(false) },
                        shape = RoundedCornerShape(4.dp),
                        color = if (!userSettings.isContinuousMode) StageYellow else SurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (!userSettings.isContinuousMode) StageYellow else SurfaceCardBorder
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "UMA A UMA",
                                color = if (!userSettings.isContinuousMode) PitchBlack else TextPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.4).sp
                            )
                        }
                    }

                    // 3. Pausar / Retomar Show
                    Button(
                        onClick = {
                            if (isPlaying) {
                                viewModel.pauseShow()
                            } else {
                                viewModel.resumeShow()
                            }
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(28.dp)
                            .testTag("stage_pause_resume_show_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPlaying) WarningAmber else StageGreen,
                            contentColor = PitchBlack
                        ),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = if (isPlaying) "PAUSAR SHOW" else "RETOMAR SHOW",
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            letterSpacing = (-0.4).sp
                        )
                    }

                    // 4. Encerrar Show
                    OutlinedButton(
                        onClick = { viewModel.stopAndFinishShow() },
                        modifier = Modifier
                            .weight(1.1f)
                            .height(28.dp)
                            .testTag("stage_finish_show_button"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PanicRed
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PanicRed.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "ENCERRAR",
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            color = PanicRed,
                            letterSpacing = (-0.4).sp
                        )
                    }
                }

                // If continuous, show compact 1-line interval stepper
                if (userSettings.isContinuousMode) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceCard, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Espera entre faixas: ${userSettings.intervalSeconds}s",
                            color = TextSecondary,
                            fontSize = 9.sp,
                            letterSpacing = (-0.3).sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.updateIntervalSeconds(userSettings.intervalSeconds - 1) },
                                enabled = userSettings.intervalSeconds > 1,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = null, tint = if (userSettings.intervalSeconds > 1) StageYellow else TextMuted, modifier = Modifier.size(11.dp))
                            }
                            Text("${userSettings.intervalSeconds}s", color = StageYellow, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            IconButton(
                                onClick = { viewModel.updateIntervalSeconds(userSettings.intervalSeconds + 1) },
                                enabled = userSettings.intervalSeconds < 10,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = if (userSettings.intervalSeconds < 10) StageYellow else TextMuted, modifier = Modifier.size(11.dp))
                            }
                        }
                    }
                }

                // Transition countdown banner (if active)
                AnimatedVisibility(visible = countdownSec != null) {
                    countdownSec?.let { sec ->
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(4.dp),
                            color = StageYellow,
                            border = androidx.compose.foundation.BorderStroke(1.dp, PitchBlack)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Próxima em ${sec}s...", color = PitchBlack, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Button(
                                        onClick = { viewModel.skipCountdownAndPlayNext() },
                                        colors = ButtonDefaults.buttonColors(containerColor = PitchBlack, contentColor = StageYellow),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                        modifier = Modifier.height(22.dp),
                                        shape = RoundedCornerShape(3.dp)
                                    ) {
                                        Text("Tocar Já", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.cancelCountdown() },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PitchBlack),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, PitchBlack),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                        modifier = Modifier.height(22.dp),
                                        shape = RoundedCornerShape(3.dp)
                                    ) {
                                        Text("Pausar", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Section Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Músicas do Repertório",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )
            val totalSongsCount = liveItems.count { it.type == "SONG" }
            val playedSongsInSetlistCount = liveItems.count {
                it.type == "SONG" && (playedItemIds.contains(it.id) || (it.songId != null && playedSongIds.contains(it.songId)))
            }
            Text(
                text = "Tocadas: $playedSongsInSetlistCount/$totalSongsCount",
                color = LilacPlayedGlow,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Main List of Songs: Ultra-compact, reduced spacing, condensed font, inline (TOCADA), time at line end
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            itemsIndexed(liveItems) { index, item ->
                val isCurrent = index == currentIndex
                val isPlayed = playedItemIds.contains(item.id) || (item.songId != null && playedSongIds.contains(item.songId))
                val isBlock = item.type == "BLOCK"
                val isPause = item.type == "PAUSE"
                val isSong = item.type == "SONG"
                val isActivePausedCue = isCurrent && isShowPausedAtCue && (isBlock || isPause)

                val rowBackground = when {
                    isCurrent && isPlaying -> SurfaceCardElevated
                    isActivePausedCue -> WarningAmber.copy(alpha = 0.2f)
                    isPlayed -> LilacPlayedBg
                    isBlock -> SurfaceCardElevated
                    isPause -> WarningAmber.copy(alpha = 0.12f)
                    else -> SurfaceCard
                }

                val rowBorderColor = when {
                    isCurrent && isPlaying -> StageGreen
                    isActivePausedCue -> WarningAmber
                    isPlayed -> LilacPlayedBorder
                    isBlock -> CyanAccent.copy(alpha = 0.4f)
                    isPause -> WarningAmber.copy(alpha = 0.5f)
                    else -> SurfaceCardBorder
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isSong) {
                            if (isCurrent && isPlaying) {
                                viewModel.audioEngine.pause()
                            } else {
                                viewModel.requestPlayLiveItemAtIndex(index)
                            }
                        },
                    shape = RoundedCornerShape(6.dp),
                    color = rowBackground,
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isCurrent || isPlayed) 1.dp else 0.5.dp,
                        color = rowBorderColor
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Direct Play Button for each song row
                            if (isSong) {
                                FilledIconButton(
                                    onClick = {
                                        if (isCurrent && isPlaying) {
                                            viewModel.audioEngine.pause()
                                        } else {
                                            viewModel.requestPlayLiveItemAtIndex(index)
                                        }
                                    },
                                    modifier = Modifier.size(28.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = when {
                                            isCurrent && isPlaying -> StageGreen
                                            isPlayed -> LilacPlayed
                                            else -> StageYellow
                                        },
                                        contentColor = PitchBlack
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (isCurrent && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Tocar Faixa",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier.size(28.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isBlock) Icons.Default.Bookmark else Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = if (isBlock) CyanAccent else WarningAmber,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Song Name + inline (TOCADA) / (TOCANDO)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = item.customTitle,
                                    color = when {
                                        isPlayed -> LilacPlayedGlow
                                        isCurrent -> TextPrimary
                                        isBlock -> CyanAccent
                                        isPause -> WarningAmber
                                        else -> TextPrimary
                                    },
                                    fontWeight = if (isCurrent || isBlock) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = titleFontSizeSp.sp,
                                    letterSpacing = (-0.4).sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )

                                // Tarja inline: (TOCADA) ao lado do nome da música
                                if (isActivePausedCue) {
                                    Text(
                                        text = " (SHOW PAUSADO • RETOMAR)",
                                        color = WarningAmber,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        letterSpacing = (-0.3).sp,
                                        maxLines = 1
                                    )
                                } else if (isPlayed && isSong) {
                                    Text(
                                        text = " (TOCADA)",
                                        color = LilacPlayed,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        letterSpacing = (-0.3).sp,
                                        maxLines = 1
                                    )
                                } else if (isCurrent && isPlaying) {
                                    Text(
                                        text = " (TOCANDO)",
                                        color = StageGreen,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        letterSpacing = (-0.3).sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Right-aligned section: Duration + Order index badge flush to right margin
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (item.durationSeconds > 0) {
                                val mins = item.durationSeconds / 60
                                val secs = item.durationSeconds % 60
                                Text(
                                    text = String.format("%02d:%02d", mins, secs),
                                    color = if (isPlayed) LilacPlayedGlow.copy(alpha = 0.9f) else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            // Order index badge with uniform minWidth so durations align vertically
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = PitchBlack,
                                modifier = Modifier.defaultMinSize(minWidth = 22.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = if (isPlayed) LilacPlayedGlow else TextMuted,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
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
