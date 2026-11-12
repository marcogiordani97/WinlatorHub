package com.legion.winlatorhub.scan

/**
 * Conversione dei document ID di ExternalStorageProvider in percorsi reali e poi in percorsi Windows.
 * Un'unica implementazione usata sia per la cartella radice sia per i singoli .exe, così i due
 * percorsi sono sempre coerenti tra loro.
 */
object PathMapper {

    /** Authority del provider di sistema per memoria interna, microSD e USB/SSD. */
    const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"

    /**
     * "4288-D0AD:Giochi/Gta" -> /storage/4288-D0AD/Giochi/Gta
     * "primary:Games"        -> /storage/emulated/0/Games
     * "home:Giochi"          -> /storage/emulated/0/Documents/Giochi
     * "raw:/storage/x/y"     -> /storage/x/y
     * split con limit = 2: i ':' dentro i nomi di cartella non troncano il percorso.
     */
    fun documentIdToPath(documentId: String): String? {
        if (documentId.startsWith("raw:")) return documentId.removePrefix("raw:").trimEnd('/')
        val parts = documentId.split(":", limit = 2)
        if (parts.size < 2 || parts[0].isBlank()) return null
        val volume = parts[0]
        val base = when (volume.lowercase()) {
            "primary" -> "/storage/emulated/0"
            "home" -> "/storage/emulated/0/Documents"
            else -> "/storage/$volume"
        }
        val relative = parts[1].trim('/')
        return if (relative.isEmpty()) base else "$base/$relative"
    }

    /**
     * Converte un percorso Android in un percorso Windows rispetto alla cartella radice,
     * che in Winlator deve essere mappata sulla lettera [drive].
     * Restituisce null se il file non si trova dentro la radice.
     */
    fun toWindowsPath(androidPath: String, root: String, drive: String): String? {
        val cleanRoot = root.trimEnd('/')
        if (androidPath != cleanRoot && !androidPath.startsWith("$cleanRoot/")) return null
        val relative = androidPath.removePrefix(cleanRoot).replace('/', '\\')
        return "$drive:${relative.ifEmpty { "\\" }}"
    }
}
