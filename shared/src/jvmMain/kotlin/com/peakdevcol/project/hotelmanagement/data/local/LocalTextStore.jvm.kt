package com.peakdevcol.project.hotelmanagement.data.local

import java.io.File

internal actual fun localTextStore(directory: String, name: String): LocalTextStore = FileTextStore(File(directory, name))

private class FileTextStore(private val file: File) : LocalTextStore {
    override fun read(): String? = if (file.exists()) file.readText() else null

    override fun write(text: String) {
        file.parentFile?.mkdirs()
        file.writeText(text)
    }
}
