package com.legion.winlatorhub.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PathMapperTest {

    @Test
    fun `volume USB`() {
        assertEquals("/storage/4288-D0AD/Giochi/Gta", PathMapper.documentIdToPath("4288-D0AD:Giochi/Gta"))
    }

    @Test
    fun `memoria interna e radice del volume`() {
        assertEquals("/storage/emulated/0/Games", PathMapper.documentIdToPath("primary:Games"))
        assertEquals("/storage/emulated/0", PathMapper.documentIdToPath("primary:"))
        assertEquals("/storage/4288-D0AD", PathMapper.documentIdToPath("4288-D0AD:"))
    }

    @Test
    fun `i due punti nei nomi non troncano il percorso`() {
        assertEquals(
            "/storage/4288-D0AD/Giochi/Halo: MCC/mcc.exe",
            PathMapper.documentIdToPath("4288-D0AD:Giochi/Halo: MCC/mcc.exe")
        )
    }

    @Test
    fun `document id non riconosciuto`() {
        assertNull(PathMapper.documentIdToPath("senzaduepunti"))
        assertEquals("/storage/x/y", PathMapper.documentIdToPath("raw:/storage/x/y"))
    }

    @Test
    fun `percorso windows relativo alla radice`() {
        assertEquals(
            "D:\\Gta\\gta.exe",
            PathMapper.toWindowsPath("/storage/4288-D0AD/Giochi/Gta/gta.exe", "/storage/4288-D0AD/Giochi", "D")
        )
        assertEquals("D:\\", PathMapper.toWindowsPath("/storage/X", "/storage/X/", "D"))
    }

    @Test
    fun `fuori dalla radice o con prefisso simile`() {
        assertNull(PathMapper.toWindowsPath("/storage/X/Altro/a.exe", "/storage/X/Giochi", "D"))
        assertNull(PathMapper.toWindowsPath("/storage/X/Giochi2/a.exe", "/storage/X/Giochi", "D"))
    }
}
