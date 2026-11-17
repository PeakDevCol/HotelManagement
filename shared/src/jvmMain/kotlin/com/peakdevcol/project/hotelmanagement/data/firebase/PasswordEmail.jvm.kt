package com.peakdevcol.project.hotelmanagement.data.firebase

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.app
import dev.gitlive.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

private const val SEND_EMAIL_URL = "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode"
private val TIMEOUT = Duration.ofSeconds(15)

// V1: the JVM Firebase SDK does not implement sendPasswordResetEmail, so the desktop asks the
// Firebase Auth REST API for the same email, with the API key of its own Firebase app.
internal actual suspend fun FirebaseAuth.sendPasswordEmail(email: String) = withContext(Dispatchers.IO) {
    val apiKey = URLEncoder.encode(Firebase.app.options.apiKey, Charsets.UTF_8)
    val request = HttpRequest.newBuilder(URI.create("$SEND_EMAIL_URL?key=$apiKey"))
        .timeout(TIMEOUT)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString("""{"requestType":"PASSWORD_RESET","email":"${email.jsonEscaped()}"}"""))
        .build()
    val response = HttpClient.newBuilder().connectTimeout(TIMEOUT).build().send(request, HttpResponse.BodyHandlers.ofString())
    if (response.statusCode() !in 200..299) throw IOException("Firebase did not send the password email: HTTP ${response.statusCode()}")
}

private fun String.jsonEscaped(): String = replace("\\", "\\\\").replace("\"", "\\\"")
