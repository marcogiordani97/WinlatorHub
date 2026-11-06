package com.legion.winlatorhub

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.legion.winlatorhub.data.AppDatabase
import com.legion.winlatorhub.data.PcGameEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val pcGameDao = db.pcGameDao()

    val games: StateFlow<List<PcGameEntity>> = pcGameDao.getAllGames()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    fun scanDirectory(treeUri: Uri) {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                withContext(Dispatchers.IO) {
                    val foundGames = mutableListOf<PcGameEntity>()
                    val context = getApplication<Application>()

                    val rootDir = DocumentFile.fromTreeUri(context, treeUri)
                    if (rootDir != null && rootDir.exists()) {
                        findExeFiles(rootDir, foundGames, maxDepth = 4, currentDepth = 0)
                    }

                    pcGameDao.clearAll()
                    if (foundGames.isNotEmpty()) {
                        pcGameDao.insertGames(foundGames)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isScanning.value = false
            }
        }
    }

    private fun findExeFiles(
        dir: DocumentFile,
        results: MutableList<PcGameEntity>,
        maxDepth: Int,
        currentDepth: Int
    ) {
        if (currentDepth > maxDepth) return

        val dirName = dir.name ?: ""
        if (dirName.startsWith(".") ||
            dirName.startsWith("$") ||
            dirName.equals("System Volume Information", ignoreCase = true) ||
            dirName.equals("Android", ignoreCase = true) ||
            dirName.equals("LOST.DIR", ignoreCase = true)
        ) {
            return
        }

        try {
            val files = dir.listFiles()
            for (file in files) {
                val fileName = file.name ?: continue
                if (fileName.startsWith(".") || fileName.startsWith("$")) continue

                if (file.isDirectory) {
                    if (currentDepth < maxDepth) {
                        findExeFiles(file, results, maxDepth, currentDepth + 1)
                    }
                } else if (file.isFile) {
                    if (fileName.endsWith(".exe", ignoreCase = true)) {
                        val gameName = if (dirName.isBlank() || dirName.equals("Games", ignoreCase = true)) {
                            fileName.removeSuffix(".exe")
                        } else {
                            dirName
                        }
                        val realPath = getRealPathFromUri(file.uri)
                        results.add(
                            PcGameEntity(
                                name = gameName,
                                exeName = fileName,
                                path = realPath
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getRealPathFromUri(uri: Uri): String {
        val uriString = uri.toString()
        if (uriString.contains("%3A") || uriString.contains(":")) {
            val decoded = Uri.decode(uriString)
            val docPart = when {
                decoded.contains("/document/") -> decoded.substringAfter("/document/")
                decoded.contains("/tree/") -> decoded.substringAfter("/tree/")
                else -> ""
            }
            if (docPart.contains(":")) {
                val split = docPart.split(":")
                val volume = split[0].substringAfterLast("/")
                val relPath = split.getOrNull(1) ?: ""
                return if (volume.equals("primary", ignoreCase = true)) {
                    "/storage/emulated/0/$relPath"
                } else {
                    "/storage/$volume/$relPath"
                }
            }
        }
        return uri.path ?: uriString
    }
}
