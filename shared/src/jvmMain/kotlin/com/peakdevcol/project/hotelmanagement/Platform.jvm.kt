package com.peakdevcol.project.hotelmanagement

actual fun platformName(): String =
    "${System.getProperty("os.name")} (JVM ${System.getProperty("java.version")})"
