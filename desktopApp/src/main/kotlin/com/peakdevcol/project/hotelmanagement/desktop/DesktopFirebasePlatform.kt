package com.peakdevcol.project.hotelmanagement.desktop

import com.google.firebase.FirebasePlatform
import java.io.File
import java.util.prefs.Preferences

/** Where the JVM Firebase SDK keeps the session and the offline data between launches. */
internal class DesktopFirebasePlatform(
    private val dataDirectory: File = appDataDirectory(),
) : FirebasePlatform() {

    private val session = Preferences.userRoot().node("com/peakdevcol/project/hotelmanagement/firebase")

    override fun store(key: String, value: String) = session.put(key, value)

    override fun retrieve(key: String): String? = session.get(key, null)

    override fun clear(key: String) = session.remove(key)

    override fun log(msg: String) = println(msg)

    // V2: the SDK defaults to the system temporary folder, which the operating system empties.
    // Orders saved offline wait in this database, so it must live where nothing deletes it.
    override fun getDatabasePath(name: String): File {
        dataDirectory.mkdirs()
        return File(dataDirectory, name)
    }
}

private const val APP_FOLDER = "HotelManagement"

/** The folder each operating system reserves for the data of an application. */
internal fun appDataDirectory(
    osName: String = System.getProperty("os.name"),
    userHome: String = System.getProperty("user.home"),
    environment: (String) -> String? = System::getenv,
): File {
    val os = osName.lowercase()
    val base = when {
        "mac" in os -> File(userHome, "Library/Application Support")
        "win" in os -> environment("APPDATA")?.let(::File) ?: File(userHome, "AppData/Roaming")
        else -> environment("XDG_DATA_HOME")?.let(::File) ?: File(userHome, ".local/share")
    }
    return File(base, APP_FOLDER)
}
