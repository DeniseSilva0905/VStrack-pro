package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.ChordTransposer
import com.example.data.model.SongEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import com.example.util.PowerPointLyricsParser
import com.example.util.RichParagraph
import com.example.util.RichSlide
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeleprompterScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val activeSong by viewModel.teleprompterSong.collectAsState()
    val allSongs by viewModel.allSongs.collectAsState()
    val scrollSpeedState by viewModel.teleprompterScrollSpeed.collectAsState()
    val fontSize by viewModel.teleprompterFontSize.collectAsState()
    val semitones by viewModel.teleprompterTransposeSemitones.collectAsState()
    val isAutoScroll by viewModel.isTeleprompterAutoScroll.collectAsState()

    val song = activeSong ?: allSongs.firstOrNull()
    val currentSongSpeed = (song?.scrollSpeed ?: scrollSpeedState.toInt()).coerceIn(1, 10)
    val listState = rememberLazyListState()

    var showSongSelector by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf("SLIDES") } // "SLIDES" or "CHORDS"
    var showImportMenu by remember { mutableStateOf(false) }

    // PowerPoint (.pptx) file pickers
    val pptForCurrentSongLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            if (song != null) {
                viewModel.importPowerPointLyricsForSong(song.id, it)
            } else {
                viewModel.importPowerPointAsNewSong(it)
            }
        }
    }

    val pptAsNewSongLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importPowerPointAsNewSong(it) }
    }

    // Auto-scroll loop tailored to EACH song's individual scroll speed (1 to 10)
    LaunchedEffect(isAutoScroll, currentSongSpeed) {
        if (isAutoScroll) {
            val delayMs = (100L - (currentSongSpeed * 8L)).coerceAtLeast(15L)
            val scrollDelta = 1.2f + (currentSongSpeed * 0.4f)
            while (true) {
                delay(delayMs)
                listState.scrollBy(scrollDelta)
            }
        }
    }

    // Transposed chords
    val displayedKey = remember(song, semitones) {
        val origKey = song?.musicalKey ?: "C"
        ChordTransposer.transposeKey(origKey, semitones)
    }

    val transposedChords = remember(song, semitones) {
        val original = song?.chords ?: "[Nenhuma cifra cadastrada para esta música]"
        ChordTransposer.transposeText(original, semitones)
    }

    // Parsed PowerPoint rich slides
    val richSlides: List<RichSlide> = remember(song?.lyricsRichJson) {
        val json = song?.lyricsRichJson ?: ""
        if (json.isNotBlank()) {
            PowerPointLyricsParser.slidesFromJson(json)
        } else {
            emptyList()
        }
    }

    val hasPowerPointSlides = richSlides.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PitchBlack)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        // TOP CONTROLS BAR: Slim, Compact & Feature-packed
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = SurfaceCardElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                // Row 1: Song title + Selector Button + PPT Upload + Play/Pause Auto-scroll
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showSongSelector = !showSongSelector }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = song?.title ?: "Selecione uma Música",
                                color = TextPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = if (showSongSelector) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = StageYellow,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${song?.artist?.ifBlank { "Ao Vivo" } ?: "Ao Vivo"} • Tom: $displayedKey",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            if (hasPowerPointSlides) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = StageYellow.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, StageYellow)
                                ) {
                                    Text(
                                        text = "PPTX (${richSlides.size} slides)",
                                        color = StageYellow,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // PowerPoint Upload Button
                        Box {
                            OutlinedButton(
                                onClick = { showImportMenu = true },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StageYellow),
                                border = androidx.compose.foundation.BorderStroke(1.dp, StageYellow),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Default.Slideshow, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("PPTX", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            DropdownMenu(
                                expanded = showImportMenu,
                                onDismissRequest = { showImportMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("Importar PPTX para esta música", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("Aplica fontes, cores e tamanhos à faixa atual", fontSize = 10.sp, color = TextSecondary)
                                        }
                                    },
                                    leadingIcon = { Icon(Icons.Default.UploadFile, contentDescription = null, tint = StageYellow) },
                                    onClick = {
                                        showImportMenu = false
                                        pptForCurrentSongLauncher.launch(
                                            arrayOf(
                                                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                                "application/vnd.ms-powerpoint",
                                                "*/*"
                                            )
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("Importar PPTX como nova música", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("Cria uma nova música no acervo com a letra", fontSize = 10.sp, color = TextSecondary)
                                        }
                                    },
                                    leadingIcon = { Icon(Icons.Default.AddBox, contentDescription = null, tint = StageGreen) },
                                    onClick = {
                                        showImportMenu = false
                                        pptAsNewSongLauncher.launch(
                                            arrayOf(
                                                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                                "application/vnd.ms-powerpoint",
                                                "*/*"
                                            )
                                        )
                                    }
                                )
                            }
                        }

                        // Auto-scroll Play / Pause Toggle Button
                        Button(
                            onClick = { viewModel.toggleTeleprompterAutoScroll() },
                            modifier = Modifier.height(30.dp).testTag("teleprompter_scroll_toggle"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAutoScroll) StageGreen else StageYellow,
                                contentColor = PitchBlack
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (isAutoScroll) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Rolagem Automática",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isAutoScroll) "ROLANDO" else "ROLAR",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Row 2: Per-song Scroll Speed Stepper + Font Size + Chords/Slides view switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Per-Song Scroll Speed Controller (1 to 10)
                    Surface(
                        color = SurfaceCard,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "Velocidade: ",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    if (song != null) {
                                        viewModel.updateSongScrollSpeed(song.id, currentSongSpeed - 1)
                                    } else {
                                        viewModel.setTeleprompterSpeed((currentSongSpeed - 1).toFloat())
                                    }
                                },
                                enabled = currentSongSpeed > 1,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = null, tint = if (currentSongSpeed > 1) StageYellow else TextMuted, modifier = Modifier.size(12.dp))
                            }
                            Text(
                                text = "${currentSongSpeed}x",
                                color = StageYellow,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                            IconButton(
                                onClick = {
                                    if (song != null) {
                                        viewModel.updateSongScrollSpeed(song.id, currentSongSpeed + 1)
                                    } else {
                                        viewModel.setTeleprompterSpeed((currentSongSpeed + 1).toFloat())
                                    }
                                },
                                enabled = currentSongSpeed < 10,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = if (currentSongSpeed < 10) StageYellow else TextMuted, modifier = Modifier.size(12.dp))
                            }
                        }
                    }

                    // Font Zoom Control
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.changeTeleprompterFontSize(-2) },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.TextDecrease, contentDescription = "Diminuir Fonte", tint = TextSecondary, modifier = Modifier.size(14.dp))
                        }
                        Text(
                            text = "${fontSize}sp",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { viewModel.changeTeleprompterFontSize(+2) },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.TextIncrease, contentDescription = "Aumentar Fonte", tint = StageYellow, modifier = Modifier.size(14.dp))
                        }
                    }

                    // View Switcher (Slides formatados vs Cifras)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Surface(
                            modifier = Modifier
                                .height(26.dp)
                                .clickable { viewMode = "SLIDES" },
                            shape = RoundedCornerShape(4.dp),
                            color = if (viewMode == "SLIDES") StageYellow else SurfaceCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (viewMode == "SLIDES") StageYellow else SurfaceCardBorder)
                        ) {
                            Text(
                                text = "Letra PPTX",
                                color = if (viewMode == "SLIDES") PitchBlack else TextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .height(26.dp)
                                .clickable { viewMode = "CHORDS" },
                            shape = RoundedCornerShape(4.dp),
                            color = if (viewMode == "CHORDS") WarningAmber else SurfaceCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (viewMode == "CHORDS") WarningAmber else SurfaceCardBorder)
                        ) {
                            Text(
                                text = "Cifras",
                                color = if (viewMode == "CHORDS") PitchBlack else TextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Expandable Quick Song Selector Tray
        AnimatedVisibility(visible = showSongSelector) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                color = SurfaceCardElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    Text(
                        text = "Selecione uma música da biblioteca para carregar letra e velocidade:",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(allSongs) { itemSong ->
                            val isSelected = itemSong.id == song?.id
                            Surface(
                                modifier = Modifier.clickable {
                                    viewModel.setTeleprompterSong(itemSong)
                                    showSongSelector = false
                                },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) StageYellow else SurfaceCard,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) StageYellow else SurfaceCardBorder
                                )
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text(
                                        text = itemSong.title,
                                        color = if (isSelected) PitchBlack else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${itemSong.scrollSpeed}x • ${if (itemSong.lyricsRichJson.isNotBlank()) "PPTX" else "TXT"}",
                                        color = if (isSelected) PitchBlack else TextSecondary,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // MAIN TELEPROMPTER CANVAS: High-Contrast Stage Display
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(10.dp),
            color = PitchBlack,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                if (viewMode == "CHORDS") {
                    // CHORDS MODE: Transposable chords with monospace font
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CIFRAS DE PALCO (Tom: $displayedKey)",
                                color = WarningAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = { viewModel.transposeTeleprompter(-1) },
                                    modifier = Modifier.height(26.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("-1", fontSize = 10.sp, color = TextPrimary)
                                }
                                Spacer(modifier = Modifier.width(3.dp))
                                OutlinedButton(
                                    onClick = { viewModel.transposeTeleprompter(+1) },
                                    modifier = Modifier.height(26.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("+1", fontSize = 10.sp, color = TextPrimary)
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = transposedChords,
                            color = StageYellow,
                            fontSize = fontSize.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = (fontSize * 1.4).sp
                        )
                    }

                    item {
                        HorizontalDivider(color = SurfaceCardBorder, thickness = 1.dp)
                    }

                    item {
                        Text(
                            text = "LETRA COMPLETA (TEXTO)",
                            color = InEarCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    item {
                        Text(
                            text = song?.lyrics?.ifBlank { "[Nenhuma letra cadastrada. Faça upload de um arquivo PowerPoint acima]" } ?: "",
                            color = TextPrimary,
                            fontSize = (fontSize - 2).sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = (fontSize * 1.5).sp
                        )
                    }
                } else {
                    // SLIDES MODE: Exact PowerPoint Interpretation with preserved fonts, colors, sizes & styles!
                    if (hasPowerPointSlides) {
                        items(richSlides) { slide ->
                            SlideCard(
                                slide = slide,
                                baseFontSize = fontSize.toFloat()
                            )
                        }
                    } else {
                        // Empty / Fallback state encouraging upload
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = SurfaceCardElevated,
                                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Slideshow,
                                        contentDescription = null,
                                        tint = StageYellow,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Nenhum PowerPoint importado para '${song?.title ?: "esta música"}'",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Faça upload de uma apresentação .pptx com a letra para assumir automaticamente as fontes, cores, tamanhos e alinhamentos originais dos slides!",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = {
                                            pptForCurrentSongLauncher.launch(
                                                arrayOf(
                                                    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                                    "application/vnd.ms-powerpoint",
                                                    "*/*"
                                                )
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = StageYellow, contentColor = PitchBlack),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp), tint = PitchBlack)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Fazer Upload do PowerPoint (.pptx)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // Also show standard lyrics if available
                        if (!song?.lyrics.isNullOrBlank()) {
                            item {
                                Text(
                                    text = "LETRA EM TEXTO SIMPLES",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            item {
                                Text(
                                    text = song?.lyrics ?: "",
                                    color = TextPrimary,
                                    fontSize = fontSize.sp,
                                    lineHeight = (fontSize * 1.5).sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders a single PowerPoint slide preserving colors, font sizes, families, bold, italic and alignments.
 */
@Composable
private fun SlideCard(
    slide: RichSlide,
    baseFontSize: Float
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = SurfaceCard.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Slide Header Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = PitchBlack,
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StageYellow.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "SLIDE ${slide.slideNumber}",
                        color = StageYellow,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Slide Paragraphs with rich styling
            slide.paragraphs.forEach { paragraph ->
                ParagraphContent(
                    paragraph = paragraph,
                    baseFontSize = baseFontSize
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun ParagraphContent(
    paragraph: RichParagraph,
    baseFontSize: Float
) {
    // Relative scaling factor based on user font size control (24sp = 1.0 scale)
    val scaleFactor = (baseFontSize / 24f).coerceIn(0.6f, 2.0f)

    val annotatedString = remember(paragraph, scaleFactor) {
        val builder = AnnotatedString.Builder()
        paragraph.runs.forEach { run ->
            val color = parseHexColor(run.colorHex)
            val scaledSp = (run.fontSizeSp * scaleFactor).sp
            val weight = if (run.isBold) FontWeight.Bold else FontWeight.Normal
            val style = if (run.isItalic) FontStyle.Italic else FontStyle.Normal
            val family = resolveFontFamily(run.fontFamily)

            builder.withStyle(
                SpanStyle(
                    color = color,
                    fontSize = scaledSp,
                    fontWeight = weight,
                    fontStyle = style,
                    fontFamily = family
                )
            ) {
                append(run.text)
            }
        }
        builder.toAnnotatedString()
    }

    val textAlign = when (paragraph.alignment) {
        "left" -> TextAlign.Start
        "right" -> TextAlign.End
        "justify" -> TextAlign.Justify
        else -> TextAlign.Center
    }

    Text(
        text = annotatedString,
        textAlign = textAlign,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun parseHexColor(hex: String): Color {
    return try {
        val cleanHex = if (hex.startsWith("#")) hex else "#$hex"
        Color(android.graphics.Color.parseColor(cleanHex))
    } catch (_: Exception) {
        Color.White
    }
}

private fun resolveFontFamily(name: String): FontFamily {
    val lower = name.lowercase()
    return when {
        lower.contains("mono") || lower.contains("courier") || lower.contains("consolas") -> FontFamily.Monospace
        lower.contains("serif") || lower.contains("times") || lower.contains("georgia") || lower.contains("garamond") -> FontFamily.Serif
        lower.contains("cursive") || lower.contains("script") -> FontFamily.Cursive
        else -> FontFamily.SansSerif
    }
}
