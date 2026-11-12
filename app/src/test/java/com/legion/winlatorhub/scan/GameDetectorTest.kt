package com.legion.winlatorhub.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameDetectorTest {

    private fun exe(name: String, size: Long = 1) = ExeFile(name, "id:$name:$size", size)
    private fun dir(name: String, vararg children: ScanNode, exes: List<ExeFile> = emptyList()) =
        ScanNode(name, "id:$name", exes, children.toList())

    @Test
    fun `scarta installer e utility`() {
        listOf("unins000.exe", "setup.exe", "vc_redist.x64.exe", "UnityCrashHandler64.exe", "DXSETUP.exe")
            .forEach { assertFalse(it, GameDetector.isCandidateExe(it)) }
        listOf("Witcher3.exe", "game.EXE", "Celeste.exe").forEach { assertTrue(it, GameDetector.isCandidateExe(it)) }
    }

    @Test
    fun `un gioco per cartella con l'eseguibile giusto`() {
        val root = dir(
            "",
            dir(
                "Celeste",
                exes = listOf(exe("Celeste.exe", 10), exe("unins000.exe", 99), exe("Helper.exe", 50))
            ),
            dir("Hades", dir("x64", exes = listOf(exe("Hades.exe", 5))), dir("_CommonRedist", exes = listOf(exe("big.exe", 999))))
        )
        val games = GameDetector.detect(root)
        assertEquals(listOf("Celeste", "Hades"), games.map { it.name })
        assertEquals("Celeste.exe", games[0].exe.name)
        assertEquals("Hades.exe", games[1].exe.name)
    }

    @Test
    fun `cartella contenitore con più giochi`() {
        val root = dir(
            "",
            dir(
                "Games",
                dir("Alpha", exes = listOf(exe("alpha.exe"))),
                dir("Beta", exes = listOf(exe("beta.exe")))
            )
        )
        assertEquals(listOf("Alpha", "Beta"), GameDetector.detect(root).map { it.name })
    }

    @Test
    fun `sottocartelle interne non spezzano il gioco`() {
        val root = dir(
            "",
            dir(
                "MyGame",
                dir("Engine", dir("Binaries", exes = listOf(exe("CrashReportClient.exe"), exe("Tool.exe", 1)))),
                dir("MyGame", dir("Binaries", dir("Win64", exes = listOf(exe("MyGame-Win64-Shipping.exe", 80)))))
            )
        )
        val games = GameDetector.detect(root)
        assertEquals(1, games.size)
        assertEquals("MyGame-Win64-Shipping.exe", games[0].exe.name)
    }

    @Test
    fun `exe nella radice e cartelle vuote`() {
        val root = dir("", dir("Vuota"), exes = listOf(exe("Doom.exe")))
        assertEquals(listOf("Doom"), GameDetector.detect(root).map { it.name })
    }
}
