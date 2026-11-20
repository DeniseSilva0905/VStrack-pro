package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SongTitleFontSizeOption
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.userSettings.collectAsState()
    val scope = rememberCoroutineScope()

    var userName by remember(settings) { mutableStateOf(settings.userName) }
    var bandName by remember(settings) { mutableStateOf(settings.bandName) }
    var stageRole by remember(settings) { mutableStateOf(settings.stageRole) }
    var stagePin by remember(settings) { mutableStateOf(settings.stagePin) }
    var intervalSec by remember(settings) { mutableStateOf(settings.intervalSeconds.toString()) }

    var isEditModalOpen by remember { mutableStateOf(false) }
    var isDeleteAccountConfirmOpen by remember { mutableStateOf(false) }

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
                    text = "Meu Perfil & Configurações",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Preferências de reprodução, trava de segurança e dados da banda",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Profile Identity Card
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(StageYellowContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = StageYellow,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = settings.userName,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = settings.userEmail,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "${settings.bandName} • ${settings.stageRole}",
                                    color = StageYellow,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        IconButton(onClick = { isEditModalOpen = true }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar", tint = StageYellow)
                        }
                    }
                }
            }
        }

        // Stage Safety Settings Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Segurança de Palco & Reprodução",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Play Confirmation Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Confirmação de Disparo",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Exibe diálogo de segurança antes de iniciar o VS para evitar disparos acidentais",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = settings.requirePlayConfirmation,
                            onCheckedChange = { viewModel.updateRequirePlayConfirmation(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PitchBlack,
                                checkedTrackColor = StageYellow
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = SurfaceCardBorder)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Continuous Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Modo Contínuo vs Modo 1x1",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (settings.isContinuousMode) "Modo Contínuo: avança automaticamente após término" else "Modo 1x1: pausa no fim da música e aguarda próximo comando",
                                color = if (settings.isContinuousMode) StageYellow else WarningAmber,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = settings.isContinuousMode,
                            onCheckedChange = { viewModel.updateContinuousMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PitchBlack,
                                checkedTrackColor = StageYellow
                            )
                        )
                    }

                    if (settings.isContinuousMode) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Tempo entre Músicas", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Espera antes do próximo disparo (${settings.intervalSeconds}s)", color = TextSecondary, fontSize = 11.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.updateIntervalSeconds(settings.intervalSeconds - 1) },
                                    enabled = settings.intervalSeconds > 1,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = null, tint = if (settings.intervalSeconds > 1) StageYellow else TextMuted)
                                }
                                Text("${settings.intervalSeconds}s", color = StageYellow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                IconButton(
                                    onClick = { viewModel.updateIntervalSeconds(settings.intervalSeconds + 1) },
                                    enabled = settings.intervalSeconds < 10,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = if (settings.intervalSeconds < 10) StageYellow else TextMuted)
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SurfaceCardBorder)

                    // Opção: Exibir ou ocultar nome do artista na biblioteca
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Exibir Artista na Biblioteca",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (settings.showArtistInLibrary) "Exibindo nome do artista abaixo do título da música" else "Ocultando nome do artista para economizar espaço e deixar a lista mais limpa",
                                color = if (settings.showArtistInLibrary) StageYellow else TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = settings.showArtistInLibrary,
                            onCheckedChange = { viewModel.updateShowArtistInLibrary(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PitchBlack,
                                checkedTrackColor = StageYellow
                            )
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SurfaceCardBorder)

                    // Opção: 3 tamanhos para a fonte do título da música
                    val activeFontOption = SongTitleFontSizeOption.fromLevel(settings.songTitleFontSize)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Tamanho do Título das Músicas",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Escolha entre 3 tamanhos para leitura ideal no palco (${activeFontOption.label} • ${activeFontOption.sizeSp}sp)",
                            color = StageYellow,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SongTitleFontSizeOption.entries.forEach { option ->
                                val isSelected = settings.songTitleFontSize == option.level
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("song_title_font_option_${option.level}")
                                        .clickable { viewModel.updateSongTitleFontSize(option.level) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) StageYellow else SurfaceCardElevated,
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) StageYellow else SurfaceCardBorder
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = option.label,
                                            color = if (isSelected) PitchBlack else TextPrimary,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "${option.sizeSp}sp",
                                            color = if (isSelected) PitchBlack.copy(alpha = 0.75f) else TextSecondary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = PitchBlack,
                            border = BorderStroke(1.dp, SurfaceCardBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = StageYellow,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Prévia: Ousado Amor (Reckless Love)",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = activeFontOption.sizeSp.sp,
                                    letterSpacing = (-0.4).sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Account Actions Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Ações da Conta",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { viewModel.showToast("Sessão sincronizada com o banco local.") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                    ) {
                        Icon(imageVector = Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Redefinir Senha de Acesso")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { isDeleteAccountConfirmOpen = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PanicRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PanicRed.copy(alpha = 0.5f))
                    ) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Excluir Conta e Dados...")
                    }
                }
            }
        }
    }

    // Modal: Edit Profile
    if (isEditModalOpen) {
        AlertDialog(
            onDismissRequest = { isEditModalOpen = false },
            title = { Text("Editar Perfil & Palco", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = userName,
                        onValueChange = { userName = it },
                        label = { Text("Nome do Músico / Operador") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bandName,
                        onValueChange = { bandName = it },
                        label = { Text("Banda / Ministério") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = stageRole,
                        onValueChange = { stageRole = it },
                        label = { Text("Função no Palco (ex: Teclado, Vocal, Bateria)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = intervalSec,
                        onValueChange = { intervalSec = it },
                        label = { Text("Intervalo entre faixas contínuas (segundos)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.repository.saveSettings(
                                settings.copy(
                                    userName = userName,
                                    bandName = bandName,
                                    stageRole = stageRole,
                                    stagePin = stagePin.ifBlank { "1234" },
                                    intervalSeconds = intervalSec.toIntOrNull() ?: 3
                                )
                            )
                            viewModel.showToast("Perfil atualizado!")
                        }
                        isEditModalOpen = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StageYellow, contentColor = PitchBlack)
                ) {
                    Text("Salvar", color = PitchBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isEditModalOpen = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = SurfaceCardElevated
        )
    }

    // Modal: Delete Account Confirmation
    if (isDeleteAccountConfirmOpen) {
        AlertDialog(
            onDismissRequest = { isDeleteAccountConfirmOpen = false },
            title = { Text("Confirmar Exclusão de Conta?", color = TextPrimary) },
            text = {
                Text(
                    "Esta ação apagará as configurações locais de usuário e redefinirá o perfil. Deseja prosseguir?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.repository.saveSettings(com.example.data.model.UserSettingsEntity())
                            viewModel.showToast("Perfil redefinido para os padrões de fábrica.")
                        }
                        isDeleteAccountConfirmOpen = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PanicRed)
                ) {
                    Text("Excluir / Resetar", color = TextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { isDeleteAccountConfirmOpen = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = SurfaceCardElevated
        )
    }
}
