package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SetlistItemEntity
import com.example.data.model.SongEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.*

enum class TrashTab {
    BIBLIOTECA,
    REPERTORIO
}

@Composable
fun TrashScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val trashSongs by viewModel.trashSongs.collectAsState()
    val trashItems by viewModel.trashSetlistItems.collectAsState()

    var activeTab by remember { mutableStateOf(TrashTab.BIBLIOTECA) }
    var showEmptyTrashConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StageGraphite)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Lixeira",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Recupere ou remova itens definitivamente",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            if (trashSongs.isNotEmpty() || trashItems.isNotEmpty()) {
                Button(
                    onClick = { showEmptyTrashConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PanicRed),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Esvaziar Lixeira",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tab Selector: Biblioteca (X) | Repertório (Y)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = activeTab == TrashTab.BIBLIOTECA,
                onClick = { activeTab = TrashTab.BIBLIOTECA },
                label = {
                    Text(
                        text = "Biblioteca (${trashSongs.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StageYellow,
                    selectedLabelColor = PitchBlack,
                    containerColor = SurfaceCard,
                    labelColor = TextSecondary
                )
            )

            FilterChip(
                selected = activeTab == TrashTab.REPERTORIO,
                onClick = { activeTab = TrashTab.REPERTORIO },
                label = {
                    Text(
                        text = "Repertório (${trashItems.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StageYellow,
                    selectedLabelColor = PitchBlack,
                    containerColor = SurfaceCard,
                    labelColor = TextSecondary
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // List Content
        when (activeTab) {
            TrashTab.BIBLIOTECA -> {
                if (trashSongs.isEmpty()) {
                    EmptyTrashBox(label = "Nenhuma música da biblioteca na lixeira.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        items(trashSongs) { song ->
                            TrashSongRow(
                                song = song,
                                onRestore = { viewModel.restoreSong(song) },
                                onDeletePermanent = { viewModel.deleteSongPermanently(song) }
                            )
                        }
                    }
                }
            }
            TrashTab.REPERTORIO -> {
                if (trashItems.isEmpty()) {
                    EmptyTrashBox(label = "Nenhum item de repertório na lixeira.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        items(trashItems) { item ->
                            TrashItemRow(
                                item = item,
                                onRestore = { viewModel.restoreItem(item) },
                                onDeletePermanent = { viewModel.deleteItemPermanently(item) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal: Confirmation to empty entire trash
    if (showEmptyTrashConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashConfirm = false },
            title = { Text("Esvaziar Lixeira?", color = TextPrimary) },
            text = {
                Text(
                    "Todos os itens da lixeira serão excluídos definitivamente. Esta ação não poderá ser desfeita.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.emptyTrash()
                        showEmptyTrashConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PanicRed)
                ) {
                    Text("Esvaziar Agora", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashConfirm = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = SurfaceCardElevated
        )
    }
}

@Composable
private fun EmptyTrashBox(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = label,
                color = TextSecondary,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun TrashSongRow(
    song: SongEntity,
    onRestore: () -> Unit,
    onDeletePermanent: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = SurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist.ifBlank { "Artista não informado" },
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Restore Button (Yellow)
                OutlinedButton(
                    onClick = onRestore,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StageYellow),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StageYellow),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = StageYellow
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restaurar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Delete Permanently Button
                IconButton(
                    onClick = onDeletePermanent,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "Excluir Definitivamente",
                        tint = PanicRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TrashItemRow(
    item: SetlistItemEntity,
    onRestore: () -> Unit,
    onDeletePermanent: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = SurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.customTitle,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Tipo: ${item.type} • Duração: ${item.durationSeconds / 60}m",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onRestore,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StageYellow),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StageYellow),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = StageYellow
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restaurar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = onDeletePermanent,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "Excluir Definitivamente",
                        tint = PanicRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
