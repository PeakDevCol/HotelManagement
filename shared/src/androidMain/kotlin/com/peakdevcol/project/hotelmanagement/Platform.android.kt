package com.peakdevcol.project.hotelmanagement

import android.os.Build

actual fun platformName(): String = "Android ${Build.VERSION.RELEASE}"
