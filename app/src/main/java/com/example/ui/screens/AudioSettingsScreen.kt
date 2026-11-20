package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.TestToneType
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@Composable
fun AudioSettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.userSettings.collectAsState()
    val activeTone by viewModel.audioEngine.activeTestTone.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(StageGraphite)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "Áudio e Split Estéreo",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Roteamento de fone In-Ear (L) e PA Instrumental (R) para palco",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Stereo Split Routing Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Divisão Estéreo (Split L/R)",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = when {
                                    !settings.isStereoSplitEnabled ->
                                        "Modo Mono: Click + Guia e VS Playback somados nos dois canais"
                                    settings.isLRSwapped ->
                                        "Canal Esquerdo = VS Playback (Público / PA)\nCanal Direito = Click + Guia (Músicos)"
                                    else ->
                                        "Canal Esquerdo = Click + Guia (Músicos)\nCanal Direito = VS Playback (Público / PA)"
                                },
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Switch(
                            checked = settings.isStereoSplitEnabled,
                            onCheckedChange = { viewModel.updateStereoSplit(it) },
                            modifier = Modifier.testTag("stereo_split_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PitchBlack,
                                checkedTrackColor = StageYellow
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = SurfaceCardBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    // L/R Digital Swap Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp)
                        ) {
                            Text(
                                text = "Inversão Digital de Canais",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = when {
                                    !settings.isStereoSplitEnabled -> "Ative a Divisão Estéreo acima para usar Split L/R"
                                    settings.isLRSwapped -> "INVERTIDO: L = PA (VS) • R = Fone (Click)"
                                    else -> "Ordem padrão: L (Fone) e R (PA)"
                                },
                                color = if (settings.isLRSwapped && settings.isStereoSplitEnabled) StageYellow else TextMuted,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = { viewModel.toggleLRSwap() },
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("lr_swap_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (settings.isLRSwapped) StageYellow else SurfaceCardElevated,
                                contentColor = if (settings.isLRSwapped) PitchBlack else TextPrimary
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (settings.isLRSwapped) StageYellow else SurfaceCardBorder
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Inverter canais L e R"
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (settings.isLRSwapped) "L/R Invertido" else "Inverter L/R",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Independent Channel Volume Faders
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Volumes Independentes de Saída",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val leftFaderColor = if (settings.isLRSwapped) StageYellow else InEarCyan
                    val rightFaderColor = if (settings.isLRSwapped) InEarCyan else StageYellow

                    // Left Channel Fader
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (settings.isLRSwapped) {
                                "Canal L (VS Playback PA • Invertido)"
                            } else {
                                "Canal L (In-Ear Click & Voz Guia)"
                            },
                            color = leftFaderColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        Text(
                            text = "${(settings.leftVolume * 100).toInt()}%",
                            color = leftFaderColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = settings.leftVolume,
                        onValueChange = { viewModel.updateLeftVolume(it) },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = leftFaderColor,
                            activeTrackColor = leftFaderColor,
                            inactiveTrackColor = PitchBlack
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Right Channel Fader
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (settings.isLRSwapped) {
                                "Canal R (In-Ear Click & Voz Guia • Invertido)"
                            } else {
                                "Canal R (VS Playback PA)"
                            },
                            color = rightFaderColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        Text(
                            text = "${(settings.rightVolume * 100).toInt()}%",
                            color = rightFaderColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = settings.rightVolume,
                        onValueChange = { viewModel.updateRightVolume(it) },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = rightFaderColor,
                            activeTrackColor = rightFaderColor,
                            inactiveTrackColor = PitchBlack
                        )
                    )
                }
            }
        }

        // Test Tones Generator (1000Hz Click & 440Hz VS)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Gerador de Tons de Teste de Palco",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Valide se o Click e o PA estão saindo nos canais corretos antes do show",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1000 Hz Tone (Click check -> L normally, R when swapped)
                        val is1kActive = activeTone == TestToneType.CLICK_1KHZ
                        Button(
                            onClick = {
                                if (is1kActive) {
                                    viewModel.audioEngine.stopTestTone()
                                } else {
                                    viewModel.audioEngine.startTestTone(TestToneType.CLICK_1KHZ)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 52.dp)
                                .testTag("test_1khz_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (is1kActive) InEarCyan else SurfaceCardElevated
                            )
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (is1kActive) "PARAR 1 kHz" else "TESTAR 1 kHz",
                                    color = if (is1kActive) PitchBlack else InEarCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = when {
                                        !settings.isStereoSplitEnabled -> "L + R (Mono)"
                                        settings.isLRSwapped -> "Canal R (Click • Inv.)"
                                        else -> "Canal L (Click)"
                                    },
                                    color = if (is1kActive) PitchBlack else TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // 440 Hz Tone (PA / A tuning check -> R normally, L when swapped)
                        val is440Active = activeTone == TestToneType.VS_440HZ
                        Button(
                            onClick = {
                                if (is440Active) {
                                    viewModel.audioEngine.stopTestTone()
                                } else {
                                    viewModel.audioEngine.startTestTone(TestToneType.VS_440HZ)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 52.dp)
                                .testTag("test_440hz_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (is440Active) StageYellow else SurfaceCardElevated
                            )
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (is440Active) "PARAR 440 Hz" else "TESTAR 440 Hz",
                                    color = if (is440Active) PitchBlack else StageYellow,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = when {
                                        !settings.isStereoSplitEnabled -> "L + R (Mono)"
                                        settings.isLRSwapped -> "Canal L (PA • Inv.)"
                                        else -> "Canal R (PA)"
                                    },
                                    color = if (is440Active) PitchBlack else TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
