package com.peakdevcol.project.hotelmanagement.data.local

import platform.Foundation.NSUserDefaults

// On iOS the user defaults already survive between launches, so no folder is needed.
internal actual fun localTextStore(directory: String, name: String): LocalTextStore = UserDefaultsTextStore(name)

private class UserDefaultsTextStore(private val key: String) : LocalTextStore {
    override fun read(): String? = NSUserDefaults.standardUserDefaults.stringForKey(key)

    override fun write(text: String) = NSUserDefaults.standardUserDefaults.setObject(text, forKey = key)
}
