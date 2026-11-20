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
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PedalScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val lastCommand by viewModel.lastPedalCommand.collectAsState()
    val lastTimestamp by viewModel.lastPedalTimestamp.collectAsState()

    var selectedPedalBrand by remember { mutableStateOf("iRig BlueTurn") }
    val pedalBrands = listOf("iRig BlueTurn", "Donner Wireless", "AirTurn DUO", "CUVAVE Page Turner")

    val formattedTime = remember(lastTimestamp) {
        if (lastTimestamp > 0) {
            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastTimestamp))
        } else "Nenhum disparo"
    }

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
                    text = "Pedais Bluetooth & Atalhos",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Controle de reprodução de palco via footswitch e teclado sem as mãos",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Live Command Monitor Box
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pedal_monitor_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, if (lastTimestamp > 0) StageYellow else SurfaceCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (lastTimestamp > 0) StageGreen else TextMuted)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "MONITOR DE ENTRADA AO VIVO",
                                color = if (lastTimestamp > 0) StageGreen else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Text(text = formattedTime, color = TextMuted, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = PitchBlack
                    ) {
                        Text(
                            text = lastCommand,
                            color = StageYellow,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Pressione qualquer botão no seu pedal Bluetooth emparelhado para testar a resposta imediata.",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Pedal Brand Profile Selector
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Perfil de Compatibilidade de Pedal",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        pedalBrands.take(2).forEach { brand ->
                            val isSelected = selectedPedalBrand == brand
                            OutlinedButton(
                                onClick = { selectedPedalBrand = brand },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) StageYellowContainer else SurfaceCardElevated,
                                    contentColor = if (isSelected) StageYellow else TextSecondary
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) StageYellow else SurfaceCardBorder)
                            ) {
                                Text(brand, fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        pedalBrands.drop(2).forEach { brand ->
                            val isSelected = selectedPedalBrand == brand
                            OutlinedButton(
                                onClick = { selectedPedalBrand = brand },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) StageYellowContainer else SurfaceCardElevated,
                                    contentColor = if (isSelected) StageYellow else TextSecondary
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) StageYellow else SurfaceCardBorder)
                            ) {
                                Text(brand, fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // Standard Key Mapping Reference Table
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Mapeamento Padrão de Comandos",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    PedalShortcutRow(keyName = "ESPAÇO", actionDesc = "Play / Pause do VS no palco")
                    PedalShortcutRow(keyName = "SETA DIREITA / PAGE DOWN", actionDesc = "Próxima Música da sequência")
                    PedalShortcutRow(keyName = "SETA ESQUERDA / PAGE UP", actionDesc = "Música Anterior da sequência")
                    PedalShortcutRow(keyName = "ESC", actionDesc = "PÂNICO (Fade-out 700ms imediato)", isPanic = true)
                }
            }
        }
    }
}

@Composable
fun PedalShortcutRow(
    keyName: String,
    actionDesc: String,
    isPanic: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isPanic) PanicRed.copy(alpha = 0.2f) else SurfaceCardElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isPanic) PanicRed else StageYellow)
        ) {
            Text(
                text = keyName,
                color = if (isPanic) PanicRed else StageYellow,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Text(
            text = actionDesc,
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
