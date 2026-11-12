package com.legion.winlatorhub

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import com.legion.winlatorhub.data.PcGameEntity
import com.legion.winlatorhub.scan.PathMapper

/** Lettera di unità che nel container Winlator punta alla cartella radice dei giochi. */
const val DRIVE_LETTER = "D"

/** Container usato per il lancio. Da rendere configurabile quando sarà chiaro cosa legge il fork. */
private const val DEFAULT_CONTAINER_ID = 1

/** Se usi un altro fork, aggiungi il package qui e nel blocco <queries> del manifest. */
private val WINLATOR_PACKAGES = listOf(
    "com.winlator",
    "com.winlator.frost",
    "com.winlator.amod",
    "com.winlator.cmod",
    "com.winlator.glodroid"
)

object WinlatorLauncher {

    /** Avvia il gioco. Restituisce un messaggio d'errore, oppure null se Winlator è stato aperto. */
    fun launch(context: Context, game: PcGameEntity, rootPath: String?): String? {
        if (rootPath == null) {
            return "Cartella dei giochi non impostata: premi CARTELLA e sceglila."
        }
        val windowsPath = PathMapper.toWindowsPath(game.path, rootPath, DRIVE_LETTER)
            ?: return "Il gioco non è dentro la cartella mappata come $DRIVE_LETTER: in Winlator. Riscansiona."

        val pm = context.packageManager
        val intent = WINLATOR_PACKAGES.firstNotNullOfOrNull { pm.getLaunchIntentForPackage(it) }
            ?: return "Winlator non è installato sul dispositivo."

        // --- DA VERIFICARE ---------------------------------------------------------------
        // getLaunchIntentForPackage apre la schermata principale di Winlator. Non è confermato
        // che il tuo fork legga questi extra: controlla manifest e codice del fork.
        intent.action = Intent.ACTION_VIEW
        intent.putExtra("container_id", DEFAULT_CONTAINER_ID)
        intent.putExtra("path", windowsPath)
        intent.putExtra("exe_path", windowsPath)
        intent.putExtra("game_path", windowsPath)
        intent.putExtra("game_name", game.name)
        // ---------------------------------------------------------------------------------

        return try {
            context.startActivity(intent)
            null
        } catch (e: ActivityNotFoundException) {
            "Winlator non ha un'activity in grado di ricevere questa richiesta."
        } catch (e: SecurityException) {
            "Winlator ha rifiutato la richiesta (activity non esportata)."
        }
    }
}
