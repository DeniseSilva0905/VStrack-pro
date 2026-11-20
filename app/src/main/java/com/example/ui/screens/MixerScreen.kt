package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.PlaybackState
import com.example.ui.MainViewModel
import com.example.ui.StemChannelState
import com.example.ui.theme.*

@Composable
fun MixerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val channels by viewModel.mixerChannels.collectAsState()
    val playbackState by viewModel.audioEngine.playbackState.collectAsState()
    val isPlaying = playbackState == PlaybackState.PLAYING

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StageGraphite)
            .padding(16.dp)
    ) {
        // Mixer Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Mixer Multipista (Stems)",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Controle independente de 8 canais, mute, solo e pan",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Surface(
                color = if (isPlaying) StageGreen.copy(alpha = 0.2f) else SurfaceCardElevated,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isPlaying) StageGreen else SurfaceCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) StageGreen else TextMuted)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPlaying) "DSP ATIVO" else "STANDBY",
                        color = if (isPlaying) StageGreen else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Horizontal Scrollable Channel Strips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
        ) {
            items(channels) { channel ->
                MixerChannelStrip(
                    channel = channel,
                    isPlaying = isPlaying,
                    onVolumeChange = { vol -> viewModel.updateChannelVolume(channel.id, vol) },
                    onMuteToggle = { viewModel.toggleChannelMute(channel.id) },
                    onSoloToggle = { viewModel.toggleChannelSolo(channel.id) },
                    onPanChange = { pan -> viewModel.updateChannelPan(channel.id, pan) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Preset Save Action
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = SurfaceCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Preset de Mixagem do Show", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "As posições dos faders são salvas automaticamente", color = TextSecondary, fontSize = 11.sp)
                }
                Button(
                    onClick = { viewModel.showToast("Configurações do Mixer salvas para este repertório!") },
                    colors = ButtonDefaults.buttonColors(containerColor = StageYellow, contentColor = PitchBlack)
                ) {
                    Text("Salvar Mix", color = PitchBlack, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MixerChannelStrip(
    channel: StemChannelState,
    isPlaying: Boolean,
    onVolumeChange: (Float) -> Unit,
    onMuteToggle: () -> Unit,
    onSoloToggle: () -> Unit,
    onPanChange: (Float) -> Unit
) {
    val isMaster = channel.id == "master"
    val isClick = channel.id == "click"

    // Animated VU meter pulse when playing and not muted
    val infiniteTransition = rememberInfiniteTransition(label = "vu")
    val animatedVu by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = channel.vuLevel,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vu_anim"
    )

    val currentVu = if (isPlaying && !channel.isMuted) animatedVu * channel.volume else 0.05f

    Surface(
        modifier = Modifier
            .width(105.dp)
            .fillMaxHeight(),
        shape = RoundedCornerShape(12.dp),
        color = if (isMaster) SurfaceCardElevated else SurfaceCard,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isMaster) StageYellow else if (channel.isSolo) WarningAmber else SurfaceCardBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Channel Name & Badge
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = channel.name,
                    color = if (isMaster) StageYellow else if (isClick) InEarCyan else TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1
                )
                Text(
                    text = "${(channel.volume * 100).toInt()}%",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }

            // Pan Slider / Display
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when {
                        channel.pan < -0.1f -> "L ${(channel.pan * -100).toInt()}%"
                        channel.pan > 0.1f -> "R ${(channel.pan * 100).toInt()}%"
                        else -> "C"
                    },
                    color = StageYellow,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = channel.pan,
                    onValueChange = onPanChange,
                    valueRange = -1.0f..1.0f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = StageYellow,
                        activeTrackColor = StageYellowContainer,
                        inactiveTrackColor = PitchBlack
                    )
                )
            }

            // Dual Central Column: VU Meter + Fader Slider
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Vertical VU Meter Bar
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(PitchBlack),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(currentVu.coerceIn(0.05f, 1f))
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when {
                                    currentVu > 0.85f -> PanicRed
                                    currentVu > 0.65f -> WarningAmber
                                    else -> StageGreen
                                }
                            )
                    )
                }

                // Volume Fader Slider
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Slider(
                        value = channel.volume,
                        onValueChange = onVolumeChange,
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxHeight(),
                        colors = SliderDefaults.colors(
                            thumbColor = if (isMaster) StageYellow else TextPrimary,
                            activeTrackColor = if (isMaster) StageYellow else TextSecondary,
                            inactiveTrackColor = PitchBlack
                        )
                    )
                }
            }

            // Mute & Solo Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // MUTE
                Button(
                    onClick = onMuteToggle,
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (channel.isMuted) PanicRed else SurfaceCardElevated
                    )
                ) {
                    Text(
                        text = "M",
                        color = if (channel.isMuted) PitchBlack else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // SOLO
                Button(
                    onClick = onSoloToggle,
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (channel.isSolo) WarningAmber else SurfaceCardElevated
                    )
                ) {
                    Text(
                        text = "S",
                        color = if (channel.isSolo) PitchBlack else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
