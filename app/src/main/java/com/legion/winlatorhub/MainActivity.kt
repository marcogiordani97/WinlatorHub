package com.legion.winlatorhub

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.legion.winlatorhub.data.PcGameEntity

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val openDocumentTreeLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            viewModel.scanDirectory(uri)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val games by viewModel.games.collectAsState()
                val isScanning by viewModel.isScanning.collectAsState()

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Winlator Hub") },
                            actions = {
                                IconButton(onClick = { openDocumentTreeLauncher.launch(null) }) {
                                    Icon(Icons.Default.Sync, contentDescription = "Scansiona SSD")
                                }
                            }
                        )
                    }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        } else if (games.isEmpty()) {
                            Text(
                                "Nessun gioco PC trovato. Premi l'icona in alto per scansionare l'SSD.",
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(220.dp),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(games) { game ->
                                    GameCard(game = game) { launchWinlator(game) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun launchWinlator(game: PcGameEntity) {
        val winlatorIntent = packageManager.getLaunchIntentForPackage("com.winlator")
        if (winlatorIntent != null) {
            winlatorIntent.putExtra("exe_path", game.path)
            winlatorIntent.putExtra("game_path", game.path)
            winlatorIntent.putExtra("path", game.path)
            winlatorIntent.putExtra("game_name", game.name)

            val displayPath = if (game.path.isNotBlank()) game.path else game.exeName
            Toast.makeText(
                this,
                "Avvio Winlator...\nPercorso gioco: $displayPath",
                Toast.LENGTH_LONG
            ).show()
            startActivity(winlatorIntent)
        } else {
            Toast.makeText(this, "Winlator non installato!", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun GameCard(game: PcGameEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Computer,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = game.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = game.exeName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (game.path.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = game.path,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
