package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.PlaybackState
import com.example.data.model.SetlistEntity
import com.example.data.model.SetlistItemEntity
import com.example.data.model.SongEntity
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

enum class SlimTab {
    BIBLIOTECA,
    REPERTORIO,
    SALVOS
}

@Composable
fun SlimStageScreen(
    viewModel: MainViewModel,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.allSongs.collectAsState()
    val setlists by viewModel.allSetlists.collectAsState()
    val activeSetlist by viewModel.activeSetlist.collectAsState()
    val liveItems by viewModel.liveItems.collectAsState()
    val historyList by viewModel.allHistory.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val playbackState by viewModel.audioEngine.playbackState.collectAsState()
    val activeSong by viewModel.audioEngine.activeSong.collectAsState()

    var activeTab by remember { mutableStateOf(SlimTab.REPERTORIO) }
    val scope = rememberCoroutineScope()

    // File pickers
    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importAudioFile(it, "Local") }
    }

    val syncFolderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = uri.lastPathSegment ?: "Google Drive / VS"
            scope.launch {
                viewModel.repository.saveSettings(userSettings.copy(googleDriveFolder = name, isGoogleDriveConnected = true))
                viewModel.showToast("Pasta vinculada: $name. Sincronização offline ativa!")
            }
        }
    }

    // Modals
    var isNewSongOpen by remember { mutableStateOf(false) }
    var isNewSetlistOpen by remember { mutableStateOf(false) }
    var songToEdit by remember { mutableStateOf<SongEntity?>(null) }
    var songToDelete by remember { mutableStateOf<SongEntity?>(null) }
    var isAddPauseOpen by remember { mutableStateOf(false) }
    var isSetlistMenuOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }

    // Repertoire metrics
    val totalSeconds = remember(liveItems) { liveItems.sumOf { it.durationSeconds } }
    val totalMinutes = (totalSeconds / 60).coerceAtLeast(1)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PitchBlack)
            .statusBarsPadding()
    ) {
        // 1. Top Sleek Header matching reference
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Official VS+ PRO Logo Image
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onOpenDrawer() }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_vs_pro),
                    contentDescription = "Logo VS+ PRO",
                    modifier = Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Fit
                )
            }

            // Right: BIBLIO count, MANUAL button, and User Avatar (D)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // BIBLIO Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "BIBLIO: ", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${songs.size}", color = GoldYellow, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }



                // User Avatar (D) in Yellow Circle
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(GoldYellow)
                        .clickable { onOpenDrawer() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userSettings.userName.firstOrNull()?.uppercase() ?: "D",
                        color = PitchBlack,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // 2. Navigation Pills: BIBLIOTECA | REPERTÓRIO (X) | SALVOS (Y)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SurfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // BIBLIOTECA
                    NavPill(
                        text = "BIBLIOTECA",
                        isSelected = activeTab == SlimTab.BIBLIOTECA,
                        onClick = { activeTab = SlimTab.BIBLIOTECA }
                    )

                    // REPERTÓRIO (X)
                    NavPill(
                        text = "REPERTÓRIO (${liveItems.size})",
                        isSelected = activeTab == SlimTab.REPERTORIO,
                        onClick = { activeTab = SlimTab.REPERTORIO }
                    )

                    // SALVOS (Y)
                    NavPill(
                        text = "SALVOS (${historyList.size.coerceAtLeast(1)})",
                        isSelected = activeTab == SlimTab.SALVOS,
                        onClick = { activeTab = SlimTab.SALVOS }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 3. Main Slim Stage Card Container
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 10.dp),
            shape = RoundedCornerShape(16.dp),
            color = SurfaceCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
        ) {
            when (activeTab) {
                SlimTab.REPERTORIO -> {
                    SlimRepertoireContent(
                        viewModel = viewModel,
                        activeSetlist = activeSetlist,
                        setlists = setlists,
                        liveItems = liveItems,
                        totalMinutes = totalMinutes,
                        onAddPause = { isAddPauseOpen = true },
                        onNewSetlist = { isNewSetlistOpen = true },
                        isSearchVisible = isSearchVisible,
                        searchQuery = searchQuery,
                        onToggleSearch = { isSearchVisible = !isSearchVisible },
                        onSearchChange = { searchQuery = it }
                    )
                }
                SlimTab.BIBLIOTECA -> {
                    SlimLibraryContent(
                        viewModel = viewModel,
                        songs = songs,
                        onNewSong = { isNewSongOpen = true },
                        onImportAudio = { audioPicker.launch(arrayOf("audio/*", "*/*")) },
                        onSyncDrive = { syncFolderPicker.launch(null) },
                        onEditSong = { songToEdit = it },
                        onDeleteSong = { songToDelete = it }
                    )
                }
                SlimTab.SALVOS -> {
                    SlimSavedContent(viewModel = viewModel)
                }
            }
        }

        // 4. Slim Fixed Stage Bottom Player
        SlimBottomPlayerBar(
            viewModel = viewModel,
            playbackState = playbackState,
            activeSong = activeSong,
            onOpenFullStage = { viewModel.navigateTo(AppScreen.STAGE_SHOW) }
        )
    }

    // Modal: New Song Form
    if (isNewSongOpen || songToEdit != null) {
        val editing = songToEdit
        SongFormDialog(
            initialSong = editing,
            onDismiss = {
                isNewSongOpen = false
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
                isNewSongOpen = false
                songToEdit = null
            }
        )
    }

    // Modal: Delete Song -> Moves to Trash
    if (songToDelete != null) {
        AlertDialog(
            onDismissRequest = { songToDelete = null },
            title = { Text("Mover Música para Lixeira?", color = TextPrimary) },
            text = { Text("Mover '${songToDelete?.title}' para a lixeira? Ela poderá ser restaurada a qualquer momento.", color = TextSecondary) },
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

    // Modal: New Setlist
    if (isNewSetlistOpen) {
        var setlistName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { isNewSetlistOpen = false },
            title = { Text("Novo Repertório", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = setlistName,
                    onValueChange = { setlistName = it },
                    label = { Text("Nome do Repertório") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (setlistName.isNotBlank()) {
                            scope.launch {
                                val id = viewModel.repository.createSetlist(setlistName)
                                val created = viewModel.repository.getSetlistById(id)
                                created?.let { viewModel.selectSetlist(it) }
                                viewModel.showToast("Repertório criado!")
                            }
                            isNewSetlistOpen = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldYellow)
                ) {
                    Text("Criar", color = PitchBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isNewSetlistOpen = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = SurfaceCardElevated
        )
    }

    // Modal: Add Pause
    if (isAddPauseOpen) {
        var pauseTitle by remember { mutableStateOf("INTERVALO - 15 MINUTOS") }
        var pauseMins by remember { mutableStateOf("15") }

        AlertDialog(
            onDismissRequest = { isAddPauseOpen = false },
            title = { Text("Inserir Pausa / Intervalo", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pauseTitle,
                        onValueChange = { pauseTitle = it },
                        label = { Text("Identificação (ex: MOMENTO DE FALA)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pauseMins,
                        onValueChange = { pauseMins = it },
                        label = { Text("Duração estimada (minutos)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val setlistId = activeSetlist?.id ?: return@Button
                        val mins = pauseMins.toIntOrNull() ?: 5
                        scope.launch {
                            viewModel.repository.addPause(setlistId, pauseTitle, mins, "")
                            viewModel.showToast("Pausa adicionada ao repertório!")
                        }
                        isAddPauseOpen = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldYellow)
                ) {
                    Text("Adicionar", color = PitchBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddPauseOpen = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = SurfaceCardElevated
        )
    }
}

@Composable
fun NavPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) GoldYellow else Color.Transparent,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            color = if (isSelected) PitchBlack else TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun SlimRepertoireContent(
    viewModel: MainViewModel,
    activeSetlist: SetlistEntity?,
    setlists: List<SetlistEntity>,
    liveItems: List<SetlistItemEntity>,
    totalMinutes: Int,
    onAddPause: () -> Unit,
    onNewSetlist: () -> Unit,
    isSearchVisible: Boolean,
    searchQuery: String,
    onToggleSearch: () -> Unit,
    onSearchChange: (String) -> Unit
) {
    val userSettings by viewModel.userSettings.collectAsState()
    val isShowActive by viewModel.isShowActive.collectAsState()
    val playedItemIds by viewModel.playedItemIds.collectAsState()
    val playedSongIds by viewModel.playedSongIds.collectAsState()
    val scope = rememberCoroutineScope()
    var isDropdownOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        // Repertoire Selector & Action Icons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dropdown Selector Button
            Box {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceCardElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                    modifier = Modifier.clickable { isDropdownOpen = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activeSetlist?.name ?: "Meu repertório",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }

                DropdownMenu(
                    expanded = isDropdownOpen,
                    onDismissRequest = { isDropdownOpen = false }
                ) {
                    setlists.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(s.name) },
                            onClick = {
                                viewModel.selectSetlist(s)
                                isDropdownOpen = false
                            }
                        )
                    }
                    Divider()
                    DropdownMenuItem(
                        text = { Text("+ Criar Novo Repertório", color = GoldYellow) },
                        onClick = {
                            isDropdownOpen = false
                            onNewSetlist()
                        }
                    )
                }
            }

            // Right Actions: [+] [✏] [🗑] [⧉]
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onNewSetlist,
                    modifier = Modifier
                        .size(30.dp)
                        .background(SurfaceCardElevated, RoundedCornerShape(6.dp))
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Novo", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }

                IconButton(
                    onClick = {
                        val s = activeSetlist ?: return@IconButton
                        scope.launch {
                            val newId = viewModel.repository.duplicateSetlist(s.id)
                            viewModel.repository.getSetlistById(newId)?.let { viewModel.selectSetlist(it) }
                            viewModel.showToast("Repertório duplicado!")
                        }
                    },
                    modifier = Modifier
                        .size(30.dp)
                        .background(SurfaceCardElevated, RoundedCornerShape(6.dp))
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Duplicar", tint = TextSecondary, modifier = Modifier.size(15.dp))
                }

                IconButton(
                    onClick = {
                        val id = activeSetlist?.id ?: return@IconButton
                        scope.launch {
                            viewModel.repository.deleteSetlist(id)
                            viewModel.showToast("Repertório excluído.")
                        }
                    },
                    modifier = Modifier
                        .size(30.dp)
                        .background(SurfaceCardElevated, RoundedCornerShape(6.dp))
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir", tint = TextSecondary, modifier = Modifier.size(15.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sub Toolbar: Cloud, INICIAR SHOW, Search, Pausa, Trash
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val playbackState by viewModel.audioEngine.playbackState.collectAsState()
            val isPlaying = playbackState == PlaybackState.PLAYING

            // Cloud Icon
            Icon(
                imageVector = Icons.Default.CloudQueue,
                contentDescription = "Cloud",
                tint = GoldYellow,
                modifier = Modifier.size(20.dp)
            )

            // INICIAR SHOW or SHOW CONTROLS
            if (!isShowActive) {
                Button(
                    onClick = {
                        activeSetlist?.let { viewModel.startLiveShow(it) }
                            ?: run {
                                if (setlists.isNotEmpty()) viewModel.startLiveShow(setlists.first())
                            }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldYellow, contentColor = PitchBlack),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = "INICIAR SHOW",
                        color = PitchBlack,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = {
                            if (isPlaying) viewModel.pauseShow() else viewModel.resumeShow()
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StageYellow, contentColor = PitchBlack),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = if (isPlaying) "PAUSAR" else "RETOMAR",
                            color = PitchBlack,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        )
                    }

                    Button(
                        onClick = { viewModel.stopAndFinishShow() },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PanicRed, contentColor = TextPrimary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = "ENCERRAR",
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Search Icon in Circle
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SurfaceCardElevated)
                    .border(1.dp, GoldYellow, CircleShape)
                    .clickable { onToggleSearch() },
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Pesquisar", tint = GoldYellow, modifier = Modifier.size(16.dp))
            }

            // Pausa Button (Yellow Outlined Pill)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = PitchBlack,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldYellow),
                modifier = Modifier.clickable { onAddPause() }
            ) {
                Text(
                    text = "Pausa",
                    color = GoldYellow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }

            // Trash Button -> Navigates to Lixeira
            IconButton(
                onClick = { viewModel.navigateTo(AppScreen.TRASH) },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Lixeira", tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
        }

        // Search Input (Collapsible)
        AnimatedVisibility(visible = isSearchVisible) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Filtrar músicas do show...", fontSize = 11.sp, color = TextMuted) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldYellow,
                    unfocusedBorderColor = SurfaceCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Stats & Stepper Row: "5 MÚSICAS • 25MIN", Stepper "- 2s +", Segmented "[Continuo | 1x1]"
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Count & Total Min
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "${liveItems.size} ", color = GoldYellow, fontWeight = FontWeight.Black, fontSize = 12.sp)
                Text(text = "MÚSICAS • ", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                Text(text = "${totalMinutes}MIN", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }

            // Stepper [- 2s +]
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = PitchBlack,
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = " - ",
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clickable {
                                val current = userSettings.intervalSeconds
                                if (current > 0) {
                                    scope.launch { viewModel.repository.saveSettings(userSettings.copy(intervalSeconds = current - 1)) }
                                }
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Text(
                        text = "${userSettings.intervalSeconds}s",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Text(
                        text = " + ",
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clickable {
                                val current = userSettings.intervalSeconds
                                scope.launch { viewModel.repository.saveSettings(userSettings.copy(intervalSeconds = current + 1)) }
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Segmented Mode Toggle: [ Continuo | 1x1 ]
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = PitchBlack,
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Row(modifier = Modifier.padding(2.dp)) {
                    // Continuo
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (userSettings.isContinuousMode) GoldYellow else Color.Transparent,
                        modifier = Modifier.clickable { viewModel.updateContinuousMode(true) }
                    ) {
                        Text(
                            text = "Continuo",
                            color = if (userSettings.isContinuousMode) PitchBlack else TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // 1x1
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (!userSettings.isContinuousMode) GoldYellow else Color.Transparent,
                        modifier = Modifier.clickable { viewModel.updateContinuousMode(false) }
                    ) {
                        Text(
                            text = "1x1",
                            color = if (!userSettings.isContinuousMode) PitchBlack else TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Slim Track List
        val filteredItems = remember(liveItems, searchQuery) {
            if (searchQuery.isBlank()) liveItems
            else liveItems.filter { it.customTitle.contains(searchQuery, ignoreCase = true) }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(filteredItems) { index, item ->
                val isPlayed = isShowActive && (playedItemIds.contains(item.id) || (item.songId != null && playedSongIds.contains(item.songId)))
                SlimTrackRow(
                    item = item,
                    index = index,
                    totalItems = filteredItems.size,
                    isPlayed = isPlayed,
                    onPlay = {
                        viewModel.requestPlayLiveItemAtIndex(index)
                    },
                    onMoveUp = {
                        val setlistId = activeSetlist?.id ?: return@SlimTrackRow
                        scope.launch { viewModel.repository.moveItem(setlistId, index, index - 1) }
                    },
                    onMoveDown = {
                        val setlistId = activeSetlist?.id ?: return@SlimTrackRow
                        scope.launch { viewModel.repository.moveItem(setlistId, index, index + 1) }
                    },
                    onDelete = {
                        viewModel.moveItemToTrash(item)
                    }
                )
            }
        }

        // Bottom Summary: "5 música(s) no repertório" | "25MIN"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${liveItems.size} música(s) no repertório",
                color = TextMuted,
                fontSize = 11.sp
            )
            Text(
                text = "${totalMinutes}MIN",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun SlimTrackRow(
    item: SetlistItemEntity,
    index: Int,
    totalItems: Int,
    isPlayed: Boolean = false,
    onPlay: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    val durationFormatted = remember(item.durationSeconds) {
        val mins = item.durationSeconds / 60
        val secs = item.durationSeconds % 60
        String.format("%d:%02d", mins, secs)
    }

    val rowBg = if (isPlayed) LilacPlayedBg else CardItemBg
    val rowBorder = if (isPlayed) LilacPlayedBorder else CardItemBorder

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = rowBg,
        border = androidx.compose.foundation.BorderStroke(if (isPlayed) 1.5.dp else 1.dp, rowBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Group: Up/Down reorder + Cyan index + Play button + Title / Subtitle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Reorder controls Up/Down
                Column(verticalArrangement = Arrangement.Center) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = index > 0,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropUp,
                            contentDescription = "Subir",
                            tint = if (index > 0) TextPrimary else TextMuted.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = index < totalItems - 1,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Descer",
                            tint = if (index < totalItems - 1) TextPrimary else TextMuted.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Track Number (Cyan Accent: 01, 02...)
                Text(
                    text = String.format("%02d", index + 1),
                    color = if (isPlayed) LilacPlayed else CyanAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(end = 6.dp)
                )

                // Play Button Circle
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isPlayed) LilacPlayed else SurfaceCardElevated)
                        .clickable { onPlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Tocar",
                        tint = if (isPlayed) PitchBlack else TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Title and Folder Subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.customTitle,
                            color = if (isPlayed) LilacPlayedGlow else GoldYellow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isPlayed) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = LilacPlayed,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "TOCADA",
                                    color = PitchBlack,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = GoldYellowDark,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (item.notes.isNotBlank()) item.notes else "VS S7/${index + 1} • ${item.customTitle}.wav",
                            color = TextMuted,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Right Group: Duration (Cyan) and Remove to Trash Button (x)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = durationFormatted,
                    color = CyanAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Mover para Lixeira",
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SlimLibraryContent(
    viewModel: MainViewModel,
    songs: List<SongEntity>,
    onNewSong: () -> Unit,
    onImportAudio: () -> Unit,
    onSyncDrive: () -> Unit,
    onEditSong: (SongEntity) -> Unit,
    onDeleteSong: (SongEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredSongs = remember(songs, searchQuery) {
        if (searchQuery.isBlank()) songs
        else songs.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        // Action Buttons Row: SINCRONIZAR ÁUDIOS | + NOVA MÚSICA | [⬆] | [🕒]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // SINCRONIZAR ÁUDIOS (Gold outlined pill)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = PitchBlack,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldYellow),
                modifier = Modifier
                    .weight(1.1f)
                    .clickable { onSyncDrive() }
            ) {
                Text(
                    text = "SINCRONIZAR ÁUDIOS",
                    color = GoldYellow,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                    maxLines = 1
                )
            }

            // + NOVA MÚSICA (Yellow Filled Pill)
            Button(
                onClick = onNewSong,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoldYellow),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier
                    .weight(1.1f)
                    .height(34.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = PitchBlack, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "NOVA MÚSICA",
                    color = PitchBlack,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }

            // Upload Button
            IconButton(
                onClick = onImportAudio,
                modifier = Modifier
                    .size(32.dp)
                    .background(SurfaceCardElevated, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.FileUpload, contentDescription = "Importar", tint = TextPrimary, modifier = Modifier.size(16.dp))
            }

            // History Button
            IconButton(
                onClick = { viewModel.navigateTo(AppScreen.HISTORY) },
                modifier = Modifier
                    .size(32.dp)
                    .background(SurfaceCardElevated, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.History, contentDescription = "Histórico", tint = TextPrimary, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar with [⇅ A-Z] and [🎤]
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar músicas...", fontSize = 11.sp, color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp)) },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldYellow,
                    unfocusedBorderColor = SurfaceCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = PitchBlack,
                    unfocusedContainerColor = PitchBlack
                )
            )

            // Sort Button [⇅ A-Z]
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SurfaceCardElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.SwapVert, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(14.dp))
                    Text(text = "A-Z", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Voice Search Mic Button
            IconButton(
                onClick = { viewModel.showToast("Escutando comando de voz...") },
                modifier = Modifier
                    .size(40.dp)
                    .background(SurfaceCardElevated, RoundedCornerShape(8.dp))
            ) {
                Icon(imageVector = Icons.Default.Mic, contentDescription = "Voz", tint = TextPrimary, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Status Row: "5 músicas carregadas" | "Limpar Tudo"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "${filteredSongs.size} músicas carregadas", color = TextMuted, fontSize = 11.sp)
            Text(
                text = "Limpar Tudo",
                color = TextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.clickable { viewModel.showToast("Para apagar, use o ícone de cada música.") }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val isShowActive by viewModel.isShowActive.collectAsState()
        val playedSongIds by viewModel.playedSongIds.collectAsState()

        // Slim Library Song Rows
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(filteredSongs) { index, song ->
                val isPlayed = isShowActive && playedSongIds.contains(song.id)
                SlimLibraryRow(
                    song = song,
                    index = index,
                    isPlayed = isPlayed,
                    onPlay = { viewModel.requestPlaySong(song) },
                    onToggleFavorite = { viewModel.toggleFavorite(song) },
                    onEdit = { onEditSong(song) },
                    onDelete = { onDeleteSong(song) }
                )
            }
        }
    }
}

@Composable
fun SlimLibraryRow(
    song: SongEntity,
    index: Int,
    isPlayed: Boolean = false,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val durationFormatted = remember(song.durationMs) {
        val totalSec = song.durationMs / 1000
        val mins = totalSec / 60
        val secs = totalSec % 60
        String.format("%d:%02d", mins, secs)
    }

    val rowBg = if (isPlayed) LilacPlayedBg else CardItemBg
    val rowBorder = if (isPlayed) LilacPlayedBorder else CardItemBorder

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = rowBg,
        border = androidx.compose.foundation.BorderStroke(if (isPlayed) 1.5.dp else 1.dp, rowBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Group: Cyan index + Play button + Title / Subtitle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Cyan Number
                Text(
                    text = String.format("%02d", index + 1),
                    color = if (isPlayed) LilacPlayed else CyanAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(end = 8.dp)
                )

                // Play Button Circle
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isPlayed) LilacPlayed else SurfaceCardElevated)
                        .clickable { onPlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Tocar",
                        tint = if (isPlayed) PitchBlack else TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = song.title,
                            color = if (isPlayed) LilacPlayedGlow else GoldYellow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isPlayed) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = LilacPlayed,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "TOCADA",
                                    color = PitchBlack,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = GoldYellowDark,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (song.notes.isNotBlank()) song.notes else "VS S7/${index + 1} • ${song.artist.ifBlank { "Artista" }}.wav",
                            color = TextMuted,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Right Group: Edit [✏], Star [⭐], Duration (Cyan), Delete [🗑]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favoritar",
                        tint = if (song.isFavorite) StageYellow else TextMuted,
                        modifier = Modifier.size(15.dp)
                    )
                }

                IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar", tint = TextSecondary, modifier = Modifier.size(14.dp))
                }

                Text(
                    text = durationFormatted,
                    color = CyanAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Mover para Lixeira", tint = PanicRed.copy(alpha = 0.8f), modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

@Composable
fun SlimSavedContent(
    viewModel: MainViewModel
) {
    val historyList by viewModel.allHistory.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Shows Salvos & Histórico",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(historyList) { index, h ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = CardItemBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardItemBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = h.showName, color = GoldYellow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = "Repertório: ${h.setlistName} • ${h.songsPlayedCount} faixas tocadas", color = TextSecondary, fontSize = 11.sp)
                        }
                        IconButton(
                            onClick = {
                                scope.launch { viewModel.repository.deleteHistory(h.id) }
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Apagar", tint = TextMuted, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SlimBottomPlayerBar(
    viewModel: MainViewModel,
    playbackState: PlaybackState,
    activeSong: SongEntity?,
    onOpenFullStage: () -> Unit
) {
    val isPlaying = playbackState == PlaybackState.PLAYING
    val currentPositionMs by viewModel.audioEngine.currentPositionMs.collectAsState()
    val durationMs by viewModel.audioEngine.durationMs.collectAsState()
    val isShowActive by viewModel.isShowActive.collectAsState()

    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }

    val currentProgress = if (durationMs > 0) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val effectiveSliderValue = if (isDraggingSlider) sliderPosition else currentProgress

    val curMins = (currentPositionMs / 1000) / 60
    val curSecs = (currentPositionMs / 1000) % 60
    val totalMins = (durationMs / 1000) / 60
    val totalSecs = (durationMs / 1000) % 60
    val timeText = String.format("%d:%02d / %d:%02d", curMins, curSecs, totalMins, totalSecs)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 10.dp, end = 10.dp, bottom = 14.dp),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCardElevated,
        tonalElevation = 10.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isShowActive) LilacPlayedBorder else SurfaceCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Live Show Quick Bar if active
            if (isShowActive) {
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
                            color = LilacPlayed
                        ) {
                            Text(
                                text = "SHOW ATIVO",
                                color = PitchBlack,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPlaying) "No Ar" else "Pausado",
                            color = if (isPlaying) StageGreen else WarningAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PitchBlack,
                            border = androidx.compose.foundation.BorderStroke(1.dp, StageYellow),
                            modifier = Modifier.clickable {
                                if (isPlaying) viewModel.pauseShow() else viewModel.resumeShow()
                            }
                        ) {
                            Text(
                                text = if (isPlaying) "Pausar" else "Retomar",
                                color = StageYellow,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PitchBlack,
                            border = androidx.compose.foundation.BorderStroke(1.dp, PanicRed),
                            modifier = Modifier.clickable { viewModel.stopAndFinishShow() }
                        ) {
                            Text(
                                text = "Encerrar",
                                color = PanicRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Interactive Scrubber Slider Row
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
                        thumbColor = GoldYellow,
                        activeTrackColor = GoldYellow,
                        inactiveTrackColor = SurfaceCardBorder
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = timeText,
                    color = CyanAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Controls & Song Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Play / Pause Circle Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) GoldYellow else SurfaceCard)
                            .clickable { viewModel.requestPlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Disparar",
                            tint = if (isPlaying) PitchBlack else TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isPlaying) StageGreen else WarningAmber)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isPlaying) "REPRODUZINDO NO PA E FONES" else "SISTEMA PRONTO PARA O SHOW",
                                color = if (isPlaying) StageGreen else TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Text(
                            text = activeSong?.let { "${it.title} - ${it.artist}" } ?: "Toque em qualquer música da lista para disparar o VS",
                            color = if (activeSong != null) GoldYellow else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right icons: -10s, +10s, Volume & Panic button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.seekRelative(-10_000L) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Voltar 10s",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.seekRelative(10_000L) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Avançar 10s",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.STEREO_SPLIT) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Volume",
                            tint = GoldYellow,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.triggerPanic() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Pânico",
                            tint = PanicRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
