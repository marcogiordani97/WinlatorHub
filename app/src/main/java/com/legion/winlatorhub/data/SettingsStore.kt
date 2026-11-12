package com.legion.winlatorhub.data

import android.content.Context
import android.net.Uri
import androidx.core.content.edit

/** Cartella radice dei giochi: URI del selettore (per riscansionare) e percorso reale (per Winlator). */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("winlator_hub_prefs", Context.MODE_PRIVATE)

    val rootPath: String? get() = prefs.getString(KEY_ROOT_PATH, null)

    val treeUri: Uri? get() = prefs.getString(KEY_TREE_URI, null)?.let(Uri::parse)

    fun saveRoot(treeUri: Uri, rootPath: String) = prefs.edit {
        putString(KEY_TREE_URI, treeUri.toString())
        putString(KEY_ROOT_PATH, rootPath)
    }

    private companion object {
        const val KEY_ROOT_PATH = "root_path"
        const val KEY_TREE_URI = "tree_uri"
    }
}
