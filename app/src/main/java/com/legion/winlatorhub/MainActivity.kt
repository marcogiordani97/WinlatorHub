package com.legion.winlatorhub

import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.legion.winlatorhub.data.PcGameEntity
import com.legion.winlatorhub.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Barre di sistema sempre chiare su sfondo scuro, anche con il tema di sistema chiaro.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        setContent {
            WinlatorHubTheme {
                HubScreen(viewModel)
            }
        }
    }
}

@Composable
private fun HubScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val games by viewModel.games.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    var selectedGameId by rememberSaveable { mutableStateOf<Int?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) viewModel.onFolderPicked(uri)
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    val onPlay: (PcGameEntity) -> Unit = { game ->
        WinlatorLauncher.launch(context, game, viewModel.rootPath)?.let { error ->
            scope.launch { snackbarHostState.showSnackbar(error) }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = SteamBgGradEnd,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenGradient)
                .padding(padding)
        ) {
            SteamHeader(
                totalGamesCount = games.size,
                isScanning = isScanning,
                onRescanClick = { if (!viewModel.rescan()) pickFolder.launch(null) },
                onPickFolderClick = { pickFolder.launch(null) }
            )

            // Durante una riscansione la libreria resta visibile: solo una barra sottile sopra.
            Box(Modifier.fillMaxWidth().height(3.dp)) {
                if (isScanning && games.isNotEmpty()) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxSize(),
                        color = SteamFocusBlue,
                        trackColor = Color.Transparent
                    )
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    games.isNotEmpty() -> GameGrid(
                        games = games,
                        selectedGameId = selectedGameId,
                        onSelect = { selectedGameId = it.id },
                        onPlay = onPlay
                    )
                    isScanning -> ScanningPlaceholder(Modifier.align(Alignment.Center))
                    else -> EmptyLibrary(Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
private fun GameGrid(
    games: List<PcGameEntity>,
    selectedGameId: Int?,
    onSelect: (PcGameEntity) -> Unit,
    onPlay: (PcGameEntity) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(170.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(games, key = { it.id }, contentType = { "game" }) { game ->
            SteamGameCard(
                game = game,
                isSelected = selectedGameId == game.id,
                onSelect = { onSelect(game) },
                onPlay = { onPlay(game) }
            )
        }
    }
}

@Composable
private fun ScanningPlaceholder(modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = SteamFocusBlue)
        Spacer(Modifier.height(16.dp))
        Text(
            "ANALISI SSD ESTERNO...",
            color = SteamFocusBlue,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun EmptyLibrary(modifier: Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Gamepad, null, tint = SteamTextSecondary, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            "LA TUA LIBRERIA È VUOTA",
            color = SteamTextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Collega l'SSD, premi CARTELLA e scegli la cartella che in Winlator è l'unità $DRIVE_LETTER:.",
            color = SteamTextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SteamHeader(
    totalGamesCount: Int,
    isScanning: Boolean,
    onRescanClick: () -> Unit,
    onPickFolderClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SteamTopBarBg)
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Gamepad, null, tint = SteamFocusBlue, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "WINLATOR HUB",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = Color.White,
                    letterSpacing = 1.5.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HeaderButton("SCANSIONA", Icons.Default.Refresh, enabled = !isScanning, onClick = onRescanClick)
                HeaderButton("CARTELLA", Icons.Default.Folder, enabled = !isScanning, onClick = onPickFolderClick)
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            "TUTTI I GIOCHI ($totalGamesCount)",
            color = SteamTextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun HeaderButton(label: String, icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .background(SteamButtonBg)
            .border(1.dp, SteamFocusBlue, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

private val CardShape = RoundedCornerShape(8.dp)

/**
 * Touch: un tocco seleziona, il lancio parte da GIOCA.
 * Gamepad/tastiera: il focus seleziona la card, A / Invio la avvia.
 */
@Composable
fun SteamGameCard(
    game: PcGameEntity,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onPlay: () -> Unit
) {
    val title = remember(game.name) { game.name.uppercase() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(CardShape)
            .background(CardGradient)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) SteamFocusBlue else SteamCardBorder,
                shape = CardShape
            )
            .onFocusChanged { if (it.isFocused) onSelect() }
            .onPreviewKeyEvent { event ->
                val confirm = event.key == Key.ButtonA || event.key == Key.Enter ||
                    event.key == Key.DirectionCenter || event.key == Key.NumPadEnter
                if (confirm && event.type == KeyEventType.KeyUp) onPlay()
                confirm
            }
            .clickable(onClick = onSelect)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .background(SteamBadgeBg, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("USB $DRIVE_LETTER:", color = SteamFocusBlue, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }

            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(56.dp).background(SteamIconBoxBg, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Gamepad,
                        contentDescription = null,
                        tint = if (isSelected) SteamFocusBlue else SteamTextSecondary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = title,
                    color = SteamTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = game.exeName,
                    color = SteamTextSecondary,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isSelected) {
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(containerColor = SteamGreenPlay),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth().height(34.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("GIOCA", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .background(SteamIdleButtonBg, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("SELEZIONA", color = SteamTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
