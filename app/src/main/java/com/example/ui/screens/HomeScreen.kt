package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.PlaybackState
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onOpenDrawer: () -> Unit = {}
) {
    val settings by viewModel.userSettings.collectAsState()
    val setlists by viewModel.allSetlists.collectAsState()
    val songs by viewModel.allSongs.collectAsState()
    val activeSetlist by viewModel.activeSetlist.collectAsState()
    val isShowActive by viewModel.isShowActive.collectAsState()
    val playedSongIds by viewModel.playedSongIds.collectAsState()
    val allHistory by viewModel.allHistory.collectAsState()
    val playbackState by viewModel.audioEngine.playbackState.collectAsState()
    val isPlaying = playbackState == PlaybackState.PLAYING

    val favoriteSongsCount = remember(songs) { songs.count { it.isFavorite } }
    val songsWithAudioCount = remember(songs) { songs.count { it.audioFileName.isNotBlank() } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(StageGraphite)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // Top Header with Drawer Access and Live Stage Status
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Abrir Menu",
                            tint = StageYellow
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "VSTracks Pro",
                            color = StageYellow,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "${settings.userName.ifBlank { "Músico" }} • ${settings.bandName.ifBlank { "Ao Vivo" }}",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceCardElevated,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isShowActive) LilacPlayedBorder else StageYellow
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isShowActive) LilacPlayed else StageGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isShowActive) "SHOW ATIVO" else "SISTEMA PRONTO",
                            color = if (isShowActive) LilacPlayedGlow else StageYellow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Live Show Controller Card (if show is currently running)
        if (isShowActive) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCardElevated),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, LilacPlayedBorder)
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
                                        .background(if (isPlaying) StageGreen else WarningAmber)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isPlaying) "SHOW AO VIVO TOCANDO" else "SHOW AO VIVO PAUSADO",
                                    color = if (isPlaying) StageGreen else WarningAmber,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Surface(
                                color = LilacPlayed,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "${playedSongIds.size} TOCADAS",
                                    color = PitchBlack,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = activeSetlist?.name ?: "Repertório do Show",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (isPlaying) viewModel.pauseShow() else viewModel.resumeShow()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPlaying) WarningAmber else StageGreen,
                                    contentColor = PitchBlack
                                )
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isPlaying) "Pausar" else "Retomar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.stopAndFinishShow() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard, contentColor = PanicRed),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PanicRed.copy(alpha = 0.6f))
                            ) {
                                Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp), tint = PanicRed)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Encerrar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.navigateTo(AppScreen.STAGE_SHOW) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StageYellowContainer, contentColor = StageYellow)
                            ) {
                                Text("Ver Palco", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Section Title: Dashboard & Visão Geral
        item {
            Text(
                text = "Dashboard do Palco",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Primary Metric Cards: Quantidade de Músicas & Quantidade de Repertórios
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: Quantidade de Músicas
                DashboardMetricCard(
                    title = "Músicas",
                    count = songs.size.toString(),
                    subtitle = "$songsWithAudioCount com VS pronto",
                    icon = Icons.Default.LibraryMusic,
                    accentColor = StageYellow,
                    buttonLabel = "Ver Biblioteca",
                    onClick = { viewModel.navigateTo(AppScreen.LIBRARY) },
                    modifier = Modifier.weight(1f)
                )

                // Card 2: Quantidade de Repertórios
                DashboardMetricCard(
                    title = "Repertórios",
                    count = setlists.size.toString(),
                    subtitle = if (activeSetlist != null) "Ativo: ${activeSetlist?.name}" else "Nenhum ativo",
                    icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                    accentColor = StageYellow,
                    buttonLabel = "Ver Repertórios",
                    onClick = { viewModel.navigateTo(AppScreen.SETLISTS) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Secondary Metric Cards: Favoritas & Histórico
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 3: Favoritas
                DashboardSmallStatCard(
                    title = "Favoritas",
                    count = favoriteSongsCount.toString(),
                    icon = Icons.Default.Star,
                    iconTint = StageYellow,
                    onClick = { viewModel.navigateTo(AppScreen.LIBRARY) },
                    modifier = Modifier.weight(1f)
                )

                // Card 4: Shows Realizados
                DashboardSmallStatCard(
                    title = "Shows no Histórico",
                    count = allHistory.size.toString(),
                    icon = Icons.Default.History,
                    iconTint = InEarCyan,
                    onClick = { viewModel.navigateTo(AppScreen.HISTORY) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Active Repertoire Launcher Card (when show is not active)
        if (!isShowActive) {
            item {
                val currentSetlist = activeSetlist ?: setlists.firstOrNull()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_launch_show_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCardElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StageYellow)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                                    contentDescription = null,
                                    tint = StageYellow,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Repertório Selecionado",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (currentSetlist != null) {
                                Surface(
                                    color = StageYellow,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "PRONTO",
                                        color = PitchBlack,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = currentSetlist?.name ?: "Nenhum repertório selecionado",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = currentSetlist?.description?.ifBlank { "Repertório pronto para palco e execução ao vivo" }
                                ?: "Crie ou selecione um repertório para iniciar a apresentação",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (currentSetlist != null) {
                                    viewModel.startLiveShow(currentSetlist)
                                } else if (setlists.isNotEmpty()) {
                                    viewModel.startLiveShow(setlists.first())
                                } else {
                                    viewModel.navigateTo(AppScreen.SETLISTS)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StageYellow,
                                contentColor = PitchBlack
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = PitchBlack
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentSetlist != null) "INICIAR SHOW" else "CRIAR REPERTÓRIO",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }

        // Section Title: Acessos Rápidos do Sistema
        item {
            Text(
                text = "Acessos Rápidos",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Quick Action Shortcuts Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickNavButton(
                        title = "Biblioteca",
                        subtitle = "${songs.size} músicas",
                        icon = Icons.Default.LibraryMusic,
                        onClick = { viewModel.navigateTo(AppScreen.LIBRARY) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickNavButton(
                        title = "Repertórios",
                        subtitle = "${setlists.size} salvos",
                        icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                        onClick = { viewModel.navigateTo(AppScreen.SETLISTS) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickNavButton(
                        title = "Sincronizar Pasta",
                        subtitle = "Pasta local de VS",
                        icon = Icons.Default.FolderOpen,
                        onClick = { viewModel.navigateTo(AppScreen.LIBRARY) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickNavButton(
                        title = "Lixeira",
                        subtitle = "Restaurar itens",
                        icon = Icons.Default.DeleteOutline,
                        onClick = { viewModel.navigateTo(AppScreen.TRASH) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickNavButton(
                        title = "Split Estéreo",
                        subtitle = if (settings.isStereoSplitEnabled) "Fone / PA Ativo" else "Estéreo Padrão",
                        icon = Icons.Default.Headphones,
                        onClick = { viewModel.navigateTo(AppScreen.STEREO_SPLIT) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickNavButton(
                        title = "Histórico",
                        subtitle = "${allHistory.size} shows",
                        icon = Icons.Default.History,
                        onClick = { viewModel.navigateTo(AppScreen.HISTORY) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardMetricCard(
    title: String,
    count: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    buttonLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = count,
                color = TextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceCardElevated,
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = buttonLabel,
                        color = StageYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = StageYellow,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardSmallStatCard(
    title: String,
    count: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = SurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = count,
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun QuickNavButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = SurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = StageYellow,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
