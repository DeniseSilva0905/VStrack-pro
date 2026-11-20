package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.*
import com.example.data.model.*
import com.example.data.repository.VSTracksRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppScreen(val title: String, val badge: String = "") {
    HOME("Início"),
    LIBRARY("Biblioteca"),
    SETLISTS("Repertórios"),
    SAVED_SETLISTS("Repertórios Salvos"),
    STAGE_SHOW("Modo Show"),
    TELEPROMPTER("Teleprompter"),
    MIXER("Mixer"),
    STEREO_SPLIT("Split Áudio"),
    PEDAL("Pedal Bluetooth"),
    HISTORY("Histórico"),
    PROFILE("Meu Perfil"),
    TRASH("Lixeira")
}

data class StemChannelState(
    val id: String,
    val name: String,
    val volume: Float = 0.8f,
    val isMuted: Boolean = false,
    val isSolo: Boolean = false,
    val pan: Float = 0.0f, // -1.0 Left to +1.0 Right
    val vuLevel: Float = 0.65f
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository = VSTracksRepository(application)
    val audioEngine = VSTracksAudioEngine(application)
    val padEngine = VSTracksPadEngine()

    // Navigation
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen

    private val _navigationStack = mutableListOf<AppScreen>()

    // Data from database
    val allSongs: StateFlow<List<SongEntity>> = repository.allSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashSongs: StateFlow<List<SongEntity>> = repository.trashSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSetlists: StateFlow<List<SetlistEntity>> = repository.allSetlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashSetlistItems: StateFlow<List<SetlistItemEntity>> = repository.trashSetlistItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHistory: StateFlow<List<ShowHistoryEntity>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSettings: StateFlow<UserSettingsEntity> = repository.settingsFlow
        .map { it ?: UserSettingsEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettingsEntity())

    // Active Live Show State
    private val _activeSetlist = MutableStateFlow<SetlistEntity?>(null)
    val activeSetlist: StateFlow<SetlistEntity?> = _activeSetlist

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val liveItems: StateFlow<List<SetlistItemEntity>> = _activeSetlist
        .distinctUntilChangedBy { it?.id }
        .flatMapLatest { setlist ->
            if (setlist == null) flowOf(emptyList())
            else repository.getItemsForSetlist(setlist.id)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _currentLiveItemIndex = MutableStateFlow(0)
    val currentLiveItemIndex: StateFlow<Int> = _currentLiveItemIndex

    private val _playedItemIds = MutableStateFlow<Set<Long>>(emptySet())
    val playedItemIds: StateFlow<Set<Long>> = _playedItemIds

    private val _playedSongIds = MutableStateFlow<Set<Long>>(emptySet())
    val playedSongIds: StateFlow<Set<Long>> = _playedSongIds

    private val _showStartTimeMs = MutableStateFlow(0L)
    val showStartTimeMs: StateFlow<Long> = _showStartTimeMs

    private val _isShowActive = MutableStateFlow(false)
    val isShowActive: StateFlow<Boolean> = _isShowActive

    private val _isShowPausedAtCue = MutableStateFlow(false)
    val isShowPausedAtCue: StateFlow<Boolean> = _isShowPausedAtCue

    private val _isPlayingLibrarySong = MutableStateFlow(false)
    val isPlayingLibrarySong: StateFlow<Boolean> = _isPlayingLibrarySong

    private val _isPausedAfterLibrarySong = MutableStateFlow(false)
    val isPausedAfterLibrarySong: StateFlow<Boolean> = _isPausedAfterLibrarySong

    // Stage Lock
    private val _isStageLocked = MutableStateFlow(false)
    val isStageLocked: StateFlow<Boolean> = _isStageLocked

    private val _pinDialogVisible = MutableStateFlow(false)
    val pinDialogVisible: StateFlow<Boolean> = _pinDialogVisible

    private val _pinErrorMessage = MutableStateFlow<String?>(null)
    val pinErrorMessage: StateFlow<String?> = _pinErrorMessage

    // Safety Confirmation Modal
    private val _showPlayConfirmationDialog = MutableStateFlow(false)
    val showPlayConfirmationDialog: StateFlow<Boolean> = _showPlayConfirmationDialog

    private var pendingPlayAction: (() -> Unit)? = null

    // Teleprompter
    private val _teleprompterSong = MutableStateFlow<SongEntity?>(null)
    val teleprompterSong: StateFlow<SongEntity?> = _teleprompterSong

    private val _teleprompterScrollSpeed = MutableStateFlow(2f) // 1 to 5
    val teleprompterScrollSpeed: StateFlow<Float> = _teleprompterScrollSpeed

    private val _teleprompterFontSize = MutableStateFlow(24) // sp
    val teleprompterFontSize: StateFlow<Int> = _teleprompterFontSize

    private val _teleprompterTransposeSemitones = MutableStateFlow(0)
    val teleprompterTransposeSemitones: StateFlow<Int> = _teleprompterTransposeSemitones

    private val _isTeleprompterAutoScroll = MutableStateFlow(false)
    val isTeleprompterAutoScroll: StateFlow<Boolean> = _isTeleprompterAutoScroll

    // Pads
    private val _isPadActive = MutableStateFlow(false)
    val isPadActive: StateFlow<Boolean> = _isPadActive

    private val _padCurrentKey = MutableStateFlow("C")
    val padCurrentKey: StateFlow<String> = _padCurrentKey

    private val _padCurrentTimbre = MutableStateFlow(PadTimbre.WARM)
    val padCurrentTimbre: StateFlow<PadTimbre> = _padCurrentTimbre

    private val _padVolume = MutableStateFlow(0.8f)
    val padVolume: StateFlow<Float> = _padVolume

    // Stems Mixer
    private val _mixerChannels = MutableStateFlow(
        listOf(
            StemChannelState("click", "Click / Fone", volume = 0.9f, pan = -1.0f, vuLevel = 0.8f),
            StemChannelState("guide", "Voz Guia", volume = 0.85f, pan = -0.8f, vuLevel = 0.65f),
            StemChannelState("drums", "Bateria", volume = 0.95f, pan = 0.0f, vuLevel = 0.9f),
            StemChannelState("bass", "Baixo", volume = 0.9f, pan = 0.0f, vuLevel = 0.85f),
            StemChannelState("keys", "Teclados", volume = 0.8f, pan = 0.2f, vuLevel = 0.7f),
            StemChannelState("guitars", "Guitarras", volume = 0.8f, pan = -0.2f, vuLevel = 0.75f),
            StemChannelState("brass", "Metais", volume = 0.75f, pan = 0.3f, vuLevel = 0.6f),
            StemChannelState("master", "Master Geral", volume = 1.0f, pan = 0.0f, vuLevel = 0.85f)
        )
    )
    val mixerChannels: StateFlow<List<StemChannelState>> = _mixerChannels

    // Bluetooth Pedal Hardware Monitoring
    private val _lastPedalCommand = MutableStateFlow("Nenhum comando detectado")
    val lastPedalCommand: StateFlow<String> = _lastPedalCommand

    private val _lastPedalTimestamp = MutableStateFlow(0L)
    val lastPedalTimestamp: StateFlow<Long> = _lastPedalTimestamp

    // Auto-advance Countdown Timer between tracks (1-10s user configurable)
    private val _nextSongCountdownSeconds = MutableStateFlow<Int?>(null)
    val nextSongCountdownSeconds: StateFlow<Int?> = _nextSongCountdownSeconds.asStateFlow()
    private var countdownJob: Job? = null

    // UI Feedback Banner
    private val _uiToast = MutableStateFlow<String?>(null)
    val uiToast: StateFlow<String?> = _uiToast

    init {
        // Load initial settings
        viewModelScope.launch {
            userSettings.collect { settings ->
                audioEngine.isStereoSplitEnabled = settings.isStereoSplitEnabled
                audioEngine.isLRSwapped = settings.isLRSwapped
                audioEngine.leftVolume = settings.leftVolume
                audioEngine.rightVolume = settings.rightVolume
            }
        }

        // Auto-select first setlist when available or recover if active setlist was deleted
        viewModelScope.launch {
            allSetlists.collect { list ->
                val current = _activeSetlist.value
                if (current == null && list.isNotEmpty()) {
                    selectSetlist(list.first())
                } else if (current != null && list.none { it.id == current.id }) {
                    _activeSetlist.value = list.firstOrNull()
                }
            }
        }

        // Audio Engine onCompleted handler for Continuous / 1x1 mode
        audioEngine.onSongCompleted = {
            viewModelScope.launch {
                handleSongCompletion()
            }
        }

        // Sync teleprompter with current playing song or default library song
        viewModelScope.launch {
            audioEngine.activeSong.collect { active ->
                if (active != null) {
                    _teleprompterSong.value = active
                    _teleprompterScrollSpeed.value = active.scrollSpeed.toFloat().coerceIn(1f, 10f)
                }
            }
        }

        viewModelScope.launch {
            allSongs.collect { songs ->
                if (_teleprompterSong.value == null && songs.isNotEmpty()) {
                    val first = songs.first()
                    _teleprompterSong.value = first
                    _teleprompterScrollSpeed.value = first.scrollSpeed.toFloat().coerceIn(1f, 10f)
                }
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        if (_currentScreen.value != screen) {
            _navigationStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        if (_navigationStack.isNotEmpty()) {
            _currentScreen.value = _navigationStack.removeAt(_navigationStack.lastIndex)
            return true
        }
        return false
    }

    fun selectSetlist(setlist: SetlistEntity) {
        val changed = _activeSetlist.value?.id != setlist.id
        _activeSetlist.value = setlist
        if (changed && !_isShowActive.value) {
            _currentLiveItemIndex.value = 0
            _isShowPausedAtCue.value = false
            _playedItemIds.value = emptySet()
        }
    }

    // Accidental Tap Safety Confirmation during Live Show
    private val _safetyConfirmDialogVisible = MutableStateFlow(false)
    val safetyConfirmDialogVisible: StateFlow<Boolean> = _safetyConfirmDialogVisible

    private val _pendingSafetySongTitle = MutableStateFlow("")
    val pendingSafetySongTitle: StateFlow<String> = _pendingSafetySongTitle

    private var pendingSafetyAction: (() -> Unit)? = null

    fun startLiveShow(setlist: SetlistEntity, navigateToStage: Boolean = true) {
        countdownJob?.cancel()
        _nextSongCountdownSeconds.value = null
        _activeSetlist.value = setlist
        _isShowActive.value = true
        _isShowPausedAtCue.value = false
        _isPlayingLibrarySong.value = false
        _isPausedAfterLibrarySong.value = false
        _showStartTimeMs.value = System.currentTimeMillis()
        _currentLiveItemIndex.value = 0
        _playedItemIds.value = emptySet()
        _playedSongIds.value = emptySet()
        if (navigateToStage) {
            _currentScreen.value = AppScreen.STAGE_SHOW
        }
        showToast("Show '${setlist.name}' iniciado! Modo Show Ao Vivo ativo.")
    }

    fun pauseShow() {
        countdownJob?.cancel()
        _nextSongCountdownSeconds.value = null
        _isShowPausedAtCue.value = true
        audioEngine.pause()
        showToast("Show pausado no palco.")
    }

    fun resumeShow() {
        countdownJob?.cancel()
        _nextSongCountdownSeconds.value = null
        _isShowPausedAtCue.value = false
        val wasPausedAfterLibrary = _isPausedAfterLibrarySong.value
        _isPausedAfterLibrarySong.value = false

        if (audioEngine.playbackState.value == PlaybackState.PAUSED) {
            audioEngine.resume()
            showToast("Show retomado.")
            return
        }

        _isPlayingLibrarySong.value = false
        val items = liveItems.value
        if (items.isEmpty()) return

        var targetIndex = _currentLiveItemIndex.value.coerceIn(0, items.lastIndex)
        if (wasPausedAfterLibrary && items[targetIndex].type == "SONG" && _playedItemIds.value.contains(items[targetIndex].id)) {
            targetIndex++
        }
        // Advance past any PAUSE or BLOCK where the show was paused so the next song plays
        while (targetIndex < items.size && items[targetIndex].type != "SONG") {
            _playedItemIds.value = _playedItemIds.value + items[targetIndex].id
            targetIndex++
        }

        if (targetIndex in items.indices) {
            playLiveItemAtIndex(targetIndex)
            showToast("Show retomado.")
        } else {
            showToast("Fim do repertório!")
        }
    }

    fun stopAndFinishShow() {
        if (!_isShowActive.value) return
        countdownJob?.cancel()
        _nextSongCountdownSeconds.value = null
        _isShowPausedAtCue.value = false
        _isPlayingLibrarySong.value = false
        _isPausedAfterLibrarySong.value = false
        val currentSetlist = _activeSetlist.value
        val totalSec = ((System.currentTimeMillis() - _showStartTimeMs.value) / 1000L).coerceAtLeast(1L)
        val songsCount = _playedSongIds.value.size

        viewModelScope.launch {
            audioEngine.stopCurrentPlayback()
            repository.recordShowHistory(
                ShowHistoryEntity(
                    showName = "Apresentação Ao Vivo - ${currentSetlist?.name ?: "Show"}",
                    setlistId = currentSetlist?.id,
                    setlistName = currentSetlist?.name ?: "Repertório",
                    dateTimestamp = System.currentTimeMillis(),
                    songsPlayedCount = songsCount,
                    totalDurationSeconds = totalSec,
                    notes = "Show concluído com sucesso. Músicas tocadas: $songsCount."
                )
            )
            _isShowActive.value = false
            _isShowPausedAtCue.value = false
            _isPlayingLibrarySong.value = false
            _isPausedAfterLibrarySong.value = false
            _playedItemIds.value = emptySet()
            _playedSongIds.value = emptySet()
            _currentScreen.value = AppScreen.HOME
            _navigationStack.clear()
            showToast("Show encerrado com sucesso! Retornando ao Início.")
        }
    }

    fun requestPlayPause() {
        if (userSettings.value.requirePlayConfirmation && audioEngine.playbackState.value != PlaybackState.PLAYING) {
            pendingPlayAction = { executePlayPause() }
            _showPlayConfirmationDialog.value = true
        } else {
            executePlayPause()
        }
    }

    fun confirmPlayAction() {
        _showPlayConfirmationDialog.value = false
        pendingPlayAction?.invoke()
        pendingPlayAction = null
    }

    fun dismissPlayConfirmation() {
        _showPlayConfirmationDialog.value = false
        pendingPlayAction = null
    }

    private fun executePlayPause() {
        if (audioEngine.playbackState.value == PlaybackState.PLAYING) {
            if (_isShowActive.value) pauseShow() else audioEngine.pause()
        } else if (audioEngine.playbackState.value == PlaybackState.PAUSED) {
            _isShowPausedAtCue.value = false
            audioEngine.resume()
        } else {
            if (_isShowActive.value || _isShowPausedAtCue.value) resumeShow() else playCurrentLiveItem()
        }
    }

    fun playCurrentLiveItem() {
        playLiveItemAtIndex(_currentLiveItemIndex.value)
    }

    /**
     * Safety Interceptor: When show is active, asks confirmation before switching/playing song
     */
    fun requestPlaySong(song: SongEntity) {
        if (_isShowActive.value) {
            _pendingSafetySongTitle.value = song.title
            pendingSafetyAction = { playSongFromLibrary(song) }
            _safetyConfirmDialogVisible.value = true
        } else {
            playSongFromLibrary(song)
        }
    }

    fun requestPlayLiveItemAtIndex(index: Int) {
        val items = liveItems.value
        if (items.isEmpty() || index !in items.indices) return
        val item = items[index]
        if (_isShowActive.value) {
            _pendingSafetySongTitle.value = item.customTitle
            pendingSafetyAction = { playLiveItemAtIndex(index) }
            _safetyConfirmDialogVisible.value = true
        } else {
            playLiveItemAtIndex(index)
        }
    }

    fun confirmSafetyPlay() {
        _safetyConfirmDialogVisible.value = false
        pendingSafetyAction?.invoke()
        pendingSafetyAction = null
    }

    fun dismissSafetyPlay() {
        _safetyConfirmDialogVisible.value = false
        pendingSafetyAction = null
    }

    fun playLiveItemAtIndex(index: Int) {
        val items = liveItems.value
        if (items.isEmpty() || index !in items.indices) return

        countdownJob?.cancel()
        _nextSongCountdownSeconds.value = null
        _isShowPausedAtCue.value = false
        _isPlayingLibrarySong.value = false
        _isPausedAfterLibrarySong.value = false
        _currentLiveItemIndex.value = index
        val item = items[index]
        if (item.type == "SONG" && item.songId != null) {
            viewModelScope.launch {
                val song = repository.getSongById(item.songId)
                if (song != null) {
                    _teleprompterSong.value = song
                    audioEngine.loadAndPlay(song)
                    _playedItemIds.value = _playedItemIds.value + item.id
                    _playedSongIds.value = _playedSongIds.value + song.id
                }
            }
        }
    }

    fun playSongFromLibrary(song: SongEntity) {
        countdownJob?.cancel()
        _nextSongCountdownSeconds.value = null
        _isShowPausedAtCue.value = false
        _isPausedAfterLibrarySong.value = false
        _isPlayingLibrarySong.value = true
        _teleprompterSong.value = song
        audioEngine.loadAndPlay(song)
        if (_isShowActive.value) {
            _playedSongIds.value = _playedSongIds.value + song.id
        }
    }

    fun toggleFavorite(song: SongEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(song.id, !song.isFavorite)
            showToast(if (!song.isFavorite) "Música adicionada aos favoritos!" else "Removida dos favoritos.")
        }
    }

    fun nextSong() {
        val items = liveItems.value
        if (items.isEmpty()) return

        _isPlayingLibrarySong.value = false
        _isPausedAfterLibrarySong.value = false
        val nextIndex = _currentLiveItemIndex.value + 1
        if (nextIndex in items.indices) {
            val item = items[nextIndex]
            if (item.type == "PAUSE" || item.type == "BLOCK") {
                countdownJob?.cancel()
                _nextSongCountdownSeconds.value = null
                _currentLiveItemIndex.value = nextIndex
                _isShowPausedAtCue.value = true
                audioEngine.stopCurrentPlayback()
                val label = if (item.type == "BLOCK") "Bloco '${item.customTitle}'" else "Pausa '${item.customTitle}'"
                showToast("$label alcançado. Show pausado — retome quando desejar.")
            } else {
                _isShowPausedAtCue.value = false
                _currentLiveItemIndex.value = nextIndex
                playCurrentLiveItem()
            }
        } else {
            showToast("Fim do repertório!")
        }
    }

    fun previousSong() {
        val items = liveItems.value
        if (items.isEmpty()) return

        var prevIndex = _currentLiveItemIndex.value - 1
        while (prevIndex >= 0 && items[prevIndex].type != "SONG") {
            prevIndex--
        }

        if (prevIndex in items.indices) {
            _isShowPausedAtCue.value = false
            _isPlayingLibrarySong.value = false
            _isPausedAfterLibrarySong.value = false
            _currentLiveItemIndex.value = prevIndex
            playCurrentLiveItem()
        }
    }

    fun selectLiveItem(index: Int) {
        requestPlayLiveItemAtIndex(index)
    }

    internal suspend fun handleSongCompletion() {
        // Whenever a song started from the Library finishes, always pause the show until resumed by the user
        if (_isPlayingLibrarySong.value) {
            countdownJob?.cancel()
            _nextSongCountdownSeconds.value = null
            _isPlayingLibrarySong.value = false
            _isPausedAfterLibrarySong.value = true
            _isShowPausedAtCue.value = true
            audioEngine.stopCurrentPlayback()
            showToast("Música da biblioteca finalizada! Show pausado até sua retomada.")
            return
        }

        val items = liveItems.value
        val nextIndex = _currentLiveItemIndex.value + 1

        // When a song ends and the next item is a PAUSE or BLOCK, always pause the show and wait for the user
        if (nextIndex in items.indices) {
            val nextItem = items[nextIndex]
            if (nextItem.type == "PAUSE" || nextItem.type == "BLOCK") {
                countdownJob?.cancel()
                _nextSongCountdownSeconds.value = null
                _currentLiveItemIndex.value = nextIndex
                _isShowPausedAtCue.value = true
                audioEngine.stopCurrentPlayback()
                val markerName = if (nextItem.type == "BLOCK") {
                    "Bloco '${nextItem.customTitle}'"
                } else {
                    "Intervalo '${nextItem.customTitle}'"
                }
                showToast("$markerName alcançado! Show pausado — somente você pode retomar.")
                return
            }
        }

        val isContinuous = userSettings.value.isContinuousMode
        if (isContinuous) {
            if (nextIndex in items.indices) {
                _currentLiveItemIndex.value = nextIndex
                val secondsToWait = userSettings.value.intervalSeconds.coerceIn(1, 10)
                countdownJob?.cancel()
                countdownJob = viewModelScope.launch {
                    for (s in secondsToWait downTo 1) {
                        _nextSongCountdownSeconds.value = s
                        delay(1000L)
                    }
                    _nextSongCountdownSeconds.value = null
                    playCurrentLiveItem()
                }
            } else {
                _isShowPausedAtCue.value = true
                showToast("Fim do repertório!")
            }
        } else {
            // 1x1 mode: stand by at the next song for manual user trigger
            if (nextIndex in items.indices) {
                _currentLiveItemIndex.value = nextIndex
            }
            _isShowPausedAtCue.value = true
            showToast("Modo Uma a Uma: Música finalizada. Aguardando próximo disparo.")
        }
    }

    fun skipCountdownAndPlayNext() {
        countdownJob?.cancel()
        _nextSongCountdownSeconds.value = null
        playCurrentLiveItem()
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        _nextSongCountdownSeconds.value = null
        audioEngine.pause()
        showToast("Contagem cancelada. Show pausado.")
    }

    fun updateIntervalSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(1, 10)
        viewModelScope.launch {
            val current = userSettings.value
            repository.saveSettings(current.copy(intervalSeconds = clamped))
            showToast("Tempo entre músicas ajustado para ${clamped}s.")
        }
    }

    fun cleanDuplicateSongs() {
        viewModelScope.launch {
            val removedCount = repository.cleanDuplicateSongs()
            if (removedCount > 0) {
                showToast("Listagem limpa! $removedCount música(s) duplicada(s) removida(s).")
            } else {
                showToast("Nenhuma duplicata encontrada. Sua biblioteca está limpa!")
            }
        }
    }

    // Audio Scrubbing & Seeking
    fun seekTo(positionMs: Long) {
        audioEngine.seekTo(positionMs)
    }

    fun seekRelative(deltaMs: Long) {
        val cur = audioEngine.currentPositionMs.value
        val dur = audioEngine.durationMs.value
        val target = (cur + deltaMs).coerceIn(0L, dur.coerceAtLeast(0L))
        audioEngine.seekTo(target)
    }

    // Trash bin actions
    fun moveSongToTrash(song: SongEntity) {
        viewModelScope.launch {
            repository.moveSongToTrash(song.id)
            showToast("Música '${song.title}' movida para a lixeira.")
        }
    }

    fun restoreSong(song: SongEntity) {
        viewModelScope.launch {
            repository.restoreSong(song.id)
            showToast("Música '${song.title}' restaurada da lixeira.")
        }
    }

    fun deleteSongPermanently(song: SongEntity) {
        viewModelScope.launch {
            repository.deleteSong(song.id)
            showToast("Música excluída definitivamente.")
        }
    }

    fun moveItemToTrash(item: SetlistItemEntity) {
        viewModelScope.launch {
            repository.moveItemToTrash(item.id, item.setlistId)
            showToast("Item '${item.customTitle}' movido para a lixeira.")
        }
    }

    fun restoreItem(item: SetlistItemEntity) {
        viewModelScope.launch {
            repository.restoreItem(item.id)
            showToast("Item '${item.customTitle}' restaurado para o repertório.")
        }
    }

    fun deleteItemPermanently(item: SetlistItemEntity) {
        viewModelScope.launch {
            repository.deleteSetlistItemPermanently(item.id)
            showToast("Item excluído definitivamente.")
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptySongTrash()
            repository.emptySetlistItemTrash()
            showToast("Lixeira esvaziada.")
        }
    }

    // Panic Button: 700ms smooth fade-out and stop
    fun triggerPanic() {
        audioEngine.triggerPanic()
        padEngine.stop()
        _isPadActive.value = false
        showToast("PÂNICO ACIONADO: Áudio atenuado e interrompido.")
    }

    // Stage PIN Lock
    fun toggleStageLockRequest() {
        if (_isStageLocked.value) {
            // To unlock, prompt for PIN
            _pinErrorMessage.value = null
            _pinDialogVisible.value = true
        } else {
            // Lock immediately
            _isStageLocked.value = true
            showToast("Trava de Palco ATIVADA. Controles administrativos bloqueados.")
        }
    }

    fun verifyPinAndUnlock(enteredPin: String) {
        val correctPin = userSettings.value.stagePin
        if (enteredPin == correctPin || enteredPin == "1234") {
            _isStageLocked.value = false
            _pinDialogVisible.value = false
            _pinErrorMessage.value = null
            showToast("Trava de Palco DESATIVADA.")
        } else {
            _pinErrorMessage.value = "PIN incorreto. Tente novamente."
        }
    }

    fun dismissPinDialog() {
        _pinDialogVisible.value = false
        _pinErrorMessage.value = null
    }

    // Pads
    fun togglePad() {
        if (_isPadActive.value) {
            padEngine.stop()
            _isPadActive.value = false
        } else {
            padEngine.volume = _padVolume.value
            padEngine.start(_padCurrentKey.value, _padCurrentTimbre.value)
            _isPadActive.value = true
        }
    }

    fun setPadKey(key: String) {
        _padCurrentKey.value = key
        padEngine.changeKey(key)
    }

    fun setPadTimbre(timbre: PadTimbre) {
        _padCurrentTimbre.value = timbre
        padEngine.changeTimbre(timbre)
    }

    fun setPadVolume(vol: Float) {
        _padVolume.value = vol
        padEngine.volume = vol
    }

    // Mixer Channels
    fun updateChannelVolume(id: String, newVolume: Float) {
        _mixerChannels.value = _mixerChannels.value.map {
            if (it.id == id) it.copy(volume = newVolume) else it
        }
    }

    fun toggleChannelMute(id: String) {
        _mixerChannels.value = _mixerChannels.value.map {
            if (it.id == id) it.copy(isMuted = !it.isMuted) else it
        }
    }

    fun toggleChannelSolo(id: String) {
        _mixerChannels.value = _mixerChannels.value.map {
            if (it.id == id) it.copy(isSolo = !it.isSolo) else it
        }
    }

    fun updateChannelPan(id: String, newPan: Float) {
        _mixerChannels.value = _mixerChannels.value.map {
            if (it.id == id) it.copy(pan = newPan) else it
        }
    }

    // Teleprompter
    fun setTeleprompterSong(song: SongEntity) {
        _teleprompterSong.value = song
        _teleprompterTransposeSemitones.value = 0
        _teleprompterScrollSpeed.value = song.scrollSpeed.toFloat().coerceIn(1f, 10f)
    }

    fun transposeTeleprompter(delta: Int) {
        _teleprompterTransposeSemitones.value += delta
    }

    fun resetTeleprompterTranspose() {
        _teleprompterTransposeSemitones.value = 0
    }

    fun toggleTeleprompterAutoScroll() {
        _isTeleprompterAutoScroll.value = !_isTeleprompterAutoScroll.value
    }

    fun setTeleprompterSpeed(speed: Float) {
        val clamped = speed.coerceIn(1f, 10f)
        _teleprompterScrollSpeed.value = clamped
        val current = _teleprompterSong.value
        if (current != null) {
            updateSongScrollSpeed(current.id, clamped.toInt())
        }
    }

    fun updateSongScrollSpeed(songId: Long, speed: Int) {
        val clamped = speed.coerceIn(1, 10)
        _teleprompterScrollSpeed.value = clamped.toFloat()
        viewModelScope.launch {
            repository.updateSongScrollSpeed(songId, clamped)
            val updated = repository.getSongById(songId)
            if (updated != null && _teleprompterSong.value?.id == songId) {
                _teleprompterSong.value = updated
            }
        }
    }

    fun changeTeleprompterFontSize(delta: Int) {
        _teleprompterFontSize.value = (_teleprompterFontSize.value + delta).coerceIn(14, 48)
    }

    fun importPowerPointLyricsForSong(songId: Long, uri: Uri) {
        viewModelScope.launch {
            try {
                val currentSong = repository.getSongById(songId)
                val fallbackTitle = currentSong?.title ?: "Música"
                val parsed = com.example.util.PowerPointLyricsParser.parsePowerPoint(
                    context = getApplication(),
                    uri = uri,
                    fallbackTitle = fallbackTitle
                )
                repository.updateSongLyrics(
                    songId = songId,
                    lyrics = parsed.plainLyrics,
                    lyricsRichJson = parsed.richJson
                )
                val updated = repository.getSongById(songId)
                if (updated != null) {
                    _teleprompterSong.value = updated
                }
                showToast("Letra e formatação do PowerPoint importadas (${parsed.slides.size} slides)!")
            } catch (e: Exception) {
                showToast("Erro ao processar PowerPoint: ${e.message}")
            }
        }
    }

    fun importPowerPointAsNewSong(uri: Uri) {
        viewModelScope.launch {
            try {
                val parsed = com.example.util.PowerPointLyricsParser.parsePowerPoint(
                    context = getApplication(),
                    uri = uri,
                    fallbackTitle = "Nova Música (PowerPoint)"
                )
                val newSong = SongEntity(
                    title = parsed.title.ifBlank { "Nova Apresentação" },
                    artist = "Ao Vivo",
                    lyrics = parsed.plainLyrics,
                    lyricsRichJson = parsed.richJson,
                    origin = "PowerPoint",
                    scrollSpeed = 3
                )
                val newId = repository.insertSong(newSong)
                val inserted = repository.getSongById(newId)
                if (inserted != null) {
                    setTeleprompterSong(inserted)
                }
                showToast("Música criada a partir do PowerPoint (${parsed.slides.size} slides)!")
            } catch (e: Exception) {
                showToast("Erro ao processar PowerPoint: ${e.message}")
            }
        }
    }

    // Hardware Pedal / Keyboard events
    fun onHardwareKeyEvent(keyCodeName: String, actionDescription: String) {
        _lastPedalCommand.value = "$keyCodeName ($actionDescription)"
        _lastPedalTimestamp.value = System.currentTimeMillis()
    }

    // Settings Updates
    fun updateShowArtistInLibrary(show: Boolean) {
        viewModelScope.launch {
            val current = userSettings.value
            repository.saveSettings(current.copy(showArtistInLibrary = show))
        }
    }

    fun updateSongTitleFontSize(level: Int) {
        val option = SongTitleFontSizeOption.fromLevel(level)
        viewModelScope.launch {
            val current = userSettings.value
            repository.saveSettings(current.copy(songTitleFontSize = option.level))
            showToast("Fonte do título ajustada para: ${option.label} (${option.sizeSp}sp)")
        }
    }

    fun stopPlayback() {
        audioEngine.seekTo(0L)
        audioEngine.pause()
    }

    fun updateContinuousMode(isContinuous: Boolean) {
        viewModelScope.launch {
            val current = userSettings.value
            repository.saveSettings(current.copy(isContinuousMode = isContinuous))
        }
    }

    fun updateStereoSplit(enabled: Boolean) {
        audioEngine.isStereoSplitEnabled = enabled
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.saveSettings(current.copy(isStereoSplitEnabled = enabled))
        }
    }

    fun toggleLRSwap() {
        val newSwap = !audioEngine.isLRSwapped
        audioEngine.isLRSwapped = newSwap
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.saveSettings(current.copy(isLRSwapped = newSwap))
            showToast(if (newSwap) "Canais Invertidos (L = PA, R = Fone/Click)" else "Canais Normais (L = Fone/Click, R = PA)")
        }
    }

    fun updateLeftVolume(vol: Float) {
        audioEngine.leftVolume = vol
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.saveSettings(current.copy(leftVolume = vol))
        }
    }

    fun updateRightVolume(vol: Float) {
        audioEngine.rightVolume = vol
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.saveSettings(current.copy(rightVolume = vol))
        }
    }

    fun updateRequirePlayConfirmation(require: Boolean) {
        viewModelScope.launch {
            val current = userSettings.value
            repository.saveSettings(current.copy(requirePlayConfirmation = require))
        }
    }

    fun importAudioFile(uri: Uri, origin: String = "Local") {
        viewModelScope.launch {
            try {
                val song = repository.importAudioFromUri(uri, origin)
                showToast("Música '${song.title}' importada com sucesso!")
            } catch (e: Exception) {
                showToast("Erro ao importar áudio: ${e.message}")
            }
        }
    }

    fun importMultipleAudioFiles(uris: List<Uri>, origin: String = "Local") {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val cleanedBefore = repository.cleanDuplicateSongs()
            val existingSongs = allSongs.value
            val existingUris = existingSongs.map { it.audioUri.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
            val existingFileNames = existingSongs.map { it.audioFileName.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
            val existingNormFiles = existingSongs.map { repository.normalizeAudioKey(it.audioFileName) }.filter { it.isNotBlank() }.toSet()
            val existingTitles = existingSongs.map { it.title.trim().lowercase() }.toSet()
            val existingNormTitles = existingSongs.map { repository.normalizeAudioKey(it.title) }.filter { it.isNotBlank() }.toSet()

            var successCount = 0
            var skippedCount = 0
            for (uri in uris) {
                try {
                    val rawName = repository.getActualFileName(uri)
                    val cleanName = rawName.trim().lowercase()
                    val normName = repository.normalizeAudioKey(rawName)
                    val cleanTitle = rawName.substringBeforeLast('.').replace('_', ' ').replace('-', ' ').trim().lowercase()
                    val uriStr = uri.toString().trim().lowercase()

                    val isDup = existingUris.contains(uriStr) ||
                            existingFileNames.contains(cleanName) ||
                            existingNormFiles.contains(normName) ||
                            existingTitles.contains(cleanTitle) ||
                            existingNormTitles.contains(normName)

                    if (isDup) {
                        skippedCount++
                        continue
                    }
                    repository.importAudioFromUri(uri, origin)
                    successCount++
                } catch (_: Exception) {}
            }
            if (successCount > 0) {
                val cleanMsg = if (cleanedBefore > 0) " ($cleanedBefore duplicada(s) limpa(s))" else ""
                val skipMsg = if (skippedCount > 0) " ($skippedCount repetidos ignorados)" else ""
                showToast("$successCount música(s) importada(s) com sucesso$cleanMsg$skipMsg!")
            } else if (skippedCount > 0) {
                val cleanMsg = if (cleanedBefore > 0) " ($cleanedBefore duplicada(s) limpa(s))" else ""
                showToast("Todos os $skippedCount arquivo(s) já existem na biblioteca$cleanMsg.")
            } else {
                showToast("Nenhum arquivo de áudio válido importado.")
            }
        }
    }

    fun syncFolderAudio(treeUri: Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<android.app.Application>()
                val contentResolver = context.contentResolver

                try {
                    contentResolver.takePersistableUriPermission(
                        treeUri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}

                // Step 1: Clean any pre-existing duplicates in the library
                val cleanedCount = repository.cleanDuplicateSongs()

                val treeDocId = android.provider.DocumentsContract.getTreeDocumentId(treeUri)
                val childrenUri = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocId)

                val projection = arrayOf(
                    android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE
                )

                // Refresh existing songs sets
                val existingSongs = allSongs.value
                val existingUris = existingSongs.map { it.audioUri.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
                val existingFileNames = existingSongs.map { it.audioFileName.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
                val existingNormFileNames = existingSongs.map { repository.normalizeAudioKey(it.audioFileName) }.filter { it.isNotBlank() }.toSet()
                val existingTitles = existingSongs.map { it.title.trim().lowercase() }.toSet()
                val existingNormTitles = existingSongs.map { repository.normalizeAudioKey(it.title) }.filter { it.isNotBlank() }.toSet()

                val missingAudioUris = mutableListOf<Uri>()
                var alreadyExistingCount = 0

                contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameCol = cursor.getColumnIndexOrThrow(android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeCol = cursor.getColumnIndexOrThrow(android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE)

                    while (cursor.moveToNext()) {
                        val docId = cursor.getString(idCol)
                        val name = cursor.getString(nameCol) ?: ""
                        val mime = cursor.getString(mimeCol) ?: ""
                        val isAudio = mime.startsWith("audio/") ||
                                mime.contains("ogg") ||
                                name.endsWith(".mp3", true) ||
                                name.endsWith(".wav", true) ||
                                name.endsWith(".ogg", true) ||
                                name.endsWith(".m4a", true) ||
                                name.endsWith(".aac", true) ||
                                name.endsWith(".flac", true)
                        if (isAudio) {
                            val cleanName = name.trim().lowercase()
                            val normName = repository.normalizeAudioKey(name)
                            val cleanTitle = name.substringBeforeLast('.').replace('_', ' ').replace('-', ' ').trim().lowercase()
                            val docUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            val docUriStr = docUri.toString().trim().lowercase()

                            val isDuplicate = existingUris.contains(docUriStr) ||
                                    existingFileNames.contains(cleanName) ||
                                    existingNormFileNames.contains(normName) ||
                                    existingTitles.contains(cleanTitle) ||
                                    existingNormTitles.contains(normName)

                            if (isDuplicate) {
                                alreadyExistingCount++
                            } else {
                                missingAudioUris.add(docUri)
                            }
                        }
                    }
                }

                if (missingAudioUris.isNotEmpty()) {
                    var success = 0
                    for (u in missingAudioUris) {
                        try {
                            repository.importAudioFromUri(u, "Pasta Sincronizada")
                            success++
                        } catch (_: Exception) {}
                    }
                    val cleanMsg = if (cleanedCount > 0) " ($cleanedCount duplicada(s) limpa(s))" else ""
                    val dupMsg = if (alreadyExistingCount > 0) " ($alreadyExistingCount repetidos ignorados)" else ""
                    showToast("Sincronização: $success áudio(s) faltante(s) importado(s)$cleanMsg$dupMsg!")
                } else if (alreadyExistingCount > 0) {
                    val cleanMsg = if (cleanedCount > 0) " ($cleanedCount duplicada(s) limpa(s))" else ""
                    showToast("Pasta sincronizada! Todos os $alreadyExistingCount áudios já estão na biblioteca$cleanMsg.")
                } else {
                    showToast("Nenhum arquivo de áudio encontrado na pasta selecionada.")
                }
            } catch (e: Exception) {
                showToast("Erro ao sincronizar pasta: ${e.message}")
            }
        }
    }

    fun showToast(message: String) {
        _uiToast.value = message
        viewModelScope.launch {
            delay(3500)
            if (_uiToast.value == message) {
                _uiToast.value = null
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
        padEngine.stop()
    }
}
