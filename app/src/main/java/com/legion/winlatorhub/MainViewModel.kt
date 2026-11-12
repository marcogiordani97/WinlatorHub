package com.legion.winlatorhub

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.legion.winlatorhub.data.AppDatabase
import com.legion.winlatorhub.data.PcGameEntity
import com.legion.winlatorhub.data.SettingsStore
import com.legion.winlatorhub.scan.DocumentScanner
import com.legion.winlatorhub.scan.GameDetector
import com.legion.winlatorhub.scan.PathMapper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val pcGameDao = AppDatabase.getDatabase(application).pcGameDao()
    private val settings = SettingsStore(application)
    private val resolver = application.contentResolver

    val games: StateFlow<List<PcGameEntity>> = pcGameDao.getAllGames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    /** Messaggi una tantum da mostrare in snackbar. */
    val messages: Flow<String> = _messages.receiveAsFlow()

    private var scanJob: Job? = null

    /** Percorso reale della cartella mappata su D: in Winlator. */
    val rootPath: String? get() = settings.rootPath

    /** Riscansiona la cartella già scelta. Restituisce false se bisogna sceglierne una. */
    fun rescan(): Boolean {
        val uri = settings.treeUri ?: return false
        val stillGranted = resolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }
        if (!stillGranted) {
            _messages.trySend("Permesso sulla cartella scaduto: sceglila di nuovo.")
            return false
        }
        startScan(uri)
        return true
    }

    fun onFolderPicked(uri: Uri) {
        if (uri.authority != PathMapper.EXTERNAL_STORAGE_AUTHORITY) {
            _messages.trySend("Scegli una cartella della memoria del dispositivo, della microSD o dell'SSD USB.")
            return
        }
        try {
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: SecurityException) {
            _messages.trySend("Impossibile mantenere l'accesso alla cartella: dovrai sceglierla a ogni avvio.")
        }
        startScan(uri)
    }

    private fun startScan(treeUri: Uri) {
        // Chiamato solo dal main thread: il controllo non ha race.
        if (scanJob?.isActive == true) return
        scanJob = viewModelScope.launch {
            _isScanning.value = true
            try {
                val count = withContext(Dispatchers.IO) { scanAndStore(treeUri) }
                _messages.send(
                    if (count == 0) "Nessun gioco trovato nella cartella scelta."
                    else "Libreria aggiornata: $count giochi."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // La libreria e la cartella precedenti restano intatte.
                _messages.send("Scansione non riuscita: ${e.message ?: e.javaClass.simpleName}")
            } finally {
                _isScanning.value = false
            }
        }
    }

    private suspend fun scanAndStore(treeUri: Uri): Int {
        val rootDocId = android.provider.DocumentsContract.getTreeDocumentId(treeUri)
        val rootPath = PathMapper.documentIdToPath(rootDocId)
            ?: error("percorso della cartella non riconosciuto ($rootDocId)")

        val tree = DocumentScanner(resolver, treeUri).scan()
        val entities = GameDetector.detect(tree).mapNotNull { game ->
            val path = PathMapper.documentIdToPath(game.exe.documentId) ?: return@mapNotNull null
            PcGameEntity(name = game.name, exeName = game.exe.name, path = path)
        }

        pcGameDao.replaceAll(entities)
        val previous = settings.treeUri
        settings.saveRoot(treeUri, rootPath)
        if (previous != null && previous != treeUri) releasePermission(previous)
        return entities.size
    }

    private fun releasePermission(uri: Uri) {
        try {
            resolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            // Permesso già rilasciato: niente da fare.
        }
    }
}
