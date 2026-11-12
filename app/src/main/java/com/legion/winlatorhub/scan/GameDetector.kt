package com.legion.winlatorhub.scan

data class ExeFile(val name: String, val documentId: String, val size: Long)

/** Una cartella letta dal disco: gli .exe che contiene direttamente e le sottocartelle. */
data class ScanNode(
    val name: String,
    val documentId: String,
    val exes: List<ExeFile> = emptyList(),
    val children: List<ScanNode> = emptyList()
)

data class DetectedGame(val name: String, val exe: ExeFile)

/**
 * Trasforma l'albero delle cartelle in una lista di giochi: un gioco per cartella,
 * con l'eseguibile più probabile, scartando installer, uninstaller, redistribuibili e crash handler.
 */
object GameDetector {

    private val ignoredExeFragments = listOf(
        "setup", "install", "redist", "vcredist", "vc_redist", "dxsetup", "dxwebsetup",
        "crashhandler", "crashreport", "crashpad", "crashsender", "bugsplat", "bugreport",
        "dotnetfx", "ndp4", "prereq", "physx", "oalinst", "dxdiag", "notification_helper",
        "updater", "quicksfv", "browsersubprocess", "cefsubprocess", "unitycrash"
    )

    private val ignoredDirNames = setOf(
        "system volume information", "android", "lost.dir", "_commonredist", "commonredist",
        "redist", "_redist", "redistributables", "directx", "vcredist", "__installer",
        "installer", "installers", "support", "prerequisites", "dotnet", "physx"
    )

    /** Cartelle che raccolgono più giochi e non sono un gioco esse stesse. */
    private val containerDirNames = setOf(
        "games", "giochi", "pc games", "pcgames", "gog games", "epic games", "steamapps", "common"
    )

    /** Sottocartelle tipiche dell'interno di un gioco: se ci sono, la cartella è un gioco unico. */
    private val gameInternalDirNames = setOf(
        "bin", "bin32", "bin64", "binaries", "win32", "win64", "x86", "x64", "engine",
        "data", "game", "system", "redist", "_commonredist"
    )

    fun isIgnoredDir(name: String): Boolean {
        val n = name.lowercase()
        return n.startsWith(".") || n.startsWith("$") || n in ignoredDirNames
    }

    fun isCandidateExe(fileName: String): Boolean {
        val lower = fileName.lowercase()
        if (!lower.endsWith(".exe")) return false
        val base = lower.removeSuffix(".exe")
        if (base.startsWith("unins") || base.startsWith(".") || base.startsWith("$")) return false
        if (base == "7z" || base == "7za") return false
        return ignoredExeFragments.none { it in base }
    }

    fun detect(root: ScanNode): List<DetectedGame> {
        val pruned = prune(root) ?: return emptyList()
        val out = mutableListOf<DetectedGame>()
        // Gli .exe direttamente nella radice sono giochi singoli.
        pruned.exes.forEach { out += DetectedGame(displayName(it.name), it) }
        pruned.children.forEach { classify(it, out) }
        return out
            .distinctBy { it.exe.documentId }
            .sortedBy { it.name.lowercase() }
    }

    /** Rimuove .exe da scartare e cartelle che non contengono nessun candidato. */
    private fun prune(node: ScanNode): ScanNode? {
        val exes = node.exes.filter { isCandidateExe(it.name) }
        val children = node.children.filterNot { isIgnoredDir(it.name) }.mapNotNull { prune(it) }
        if (exes.isEmpty() && children.isEmpty()) return null
        return node.copy(exes = exes, children = children)
    }

    private fun classify(node: ScanNode, out: MutableList<DetectedGame>) {
        val isContainer = node.exes.isEmpty() && (
            node.name.lowercase() in containerDirNames ||
                (node.children.size >= 2 && node.children.none { it.name.lowercase() in gameInternalDirNames })
            )
        if (isContainer) {
            node.children.forEach { classify(it, out) }
        } else {
            pickBestExe(node)?.let { out += DetectedGame(node.name, it) }
        }
    }

    private fun normalize(s: String) = s.lowercase().filter { it.isLetterOrDigit() }

    private fun pickBestExe(game: ScanNode): ExeFile? {
        val folder = normalize(game.name)
        val all = mutableListOf<Pair<ExeFile, Int>>()
        fun collect(n: ScanNode, depth: Int) {
            n.exes.forEach { all += it to depth }
            n.children.forEach { collect(it, depth + 1) }
        }
        collect(game, 0)

        return all.maxWithOrNull(
            compareBy<Pair<ExeFile, Int>> { (exe, depth) ->
                val base = normalize(displayName(exe.name))
                var score = -5 * depth
                if (folder.isNotEmpty() && base.isNotEmpty()) {
                    if (base == folder) score += 100
                    else if (base.contains(folder) || folder.contains(base)) score += 50
                }
                if ("shipping" in base) score += 20
                if ("launcher" in base) score -= 10
                score
            }.thenBy { it.first.size }
        )?.first
    }

    private fun displayName(fileName: String) =
        fileName.substring(0, fileName.length - 4)
}
