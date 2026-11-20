package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.PlaybackState
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.SpotifyMiniPlayer
import com.example.ui.screens.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VSTracksAppScaffold(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isShowActive by viewModel.isShowActive.collectAsState()
    val allSetlists by viewModel.allSetlists.collectAsState()
    val playbackState by viewModel.audioEngine.playbackState.collectAsState()
    val showPlayConfirmation by viewModel.showPlayConfirmationDialog.collectAsState()
    val uiToast by viewModel.uiToast.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Handle back button
    BackHandler(enabled = currentScreen != AppScreen.HOME) {
        if (!viewModel.navigateBack()) {
            viewModel.navigateTo(AppScreen.HOME)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = StageGraphite,
                drawerContentColor = TextPrimary,
                modifier = Modifier.width(300.dp)
            ) {
                // Drawer Brand Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceCardElevated)
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.logo_vs_pro),
                                contentDescription = "Logo VS+ PRO",
                                modifier = Modifier
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Seu palco, seu repertório, seu som.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Scrollable Drawer Navigation Items
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 12.dp)
                ) {
                    NavigationDrawerItem(
                        label = { Text("1. Início") },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        selected = currentScreen == AppScreen.HOME,
                        onClick = {
                            viewModel.navigateTo(AppScreen.HOME)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )

                    NavigationDrawerItem(
                        label = { Text("2. Minha Biblioteca") },
                        icon = { Icon(Icons.Default.LibraryMusic, contentDescription = null) },
                        selected = currentScreen == AppScreen.LIBRARY,
                        onClick = {
                            viewModel.navigateTo(AppScreen.LIBRARY)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )

                    NavigationDrawerItem(
                        label = { Text("3. Repertórios & Shows") },
                        icon = { Icon(Icons.Default.QueueMusic, contentDescription = null) },
                        selected = currentScreen == AppScreen.SETLISTS,
                        onClick = {
                            viewModel.navigateTo(AppScreen.SETLISTS)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )

                    // Sub-tab directly below Repertórios & Shows
                    NavigationDrawerItem(
                        label = {
                            Text(
                                text = "Repertórios Salvos",
                                fontSize = 13.sp,
                                fontWeight = if (currentScreen == AppScreen.SAVED_SETLISTS) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.SubdirectoryArrowRight,
                                contentDescription = null,
                                tint = StageYellow,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        badge = {
                            Text(
                                text = "${allSetlists.size}",
                                color = StageYellow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        selected = currentScreen == AppScreen.SAVED_SETLISTS,
                        onClick = {
                            viewModel.navigateTo(AppScreen.SAVED_SETLISTS)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier
                            .padding(start = 28.dp, end = 12.dp, top = 0.dp, bottom = 2.dp)
                            .testTag("drawer_saved_setlists_subtab")
                    )

                NavigationDrawerItem(
                    label = {
                        Text(
                            "4. MODO SHOW AO VIVO",
                            fontWeight = FontWeight.Bold,
                            color = if (currentScreen == AppScreen.STAGE_SHOW) PitchBlack else StageGreen
                        )
                    },
                    icon = { Icon(Icons.Default.PlayCircleFilled, contentDescription = null, tint = StageGreen) },
                    selected = currentScreen == AppScreen.STAGE_SHOW,
                    colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = StageGreen),
                    onClick = {
                        viewModel.navigateTo(AppScreen.STAGE_SHOW)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    label = { Text("5. Teleprompter (Cifras/Letras)") },
                    icon = { Icon(Icons.Default.TextFields, contentDescription = null) },
                    selected = currentScreen == AppScreen.TELEPROMPTER,
                    onClick = {
                        viewModel.navigateTo(AppScreen.TELEPROMPTER)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    label = { Text("6. Mixer Multipista (Stems)") },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null) },
                    selected = currentScreen == AppScreen.MIXER,
                    onClick = {
                        viewModel.navigateTo(AppScreen.MIXER)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    label = { Text("7. Áudio e Split Estéreo L/R") },
                    icon = { Icon(Icons.Default.Headphones, contentDescription = null) },
                    selected = currentScreen == AppScreen.STEREO_SPLIT,
                    onClick = {
                        viewModel.navigateTo(AppScreen.STEREO_SPLIT)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    label = { Text("8. Pedais Bluetooth & Atalhos") },
                    icon = { Icon(Icons.Default.SettingsInputComponent, contentDescription = null) },
                    selected = currentScreen == AppScreen.PEDAL,
                    onClick = {
                        viewModel.navigateTo(AppScreen.PEDAL)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    label = { Text("9. Histórico de Shows") },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    selected = currentScreen == AppScreen.HISTORY,
                    onClick = {
                        viewModel.navigateTo(AppScreen.HISTORY)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    label = { Text("10. Meu Perfil") },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    selected = currentScreen == AppScreen.PROFILE,
                    onClick = {
                        viewModel.navigateTo(AppScreen.PROFILE)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                    NavigationDrawerItem(
                        label = { Text("11. Lixeira") },
                        icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
                        selected = currentScreen == AppScreen.TRASH,
                        onClick = {
                            viewModel.navigateTo(AppScreen.TRASH)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .background(StageGraphite),
            topBar = {
                if (currentScreen != AppScreen.HOME) {
                    // Stage Top Bar for Sub-screens: Slim & Compact
                    Surface(
                        color = PitchBlack,
                        tonalElevation = 2.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 10.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        scope.launch { drawerState.open() }
                                    },
                                    modifier = Modifier.size(32.dp).testTag("app_drawer_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Menu Principal",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                Text(
                                    text = currentScreen.title,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.navigateTo(AppScreen.HOME) },
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                    modifier = Modifier.height(26.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StageYellow),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, StageYellow)
                                ) {
                                    Icon(imageVector = Icons.Default.Home, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("INÍCIO", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 12.dp, end = 12.dp, bottom = 14.dp)
                ) {
                    SpotifyMiniPlayer(viewModel = viewModel)
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Active Screen Switcher
                when (currentScreen) {
                    AppScreen.HOME -> HomeScreen(
                        viewModel = viewModel,
                        onOpenDrawer = { scope.launch { drawerState.open() } }
                    )
                    AppScreen.LIBRARY -> LibraryScreen(viewModel = viewModel)
                    AppScreen.SETLISTS -> SetlistsScreen(viewModel = viewModel)
                    AppScreen.SAVED_SETLISTS -> SavedSetlistsScreen(viewModel = viewModel)
                    AppScreen.STAGE_SHOW -> StageShowScreen(viewModel = viewModel)
                    AppScreen.TELEPROMPTER -> TeleprompterScreen(viewModel = viewModel)
                    AppScreen.MIXER -> MixerScreen(viewModel = viewModel)
                    AppScreen.STEREO_SPLIT -> AudioSettingsScreen(viewModel = viewModel)
                    AppScreen.PEDAL -> PedalScreen(viewModel = viewModel)
                    AppScreen.HISTORY -> HistoryScreen(viewModel = viewModel)
                    AppScreen.PROFILE -> ProfileScreen(viewModel = viewModel)
                    AppScreen.TRASH -> TrashScreen(viewModel = viewModel)
                }

                // Floating Visual Feedback Toast
                AnimatedVisibility(
                    visible = uiToast != null,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                ) {
                    uiToast?.let { msg ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = SurfaceCardElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, StageYellow),
                            tonalElevation = 10.dp
                        ) {
                            Text(
                                text = msg,
                                color = StageYellowLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal: Play Safety Confirmation Dialog
    if (showPlayConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPlayConfirmation() },
            title = { Text("Confirmar Disparo de Palco", color = TextPrimary) },
            text = {
                Text(
                    "Deseja iniciar o playback agora no PA e fones?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmPlayAction() },
                    colors = ButtonDefaults.buttonColors(containerColor = StageGreen)
                ) {
                    Text("Disparar Agora", color = PitchBlack, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPlayConfirmation() }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = SurfaceCardElevated
        )
    }

    // Modal: Accidental Tap Safety Confirmation during Live Show (Sim / Não)
    val safetyConfirmDialogVisible by viewModel.safetyConfirmDialogVisible.collectAsState()
    val pendingSafetySongTitle by viewModel.pendingSafetySongTitle.collectAsState()

    if (safetyConfirmDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSafetyPlay() },
            title = {
                Text(
                    text = "Confirmar troca de música no Show",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Tem certeza que deseja tocar a música \"$pendingSafetySongTitle\" agora no show?",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmSafetyPlay() },
                    colors = ButtonDefaults.buttonColors(containerColor = StageYellow)
                ) {
                    Text("Sim", color = PitchBlack, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissSafetyPlay() }) {
                    Text("Não", color = TextSecondary)
                }
            },
            containerColor = SurfaceCardElevated
        )
    }
}
