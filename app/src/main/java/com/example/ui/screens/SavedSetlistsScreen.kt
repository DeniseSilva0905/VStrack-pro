package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.data.model.SetlistEntity
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SavedSetlistsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val setlists by viewModel.allSetlists.collectAsState()
    val activeSetlist by viewModel.activeSetlist.collectAsState()
    val scope = rememberCoroutineScope()

    var isCreateSetlistOpen by remember { mutableStateOf(false) }
    var setlistToDelete by remember { mutableStateOf<SetlistEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StageGraphite)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // Slim Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FolderSpecial,
                    contentDescription = null,
                    tint = StageYellow,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Repertórios Salvos",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = SurfaceCardElevated
                ) {
                    Text(
                        text = "${setlists.size}",
                        color = StageYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo(AppScreen.TRASH) },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Lixeira",
                        tint = TextSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Button(
                    onClick = { isCreateSetlistOpen = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StageYellow,
                        contentColor = PitchBlack
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .height(28.dp)
                        .testTag("saved_setlists_create_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = PitchBlack,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Novo Repertório",
                        color = PitchBlack,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = (-0.3).sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Toque em um repertório salvo para abri-lo na tela de Repertórios.",
            color = TextSecondary,
            fontSize = 11.sp,
            letterSpacing = (-0.2).sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (setlists.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                color = SurfaceCard,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Nenhum repertório salvo",
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Crie seu primeiro repertório para montar a ordem do show.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                itemsIndexed(setlists, key = { _, s -> s.id }) { index, setlist ->
                    val isActive = activeSetlist?.id == setlist.id

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("saved_setlist_card_${setlist.id}")
                            .clickable {
                                viewModel.selectSetlist(setlist)
                                viewModel.navigateTo(AppScreen.SETLISTS)
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isActive) SurfaceCardElevated else SurfaceCard,
                        border = BorderStroke(
                            width = if (isActive) 1.5.dp else 1.dp,
                            color = if (isActive) StageYellow else SurfaceCardBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isActive) StageYellow else PitchBlack
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = if (isActive) PitchBlack else TextSecondary,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = setlist.name,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            letterSpacing = (-0.3).sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (isActive) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = StageYellow
                                            ) {
                                                Text(
                                                    text = "SELECIONADO",
                                                    color = PitchBlack,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = setlist.description.ifBlank { "Toque para abrir na tela de Repertórios" },
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        letterSpacing = (-0.2).sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                // Start Live Show directly
                                IconButton(
                                    onClick = {
                                        viewModel.selectSetlist(setlist)
                                        viewModel.startLiveShow(setlist, navigateToStage = true)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayCircle,
                                        contentDescription = "Iniciar Show com este Repertório",
                                        tint = StageGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Duplicate Setlist
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            val newId = viewModel.repository.duplicateSetlist(setlist.id)
                                            val duplicated = viewModel.repository.getSetlistById(newId)
                                            duplicated?.let { viewModel.selectSetlist(it) }
                                            viewModel.showToast("Repertório '${setlist.name}' duplicado!")
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Duplicar Repertório",
                                        tint = StageYellow,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Delete Setlist
                                IconButton(
                                    onClick = { setlistToDelete = setlist },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Excluir Repertório",
                                        tint = PanicRed.copy(alpha = 0.85f),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }
                }
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
                                created?.let {
                                    viewModel.selectSetlist(it)
                                    viewModel.navigateTo(AppScreen.SETLISTS)
                                }
                                viewModel.showToast("Repertório '$newName' criado e aberto!")
                            }
                            isCreateSetlistOpen = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StageYellow,
                        contentColor = PitchBlack
                    )
                ) {
                    Text("Criar e Abrir", color = PitchBlack, fontWeight = FontWeight.Bold)
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

    // Modal: Delete Setlist
    if (setlistToDelete != null) {
        AlertDialog(
            onDismissRequest = { setlistToDelete = null },
            title = { Text("Excluir Repertório?", color = TextPrimary) },
            text = {
                Text(
                    text = "Deseja excluir o repertório '${setlistToDelete?.name}'? As músicas continuarão salvas na Biblioteca.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = setlistToDelete ?: return@Button
                        scope.launch {
                            viewModel.repository.deleteSetlist(target.id)
                            if (activeSetlist?.id == target.id) {
                                val remaining = viewModel.allSetlists.value.filter { it.id != target.id }
                                remaining.firstOrNull()?.let { viewModel.selectSetlist(it) }
                            }
                            viewModel.showToast("Repertório excluído.")
                        }
                        setlistToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PanicRed)
                ) {
                    Text("Excluir", color = TextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { setlistToDelete = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = SurfaceCardElevated
        )
    }
}
