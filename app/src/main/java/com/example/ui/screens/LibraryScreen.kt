package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.data.model.SongEntity
import com.example.data.model.SongTitleFontSizeOption
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.allSongs.collectAsState()
    val setlists by viewModel.allSetlists.collectAsState()
    val playedSongIds by viewModel.playedSongIds.collectAsState()
    val isShowActive by viewModel.isShowActive.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val titleFontSizeSp = SongTitleFontSizeOption.fromLevel(userSettings.songTitleFontSize).sizeSp
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var searchQuery by remember { mutableStateOf("") }
    var onlyFavoritesFilter by remember { mutableStateOf(false) }

    var songToEdit by remember { mutableStateOf<SongEntity?>(null) }
    var songToDelete by remember { mutableStateOf<SongEntity?>(null) }
    var songToAddToSetlist by remember { mutableStateOf<SongEntity?>(null) }
    var isNewSongModalOpen by remember { mutableStateOf(false) }

    val folderSyncPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { viewModel.syncFolderAudio(it) }
    }

    val directAudioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importMultipleAudioFiles(uris, "Local")
        }
    }

    // Filter logic: Search by title/artist and favorites sorted to top in alphabetical order
    val filteredSongs = remember(songs, searchQuery, onlyFavoritesFilter) {
        songs.filter { song ->
            val matchesFavorites = !onlyFavoritesFilter || song.isFavorite
            val matchesQuery = searchQuery.isBlank() ||
                song.title.contains(searchQuery, ignoreCase = true) ||
                song.artist.contains(searchQuery, ignoreCase = true)
            matchesFavorites && matchesQuery
        }.sortedWith(
            compareByDescending<SongEntity> { it.isFavorite }
                .thenBy { it.title.lowercase() }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StageGraphite,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isNewSongModalOpen = true },
                containerColor = StageYellow,
                contentColor = PitchBlack,
                modifier = Modifier.testTag("add_song_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Adicionar Música")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                }
                .padding(horizontal = 14.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // Screen Header & Import / Sync Buttons: Slim & Grouped
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Biblioteca",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SurfaceCardElevated
                    ) {
                        Text(
                            text = "${songs.size}",
                            color = StageYellow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { viewModel.cleanDuplicateSongs() },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CleaningServices, contentDescription = "Limpar Duplicadas", tint = StageYellow, modifier = Modifier.size(17.dp))
                    }

                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.TRASH) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Lixeira", tint = TextSecondary, modifier = Modifier.size(17.dp))
                    }

                    // Sincronizar Pasta VS
                    OutlinedButton(
                        onClick = { folderSyncPicker.launch(null) },
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StageYellow),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StageYellow),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(13.dp), tint = StageYellow)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Sincronizar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    // Importar Vários VS
                    Button(
                        onClick = { directAudioPicker.launch(arrayOf("audio/*", "*/*")) },
                        colors = ButtonDefaults.buttonColors(containerColor = StageYellow, contentColor = PitchBlack),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 1.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(13.dp), tint = PitchBlack)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Importar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Observação do caminho da pasta sincronizada em cinza claro
            val folderDisplay = if (userSettings.googleDriveFolder.isNotBlank()) {
                "Pasta sincronizada: ${userSettings.googleDriveFolder}"
            } else {
                "Nenhuma pasta conectada para sincronização"
            }
            Text(
                text = folderDisplay,
                color = TextSecondary.copy(alpha = 0.55f),
                fontSize = 10.sp,
                letterSpacing = (-0.2).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
            )

            // Search Bar (Cursor active when focused, dismisses when tapping outside)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("library_search_input"),
                placeholder = { Text("Pesquisar música ou artista...", color = TextMuted, fontSize = 12.sp) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = ""; focusManager.clearFocus() }, modifier = Modifier.size(24.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Limpar", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StageYellow,
                    unfocusedBorderColor = SurfaceCardBorder,
                    focusedContainerColor = SurfaceCard,
                    unfocusedContainerColor = SurfaceCard,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Filter Chips - All Songs and Favorites Only
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = !onlyFavoritesFilter,
                        onClick = { onlyFavoritesFilter = false },
                        label = { Text("Todas (${songs.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StageYellow,
                            selectedLabelColor = PitchBlack,
                            containerColor = SurfaceCard,
                            labelColor = TextSecondary
                        )
                    )
                }

                item {
                    FilterChip(
                        selected = onlyFavoritesFilter,
                        onClick = { onlyFavoritesFilter = !onlyFavoritesFilter },
                        leadingIcon = {
                            Icon(
                                imageVector = if (onlyFavoritesFilter) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (onlyFavoritesFilter) PitchBlack else StageYellow
                            )
                        },
                        label = { Text("Favoritas", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StageYellow,
                            selectedLabelColor = PitchBlack,
                            containerColor = SurfaceCard,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Songs List
            if (filteredSongs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.MusicOff,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (onlyFavoritesFilter) "Nenhuma música favoritada ainda" else "Nenhuma música encontrada",
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Toque em '+' para cadastrar uma nova música ou na estrela para favoritar",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(filteredSongs) { song ->
                        val isPlayedInShow = isShowActive && playedSongIds.contains(song.id)
                        LibrarySongRow(
                            song = song,
                            showArtist = userSettings.showArtistInLibrary,
                            titleFontSizeSp = titleFontSizeSp,
                            isPlayedInShow = isPlayedInShow,
                            onPlay = { viewModel.requestPlaySong(song) },
                            onToggleFavorite = { viewModel.toggleFavorite(song) },
                            onOpenLyrics = {
                                viewModel.setTeleprompterSong(song)
                                viewModel.navigateTo(AppScreen.TELEPROMPTER)
                            },
                            onAddToSetlist = { songToAddToSetlist = song },
                            onEdit = { songToEdit = song },
                            onDelete = { songToDelete = song }
                        )
                    }
                }
            }
        }
    }

    // Modal: Add / Edit Song (Title mandatory, Artist optional, VS audio attachment from Device or Google Drive)
    if (isNewSongModalOpen || songToEdit != null) {
        val editing = songToEdit
        SongFormDialog(
            initialSong = editing,
            onDismiss = {
                isNewSongModalOpen = false
                songToEdit = null
            },
            onSave = { updated ->
                scope.launch {
                    if (editing != null) {
                        viewModel.repository.updateSong(updated)
                        viewModel.showToast("Música '${updated.title}' atualizada!")
                    } else {
                        viewModel.repository.insertSong(updated)
                        viewModel.showToast("Música '${updated.title}' adicionada!")
                    }
                }
                isNewSongModalOpen = false
                songToEdit = null
            }
        )
    }

    // Modal: Delete Confirmation -> Moves to Trash
    if (songToDelete != null) {
        AlertDialog(
            onDismissRequest = { songToDelete = null },
            title = { Text("Mover Música para Lixeira?", color = TextPrimary) },
            text = {
                Text(
                    "Deseja mover '${songToDelete?.title}' para a lixeira? Ela poderá ser restaurada a qualquer momento.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val s = songToDelete ?: return@Button
                        viewModel.moveSongToTrash(s)
                        songToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PanicRed)
                ) {
                    Text("Mover para Lixeira", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { songToDelete = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = SurfaceCardElevated
        )
    }

    // Modal: Add to Setlist
    if (songToAddToSetlist != null) {
        AlertDialog(
            onDismissRequest = { songToAddToSetlist = null },
            title = { Text("Adicionar ao Repertório", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Selecione em qual repertório incluir '${songToAddToSetlist?.title}':", color = TextSecondary)
                    setlists.forEach { s ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val song = songToAddToSetlist ?: return@clickable
                                    scope.launch {
                                        viewModel.repository.addSongToSetlist(s.id, song)
                                        viewModel.showToast("'${song.title}' adicionada a '${s.name}'!")
                                    }
                                    songToAddToSetlist = null
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                        ) {
                            Text(
                                text = s.name,
                                color = TextPrimary,
                                modifier = Modifier.padding(12.dp),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { songToAddToSetlist = null }) {
                    Text("Fechar", color = TextSecondary)
                }
            },
            containerColor = SurfaceCardElevated
        )
    }
}

@Composable
fun LibrarySongRow(
    song: SongEntity,
    showArtist: Boolean = true,
    titleFontSizeSp: Int = 16,
    isPlayedInShow: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenLyrics: () -> Unit,
    onAddToSetlist: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    // When song has been played during the show, highlight in LILAC (exclusive for played songs)
    val rowBackground = if (isPlayedInShow) LilacPlayedBg else SurfaceCard
    val rowBorder = if (isPlayedInShow) LilacPlayedBorder else SurfaceCardBorder

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = rowBackground,
        border = androidx.compose.foundation.BorderStroke(if (isPlayedInShow) 1.5.dp else 1.dp, rowBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = onPlay,
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isPlayedInShow) LilacPlayed else StageYellow,
                        contentColor = PitchBlack
                    )
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Tocar", modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = song.title,
                            color = if (isPlayedInShow) LilacPlayedGlow else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = titleFontSizeSp.sp,
                            letterSpacing = (-0.4).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isPlayedInShow) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "(TOCADA)",
                                color = LilacPlayed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.3).sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (showArtist && song.artist.isNotBlank()) {
                            Text(
                                text = song.artist,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                letterSpacing = (-0.3).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        if (song.lyricsRichJson.isNotBlank()) {
                            Surface(
                                color = StageYellow.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(3.dp),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, StageYellow)
                            ) {
                                Text(
                                    text = "PPTX",
                                    color = StageYellow,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Compact Action Buttons Row (Estrela, Folhinha, Adicionar, Lápis, Lixeira)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Estrela (Favorito)
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = if (song.isFavorite) "Desfavoritar" else "Favoritar",
                        tint = if (song.isFavorite) StageYellow else TextMuted,
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Folhinha (Letra / Cifras)
                IconButton(
                    onClick = onOpenLyrics,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "Letra e Cifras",
                        tint = StageYellow,
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Adicionar ao Repertório
                IconButton(
                    onClick = onAddToSetlist,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistAdd,
                        contentDescription = "Add ao Setlist",
                        tint = StageYellow,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Lápis (Editar)
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Lixeira (Excluir)
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Excluir",
                        tint = PanicRed.copy(alpha = 0.8f),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

/**
 * Clean & Frictionless Song Modal:
 * - Nome da Música (obrigatório)
 * - Artista (não obrigatório)
 * - Adicionar VS: Do Aparelho ou Do Google Drive
 * - Botão Salvar em Amarelo (StageYellow)
 */
@Composable
fun SongFormDialog(
    initialSong: SongEntity?,
    onDismiss: () -> Unit,
    onSave: (SongEntity) -> Unit
) {
    var title by remember { mutableStateOf(initialSong?.title ?: "") }
    var artist by remember { mutableStateOf(initialSong?.artist ?: "") }
    var audioUri by remember { mutableStateOf(initialSong?.audioUri ?: "") }
    var audioSource by remember { mutableStateOf(initialSong?.origin ?: "") }
    var audioFileName by remember { mutableStateOf(initialSong?.audioFileName ?: "") }

    // Single Audio Picker for local device file
    val deviceAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            audioUri = it.toString()
            audioSource = "Local"
            val lastPart = it.lastPathSegment ?: "audio_vs.mp3"
            audioFileName = lastPart.substringAfterLast('/')
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialSong == null) "Adicionar Nova Música" else "Editar Música",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Nome da Música (OBRIGATÓRIO)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nome da Música *", color = StageYellow) },
                    placeholder = { Text("Ex: Me Leva Pra Casa", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StageYellow,
                        unfocusedBorderColor = SurfaceCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                // 2. Artista (NÃO OBRIGATÓRIO)
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artista / Banda (Opcional)", color = TextSecondary) },
                    placeholder = { Text("Ex: Israel Subirá", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StageYellow,
                        unfocusedBorderColor = SurfaceCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                // 3. Adicionar VS (Arquivo Individual do Aparelho)
                Text(
                    text = "Arquivo de VS (Playback):",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = { deviceAudioLauncher.launch(arrayOf("audio/*", "application/ogg", "*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (audioUri.isNotBlank()) StageYellowContainer else SurfaceCardElevated,
                        contentColor = if (audioUri.isNotBlank()) StageYellow else TextPrimary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StageYellow)
                ) {
                    Icon(
                        imageVector = Icons.Default.AudioFile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = StageYellow
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (audioUri.isNotBlank()) "Alterar Arquivo de VS" else "Selecionar Arquivo de VS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Audio status card
                Surface(
                    color = SurfaceCard,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (audioUri.isNotBlank()) StageYellow else SurfaceCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (audioUri.isNotBlank()) Icons.Default.CheckCircle else Icons.Default.AudioFile,
                            contentDescription = null,
                            tint = if (audioUri.isNotBlank()) StageYellow else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (audioUri.isNotBlank()) "VS Conectado: ${audioFileName.ifBlank { "Arquivo de áudio" }}" else "Nenhum arquivo de VS selecionado",
                                color = if (audioUri.isNotBlank()) TextPrimary else TextMuted,
                                fontSize = 12.sp,
                                fontWeight = if (audioUri.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (audioSource.isNotBlank()) {
                                Text(
                                    text = "Origem: $audioSource",
                                    color = StageYellow,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val song = (initialSong ?: SongEntity(title = title.trim(), artist = artist.trim())).copy(
                            title = title.trim(),
                            artist = artist.trim(),
                            audioUri = audioUri,
                            audioFileName = audioFileName,
                            origin = if (audioSource.isNotBlank()) audioSource else "Local",
                            scrollSpeed = initialSong?.scrollSpeed ?: 3
                        )
                        onSave(song)
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StageYellow,
                    contentColor = PitchBlack,
                    disabledContainerColor = SurfaceCardBorder,
                    disabledContentColor = TextMuted
                )
            ) {
                Text("Salvar", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        },
        containerColor = SurfaceCardElevated
    )
}
