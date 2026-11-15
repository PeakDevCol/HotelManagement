package com.peakdevcol.project.hotelmanagement.desktop

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AppDataDirectoryTest {
    private val temporaryFolder = File(System.getProperty("java.io.tmpdir")).canonicalPath

    @Test
    fun macKeepsTheDataInApplicationSupport() {
        val directory = appDataDirectory(osName = "Mac OS X", userHome = "/Users/ana", environment = { null })

        assertEquals(File("/Users/ana/Library/Application Support/HotelManagement"), directory)
    }

    @Test
    fun windowsKeepsTheDataInAppData() {
        val directory = appDataDirectory(
            osName = "Windows 11",
            userHome = "C:/Users/ana",
            environment = { name -> "C:/Users/ana/AppData/Roaming".takeIf { name == "APPDATA" } },
        )

        assertEquals(File("C:/Users/ana/AppData/Roaming/HotelManagement"), directory)
    }

    @Test
    fun linuxKeepsTheDataInTheUserDataFolder() {
        val directory = appDataDirectory(osName = "Linux", userHome = "/home/ana", environment = { null })

        assertEquals(File("/home/ana/.local/share/HotelManagement"), directory)
    }

    @Test
    fun theOfflineDatabaseIsNotStoredInTheTemporaryFolder() {
        val database = DesktopFirebasePlatform().getDatabasePath("firestore.db")

        assertFalse(database.canonicalPath.startsWith(temporaryFolder))
    }
}
