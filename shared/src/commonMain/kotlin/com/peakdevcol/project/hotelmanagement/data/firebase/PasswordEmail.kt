package com.peakdevcol.project.hotelmanagement.data.firebase

import dev.gitlive.firebase.auth.FirebaseAuth

/**
 * Asks Firebase to email the link to choose a password. Each platform does it its own way,
 * because the desktop SDK does not implement it (V1).
 */
internal expect suspend fun FirebaseAuth.sendPasswordEmail(email: String)
