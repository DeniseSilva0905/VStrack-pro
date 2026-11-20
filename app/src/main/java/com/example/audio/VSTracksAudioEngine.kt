package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaPlayer
import android.net.Uri
import com.example.data.model.SongEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

enum class PlaybackState {
    IDLE,
    PLAYING,
    PAUSED,
    PANIC_STOPPED
}

enum class TestToneType {
    NONE,
    CLICK_1KHZ,
    VS_440HZ
}

class VSTracksAudioEngine(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var mediaPlayer: MediaPlayer? = null
    private var decoderAudioTrack: AudioTrack? = null
    private var decoderPlaybackJob: Job? = null
    private var testToneTrack: AudioTrack? = null
    private var testToneJob: Job? = null
    private var synthPlaybackJob: Job? = null
    private var synthAudioTrack: AudioTrack? = null

    @Volatile
    private var isUsingPcmDecoder: Boolean = false

    @Volatile
    private var pendingSeekMs: Long = -1L

    @Volatile
    private var fadeGain: Float = 1.0f

    @Volatile
    private var isPanicFading: Boolean = false

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs

    private val _activeSong = MutableStateFlow<SongEntity?>(null)
    val activeSong: StateFlow<SongEntity?> = _activeSong

    private val _activeTestTone = MutableStateFlow(TestToneType.NONE)
    val activeTestTone: StateFlow<TestToneType> = _activeTestTone

    // Stereo Split Settings (read on real-time audio threads)
    @Volatile
    var isStereoSplitEnabled: Boolean = true
        set(value) {
            field = value
            applyVolumes()
        }

    @Volatile
    var isLRSwapped: Boolean = false
        set(value) {
            field = value
            applyVolumes()
        }

    @Volatile
    var leftVolume: Float = 0.9f
        set(value) {
            field = value
            applyVolumes()
        }

    @Volatile
    var rightVolume: Float = 1.0f
        set(value) {
            field = value
            applyVolumes()
        }

    @Volatile
    var masterVolume: Float = 1.0f
        set(value) {
            field = value
            applyVolumes()
        }

    // Callbacks
    var onSongCompleted: (() -> Unit)? = null

    private var progressJob: Job? = null
    private var panicJob: Job? = null

    init {
        startProgressMonitor()
    }

    private fun startProgressMonitor() {
        progressJob = scope.launch {
            while (isActive) {
                if (_playbackState.value == PlaybackState.PLAYING) {
                    if (mediaPlayer != null) {
                        try {
                            _currentPositionMs.value = mediaPlayer?.currentPosition?.toLong() ?: 0L
                        } catch (_: Exception) {}
                    }
                }
                delay(100)
            }
        }
    }

    fun loadAndPlay(song: SongEntity) {
        stopCurrentPlayback()
        fadeGain = 1.0f
        isPanicFading = false
        pendingSeekMs = -1L
        _activeSong.value = song
        _durationMs.value = if (song.durationMs > 0) song.durationMs else 180000L
        _currentPositionMs.value = 0L

        if (song.audioUri.isNotBlank()) {
            // Primary real-audio path: MediaExtractor + MediaCodec + AudioTrack for true PCM L/R swap
            if (startPcmDecodedPlayback(song)) {
                return
            }

            // Fallback MediaPlayer path if MediaCodec cannot initialize URI
            try {
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(context, Uri.parse(song.audioUri))
                    prepare()
                    setOnCompletionListener {
                        _playbackState.value = PlaybackState.IDLE
                        onSongCompleted?.invoke()
                    }
                }
                mediaPlayer = player
                applyVolumes()
                player.start()
                _playbackState.value = PlaybackState.PLAYING
                return
            } catch (_: Exception) {
                // If URI failed or is virtual demo, fallback to high-fidelity live audio generator
            }
        }

        // Live synthetic VS track reproduction with stereo split
        playSyntheticSong(song, startPositionMs = 0L)
    }

    private fun buildStereoAudioTrack(sampleRate: Int): AudioTrack {
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    private fun startPcmDecodedPlayback(song: SongEntity): Boolean {
        var extractor: MediaExtractor? = null
        var codec: MediaCodec? = null
        try {
            extractor = MediaExtractor()
            val parsedUri = Uri.parse(song.audioUri)
            if (parsedUri.scheme.isNullOrEmpty()) {
                extractor.setDataSource(song.audioUri)
            } else {
                extractor.setDataSource(context, parsedUri, null)
            }

            var audioTrackIndex = -1
            var inputFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    inputFormat = format
                    break
                }
            }

            if (audioTrackIndex < 0 || inputFormat == null) {
                extractor.release()
                return false
            }

            extractor.selectTrack(audioTrackIndex)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: run {
                extractor.release()
                return false
            }

            if (inputFormat.containsKey(MediaFormat.KEY_DURATION)) {
                val fileDurationMs = inputFormat.getLong(MediaFormat.KEY_DURATION) / 1000L
                if (fileDurationMs > 0L && song.durationMs <= 0L) {
                    _durationMs.value = fileDurationMs
                }
            }

            var sampleRate = if (inputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else 44100
            var channelCount = if (inputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else 2
            var pcmEncoding = if (inputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                inputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
            } else AudioFormat.ENCODING_PCM_16BIT

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(inputFormat, null, null, 0)
            codec.start()

            var track = buildStereoAudioTrack(sampleRate)
            decoderAudioTrack = track
            track.play()

            isUsingPcmDecoder = true
            _playbackState.value = PlaybackState.PLAYING

            val activeExtractor = extractor
            val activeCodec = codec

            decoderPlaybackJob = scope.launch(Dispatchers.IO) {
                val bufferInfo = MediaCodec.BufferInfo()
                var sawInputEOS = false
                var sawOutputEOS = false
                var totalFramesWritten = 0L
                var scratchStereo = ShortArray(4096)

                try {
                    while (isActive && !sawOutputEOS && (_playbackState.value == PlaybackState.PLAYING || _playbackState.value == PlaybackState.PAUSED || isPanicFading)) {
                        val seekTarget = pendingSeekMs
                        if (seekTarget >= 0L) {
                            pendingSeekMs = -1L
                            activeCodec.flush()
                            activeExtractor.seekTo(seekTarget * 1000L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                            try {
                                track.pause()
                                track.flush()
                                totalFramesWritten = 0L
                                if (_playbackState.value == PlaybackState.PLAYING || isPanicFading) {
                                    track.play()
                                }
                            } catch (_: Exception) {}
                            sawInputEOS = false
                            sawOutputEOS = false
                            continue
                        }

                        if (_playbackState.value == PlaybackState.PAUSED && !isPanicFading) {
                            try {
                                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                                    track.pause()
                                }
                            } catch (_: Exception) {}
                            delay(30)
                            continue
                        }

                        try {
                            if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                                track.play()
                            }
                        } catch (_: Exception) {}

                        if (!sawInputEOS) {
                            val inputIndex = activeCodec.dequeueInputBuffer(10_000)
                            if (inputIndex >= 0) {
                                val inputBuffer = activeCodec.getInputBuffer(inputIndex)
                                if (inputBuffer != null) {
                                    val sampleSize = activeExtractor.readSampleData(inputBuffer, 0)
                                    if (sampleSize < 0) {
                                        activeCodec.queueInputBuffer(
                                            inputIndex,
                                            0,
                                            0,
                                            0L,
                                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                        )
                                        sawInputEOS = true
                                    } else {
                                        val pts = activeExtractor.sampleTime
                                        activeCodec.queueInputBuffer(inputIndex, 0, sampleSize, pts, 0)
                                        activeExtractor.advance()
                                    }
                                }
                            }
                        }

                        val outputIndex = activeCodec.dequeueOutputBuffer(bufferInfo, 10_000)
                        when {
                            outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                                val newFormat = activeCodec.outputFormat
                                val newSampleRate = if (newFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                                    newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                                } else sampleRate
                                channelCount = if (newFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                                    newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                                } else channelCount
                                pcmEncoding = if (newFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                                    newFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                                } else AudioFormat.ENCODING_PCM_16BIT

                                if (newSampleRate != sampleRate && newSampleRate > 0) {
                                    sampleRate = newSampleRate
                                    try {
                                        track.stop()
                                        track.release()
                                    } catch (_: Exception) {}
                                    track = buildStereoAudioTrack(sampleRate)
                                    decoderAudioTrack = track
                                    totalFramesWritten = 0L
                                    if (_playbackState.value == PlaybackState.PLAYING || isPanicFading) {
                                        track.play()
                                    }
                                }
                            }
                            outputIndex >= 0 -> {
                                val outBuf = activeCodec.getOutputBuffer(outputIndex)
                                if (outBuf != null && bufferInfo.size > 0) {
                                    outBuf.position(bufferInfo.offset)
                                    outBuf.limit(bufferInfo.offset + bufferInfo.size)
                                    outBuf.order(ByteOrder.LITTLE_ENDIAN)

                                    val channels = channelCount.coerceAtLeast(1)
                                    val frameCount = when (pcmEncoding) {
                                        AudioFormat.ENCODING_PCM_FLOAT -> (bufferInfo.size / 4) / channels
                                        AudioFormat.ENCODING_PCM_8BIT -> bufferInfo.size / channels
                                        else -> (bufferInfo.size / 2) / channels
                                    }

                                    if (frameCount > 0) {
                                        val neededShorts = frameCount * 2
                                        if (scratchStereo.size < neededShorts) {
                                            scratchStereo = ShortArray(neededShorts)
                                        }

                                        when (pcmEncoding) {
                                            AudioFormat.ENCODING_PCM_FLOAT -> {
                                                val floatBuf = outBuf.asFloatBuffer()
                                                for (i in 0 until frameCount) {
                                                    val base = i * channels
                                                    val s0 = (floatBuf.get(base).coerceIn(-1f, 1f) * 32767f).toInt().toShort()
                                                    val s1 = if (channels >= 2) {
                                                        (floatBuf.get(base + 1).coerceIn(-1f, 1f) * 32767f).toInt().toShort()
                                                    } else s0
                                                    val outIdx = i shl 1
                                                    scratchStereo[outIdx] = s0
                                                    scratchStereo[outIdx + 1] = s1
                                                }
                                            }
                                            AudioFormat.ENCODING_PCM_8BIT -> {
                                                for (i in 0 until frameCount) {
                                                    val s0 = (((outBuf.get().toInt() and 0xFF) - 128) shl 8).toShort()
                                                    val s1 = if (channels >= 2) {
                                                        (((outBuf.get().toInt() and 0xFF) - 128) shl 8).toShort()
                                                    } else s0
                                                    for (skip in 2 until channels) {
                                                        outBuf.get()
                                                    }
                                                    val outIdx = i shl 1
                                                    scratchStereo[outIdx] = s0
                                                    scratchStereo[outIdx + 1] = s1
                                                }
                                            }
                                            else -> {
                                                val shortBuf = outBuf.asShortBuffer()
                                                for (i in 0 until frameCount) {
                                                    val base = i * channels
                                                    val s0 = shortBuf.get(base)
                                                    val s1 = if (channels >= 2) shortBuf.get(base + 1) else s0
                                                    val outIdx = i shl 1
                                                    scratchStereo[outIdx] = s0
                                                    scratchStereo[outIdx + 1] = s1
                                                }
                                            }
                                        }

                                        applyStereoRoutingInPlace(
                                            interleavedStereo = scratchStereo,
                                            frameCount = frameCount,
                                            leftVol = leftVolume,
                                            rightVol = rightVolume,
                                            masterVol = masterVolume * fadeGain,
                                            isStereoSplitEnabled = isStereoSplitEnabled,
                                            isLRSwapped = isLRSwapped
                                        )

                                        val writtenShorts = track.write(scratchStereo, 0, neededShorts)
                                        if (writtenShorts > 0) {
                                            totalFramesWritten += (writtenShorts / 2)
                                        }
                                    }

                                    if (pendingSeekMs < 0L && bufferInfo.presentationTimeUs >= 0L) {
                                        _currentPositionMs.value = bufferInfo.presentationTimeUs / 1000L
                                    }
                                }

                                activeCodec.releaseOutputBuffer(outputIndex, false)
                                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                    sawOutputEOS = true
                                }
                            }
                        }
                    }

                    if (sawOutputEOS && isActive && (_playbackState.value == PlaybackState.PLAYING)) {
                        // Drain remaining queued frames briefly
                        var drainChecks = 0
                        while (isActive && drainChecks < 60 && _playbackState.value == PlaybackState.PLAYING) {
                            val headPos = try { track.playbackHeadPosition.toLong() } catch (_: Exception) { totalFramesWritten }
                            if (headPos >= totalFramesWritten) break
                            delay(30)
                            drainChecks++
                        }

                        withContext(Dispatchers.Main) {
                            if (_playbackState.value == PlaybackState.PLAYING) {
                                _playbackState.value = PlaybackState.IDLE
                                isUsingPcmDecoder = false
                                onSongCompleted?.invoke()
                            }
                        }
                    }
                } finally {
                    try { activeCodec.stop() } catch (_: Exception) {}
                    try { activeCodec.release() } catch (_: Exception) {}
                    try { activeExtractor.release() } catch (_: Exception) {}
                    try { track.stop() } catch (_: Exception) {}
                    try { track.release() } catch (_: Exception) {}
                    if (decoderAudioTrack === track) {
                        decoderAudioTrack = null
                    }
                    isUsingPcmDecoder = false
                }
            }
            return true
        } catch (_: Exception) {
            try { codec?.stop() } catch (_: Exception) {}
            try { codec?.release() } catch (_: Exception) {}
            try { extractor?.release() } catch (_: Exception) {}
            try { decoderAudioTrack?.stop() } catch (_: Exception) {}
            try { decoderAudioTrack?.release() } catch (_: Exception) {}
            decoderAudioTrack = null
            isUsingPcmDecoder = false
            return false
        }
    }

    private fun playSyntheticSong(song: SongEntity, startPositionMs: Long = 0L) {
        val sampleRate = 44100
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        ) * 2

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        synthAudioTrack = track
        track.play()
        _playbackState.value = PlaybackState.PLAYING

        synthPlaybackJob = scope.launch(Dispatchers.Default) {
            val numFrames = 1024
            val buffer = ShortArray(numFrames * 2)
            val bpm = song.bpm.coerceIn(40, 240)
            val beatIntervalSamples = (sampleRate * 60.0 / bpm).toInt()
            var sampleCounter = (startPositionMs.coerceAtLeast(0L) * sampleRate) / 1000L

            var synthPhase = 0.0
            val rootFreq = 164.81 // E key base

            while (isActive && (_playbackState.value == PlaybackState.PLAYING || isPanicFading)) {
                val seekTarget = pendingSeekMs
                if (seekTarget >= 0L) {
                    pendingSeekMs = -1L
                    sampleCounter = (seekTarget.coerceAtLeast(0L) * sampleRate) / 1000L
                }

                for (i in 0 until numFrames) {
                    val beatPosition = (sampleCounter % beatIntervalSamples).toInt()
                    val isFirstBeat = (sampleCounter % (beatIntervalSamples * 4)).toInt() < sampleRate / 10

                    // Logical Left Channel: Click track metronome click (1.6kHz accent on 1, 900Hz on 2-3-4)
                    val clickSample: Double = if (beatPosition < sampleRate / 20) {
                        val clickFreq = if (isFirstBeat) 1600.0 else 900.0
                        val clickEnv = 1.0 - (beatPosition.toDouble() / (sampleRate / 20))
                        sin(2.0 * PI * clickFreq * beatPosition / sampleRate) * clickEnv * 0.85
                    } else {
                        0.0
                    }

                    // Logical Right Channel: VS Playback backing track (warm chords + sub rhythm)
                    synthPhase += 2.0 * PI * rootFreq / sampleRate
                    val chord = sin(synthPhase) * 0.4 + sin(synthPhase * 1.5) * 0.3 + sin(synthPhase * 2.0) * 0.2
                    val vsSample = chord * 0.65

                    buffer[i * 2] = (clickSample * 28000.0).toInt().coerceIn(-32767, 32767).toShort()
                    buffer[i * 2 + 1] = (vsSample * 28000.0).toInt().coerceIn(-32767, 32767).toShort()

                    sampleCounter++
                }

                applyStereoRoutingInPlace(
                    interleavedStereo = buffer,
                    frameCount = numFrames,
                    leftVol = leftVolume,
                    rightVol = rightVolume,
                    masterVol = masterVolume * fadeGain,
                    isStereoSplitEnabled = isStereoSplitEnabled,
                    isLRSwapped = isLRSwapped
                )

                track.write(buffer, 0, buffer.size)

                val elapsedMs = (sampleCounter * 1000L / sampleRate)
                withContext(Dispatchers.Main) {
                    if (_playbackState.value == PlaybackState.PLAYING) {
                        _currentPositionMs.value = elapsedMs
                        if (elapsedMs >= _durationMs.value) {
                            _playbackState.value = PlaybackState.IDLE
                            onSongCompleted?.invoke()
                        }
                    }
                }
            }
        }
    }

    fun playPause() {
        when (_playbackState.value) {
            PlaybackState.PLAYING -> pause()
            PlaybackState.PAUSED -> resume()
            PlaybackState.IDLE, PlaybackState.PANIC_STOPPED -> {
                _activeSong.value?.let { loadAndPlay(it) }
            }
        }
    }

    fun pause() {
        if (_playbackState.value == PlaybackState.PLAYING) {
            mediaPlayer?.pause()
            if (isUsingPcmDecoder) {
                try { decoderAudioTrack?.pause() } catch (_: Exception) {}
                _playbackState.value = PlaybackState.PAUSED
            } else {
                synthPlaybackJob?.cancel()
                try {
                    synthAudioTrack?.stop()
                    synthAudioTrack?.release()
                } catch (_: Exception) {}
                synthAudioTrack = null
                _playbackState.value = PlaybackState.PAUSED
            }
        }
    }

    fun resume() {
        if (_playbackState.value == PlaybackState.PAUSED) {
            fadeGain = 1.0f
            isPanicFading = false
            when {
                isUsingPcmDecoder && decoderPlaybackJob?.isActive == true -> {
                    try { decoderAudioTrack?.play() } catch (_: Exception) {}
                    _playbackState.value = PlaybackState.PLAYING
                }
                mediaPlayer != null -> {
                    mediaPlayer?.start()
                    _playbackState.value = PlaybackState.PLAYING
                }
                else -> {
                    _activeSong.value?.let {
                        playSyntheticSong(it, startPositionMs = _currentPositionMs.value)
                    }
                }
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceAtLeast(0L)
        _currentPositionMs.value = clamped
        pendingSeekMs = clamped
        try {
            mediaPlayer?.seekTo(clamped.toInt())
        } catch (_: Exception) {}
    }

    /**
     * Panic Button: 700ms smooth exponential fade-out to stop audio cleanly without clicks/pops.
     */
    fun triggerPanic() {
        panicJob?.cancel()
        panicJob = scope.launch {
            isPanicFading = true
            val steps = 14
            val fadeStepMs = 50L // 14 * 50 = 700ms total fade duration
            val initialMaster = masterVolume

            for (step in 1..steps) {
                val decayFactor = (1.0f - (step.toFloat() / steps))
                fadeGain = (decayFactor * decayFactor).coerceAtLeast(0f)
                applyCustomVolume(initialMaster * fadeGain)
                delay(fadeStepMs)
            }

            isPanicFading = false
            stopCurrentPlayback()
            stopTestTone()
            fadeGain = 1.0f
            masterVolume = initialMaster
            applyVolumes()
            _playbackState.value = PlaybackState.PANIC_STOPPED
        }
    }

    fun stopCurrentPlayback() {
        panicJob?.cancel()
        isPanicFading = false
        fadeGain = 1.0f
        pendingSeekMs = -1L

        decoderPlaybackJob?.cancel()
        decoderPlaybackJob = null
        isUsingPcmDecoder = false
        try {
            decoderAudioTrack?.stop()
            decoderAudioTrack?.release()
        } catch (_: Exception) {}
        decoderAudioTrack = null

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null

        synthPlaybackJob?.cancel()
        synthPlaybackJob = null
        try {
            synthAudioTrack?.stop()
            synthAudioTrack?.release()
        } catch (_: Exception) {}
        synthAudioTrack = null
        _playbackState.value = PlaybackState.IDLE
    }

    private fun applyVolumes() {
        val l = if (isLRSwapped) rightVolume else leftVolume
        val r = if (isLRSwapped) leftVolume else rightVolume
        val gain = masterVolume * fadeGain
        try {
            mediaPlayer?.setVolume(l * gain, r * gain)
        } catch (_: Exception) {}
    }

    private fun applyCustomVolume(vol: Float) {
        val l = if (isLRSwapped) rightVolume else leftVolume
        val r = if (isLRSwapped) leftVolume else rightVolume
        try {
            mediaPlayer?.setVolume(l * vol, r * vol)
        } catch (_: Exception) {}
    }

    /**
     * Test tone generators: 1000Hz (Click reference) and 440Hz (VS / A tuning tone).
     * Respects live L/R swap, stereo split mode, and channel volumes in real time.
     */
    fun startTestTone(type: TestToneType) {
        stopTestTone()
        if (type == TestToneType.NONE) return

        _activeTestTone.value = type
        val freq = if (type == TestToneType.CLICK_1KHZ) 1000.0 else 440.0
        val sampleRate = 44100
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        testToneTrack = track
        track.play()

        testToneJob = scope.launch(Dispatchers.Default) {
            val numFrames = 512
            val buffer = ShortArray(numFrames * 2)
            var phase = 0.0
            val step = 2.0 * PI * freq / sampleRate

            while (isActive && _activeTestTone.value != TestToneType.NONE) {
                for (i in 0 until numFrames) {
                    val s = (sin(phase) * 16000.0).toInt().toShort()
                    phase += step

                    if (type == TestToneType.CLICK_1KHZ) {
                        // Logical Left tone (Click/Guide reference)
                        buffer[i * 2] = s
                        buffer[i * 2 + 1] = 0
                    } else {
                        // Logical Right tone (VS/PA reference)
                        buffer[i * 2] = 0
                        buffer[i * 2 + 1] = s
                    }
                }

                applyStereoRoutingInPlace(
                    interleavedStereo = buffer,
                    frameCount = numFrames,
                    leftVol = leftVolume,
                    rightVol = rightVolume,
                    masterVol = masterVolume * fadeGain,
                    isStereoSplitEnabled = isStereoSplitEnabled,
                    isLRSwapped = isLRSwapped
                )

                track.write(buffer, 0, buffer.size)
            }
        }
    }

    fun stopTestTone() {
        _activeTestTone.value = TestToneType.NONE
        testToneJob?.cancel()
        testToneJob = null
        try {
            testToneTrack?.stop()
            testToneTrack?.release()
        } catch (_: Exception) {}
        testToneTrack = null
    }

    fun release() {
        stopCurrentPlayback()
        stopTestTone()
        progressJob?.cancel()
    }

    companion object {
        /**
         * Zero-allocation stereo channel routing & digital L/R inversion helper.
         * - When stereo split is disabled (Mono): mixes L and R into both physical channels.
         * - When stereo split is enabled and isLRSwapped = false: outputs L->L and R->R.
         * - When stereo split is enabled and isLRSwapped = true: outputs R->L and L->R.
         */
        fun applyStereoRoutingInPlace(
            interleavedStereo: ShortArray,
            frameCount: Int,
            leftVol: Float,
            rightVol: Float,
            masterVol: Float,
            isStereoSplitEnabled: Boolean,
            isLRSwapped: Boolean
        ) {
            val effMaster = masterVol.coerceIn(0f, 1f)
            val lGain = leftVol.coerceIn(0f, 1f) * effMaster
            val rGain = rightVol.coerceIn(0f, 1f) * effMaster

            for (i in 0 until frameCount) {
                val idx = i shl 1
                val srcL = interleavedStereo[idx].toInt()
                val srcR = interleavedStereo[idx + 1].toInt()
                if (!isStereoSplitEnabled) {
                    val mono = (srcL + srcR) * 0.5f
                    interleavedStereo[idx] = (mono * lGain).toInt().coerceIn(-32767, 32767).toShort()
                    interleavedStereo[idx + 1] = (mono * rGain).toInt().coerceIn(-32767, 32767).toShort()
                } else if (isLRSwapped) {
                    interleavedStereo[idx] = (srcR * lGain).toInt().coerceIn(-32767, 32767).toShort()
                    interleavedStereo[idx + 1] = (srcL * rGain).toInt().coerceIn(-32767, 32767).toShort()
                } else {
                    interleavedStereo[idx] = (srcL * lGain).toInt().coerceIn(-32767, 32767).toShort()
                    interleavedStereo[idx + 1] = (srcR * rGain).toInt().coerceIn(-32767, 32767).toShort()
                }
            }
        }
    }
}
