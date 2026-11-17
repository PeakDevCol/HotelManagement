package com.peakdevcol.project.hotelmanagement.data.firebase

import dev.gitlive.firebase.auth.FirebaseAuth

internal actual suspend fun FirebaseAuth.sendPasswordEmail(email: String) = sendPasswordResetEmail(email)
