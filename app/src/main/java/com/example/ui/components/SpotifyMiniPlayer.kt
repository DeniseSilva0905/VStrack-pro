package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.data.model.SongTitleFontSizeOption
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@Composable
fun SpotifyMiniPlayer(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val activeSong by viewModel.audioEngine.activeSong.collectAsState()
    val playbackState by viewModel.audioEngine.playbackState.collectAsState()
    val currentPositionMs by viewModel.audioEngine.currentPositionMs.collectAsState()
    val durationMs by viewModel.audioEngine.durationMs.collectAsState()
    val isShowActive by viewModel.isShowActive.collectAsState()
    val isShowPausedAtCue by viewModel.isShowPausedAtCue.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()

    if (activeSong == null) return

    val song = activeSong!!
    val isPlaying = playbackState == PlaybackState.PLAYING

    // Format mm:ss
    val curMins = (currentPositionMs / 1000) / 60
    val curSecs = (currentPositionMs / 1000) % 60
    val totalMins = (durationMs / 1000) / 60
    val totalSecs = (durationMs / 1000) % 60
    val timeLabel = String.format("%d:%02d / %d:%02d", curMins, curSecs, totalMins, totalSecs)

    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }

    val currentProgress = if (durationMs > 0) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val effectiveSliderValue = if (isDraggingSlider) sliderPosition else currentProgress

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("spotify_mini_player"),
        color = SurfaceCardElevated,
        tonalElevation = 12.dp,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            // Live Show Status Banner if active or paused awaiting manual resume
            if (isShowActive || isShowPausedAtCue) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isShowActive) LilacPlayed else WarningAmber
                        ) {
                            Text(
                                text = if (isShowActive) "SHOW AO VIVO" else "SHOW PAUSADO",
                                color = PitchBlack,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPlaying) "Reproduzindo no PA" else "Aguardando retomada",
                            color = if (isPlaying) StageGreen else WarningAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Pause / Resume Show button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PitchBlack,
                            border = androidx.compose.foundation.BorderStroke(1.dp, StageYellow),
                            modifier = Modifier.clickable {
                                if (isPlaying) viewModel.pauseShow() else viewModel.resumeShow()
                            }
                        ) {
                            Text(
                                text = if (isPlaying) "Pausar Show" else "Retomar Show",
                                color = StageYellow,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        if (isShowActive) {
                            // Finish Show button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PitchBlack,
                                border = androidx.compose.foundation.BorderStroke(1.dp, PanicRed),
                                modifier = Modifier.clickable { viewModel.stopAndFinishShow() }
                            ) {
                                Text(
                                    text = "Encerrar Show",
                                    color = PanicRed,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Audio Scrubber Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Slider(
                    value = effectiveSliderValue,
                    onValueChange = {
                        isDraggingSlider = true
                        sliderPosition = it
                    },
                    onValueChangeFinished = {
                        val targetMs = (sliderPosition * durationMs).toLong()
                        viewModel.seekTo(targetMs)
                        isDraggingSlider = false
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = StageYellow,
                        activeTrackColor = StageYellow,
                        inactiveTrackColor = SurfaceCardBorder
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = timeLabel,
                    color = CyanAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Controls & Song Info Row: Play/Stop at start, Title only in middle, Seek/Skip at end
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Play / Pause Button at the start
                FilledIconButton(
                    onClick = { viewModel.requestPlayPause() },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("mini_player_play_pause"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isPlaying) StageYellow else StageGreen,
                        contentColor = PitchBlack
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // 2. Stop Button right next to Play
                IconButton(
                    onClick = { viewModel.stopPlayback() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Parar Música",
                        tint = PanicRed,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // 3. Middle: ONLY Song Title (clickable to Teleprompter)
                val miniPlayerFontSp = when (SongTitleFontSizeOption.fromLevel(userSettings.songTitleFontSize)) {
                    SongTitleFontSizeOption.SMALL -> 14
                    SongTitleFontSizeOption.MEDIUM -> 15
                    SongTitleFontSizeOption.LARGE -> 17
                }
                Text(
                    text = song.title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = miniPlayerFontSp.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = (-0.4).sp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            viewModel.setTeleprompterSong(song)
                            viewModel.navigateTo(AppScreen.TELEPROMPTER)
                        }
                )

                Spacer(modifier = Modifier.width(6.dp))

                // 4. End: Compact Seek & Skip Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    // Replay 10s
                    IconButton(
                        onClick = { viewModel.seekRelative(-10_000L) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Voltar 10s",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Previous Track
                    IconButton(
                        onClick = { viewModel.previousSong() },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Anterior",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Next Track
                    IconButton(
                        onClick = { viewModel.nextSong() },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Próxima",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Forward 10s
                    IconButton(
                        onClick = { viewModel.seekRelative(10_000L) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Avançar 10s",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
