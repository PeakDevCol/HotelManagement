package com.peakdevcol.project.hotelmanagement.data.local

/** One small text kept on the device between launches of the app. */
internal interface LocalTextStore {
    fun read(): String?

    fun write(text: String)
}

/** The store named [name] inside [directory]. Each platform keeps it its own way. */
internal expect fun localTextStore(directory: String, name: String): LocalTextStore
