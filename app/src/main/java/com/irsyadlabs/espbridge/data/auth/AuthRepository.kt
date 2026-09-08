package com.irsyadlabs.espbridge.data.auth

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.FirebaseException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.irsyadlabs.espbridge.BuildConfig
import com.irsyadlabs.espbridge.data.local.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

sealed interface AuthResult {
    data class Success(val uid: String?, val email: String?) : AuthResult
    data class Error(val message: String) : AuthResult
}

class AuthRepository(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    fun isFirebaseReady(): Boolean =
        BuildConfig.FIREBASE_CONFIG_PRESENT && FirebaseApp.getApps(context).isNotEmpty()

    fun currentUid(): String? = if (isFirebaseReady()) FirebaseAuth.getInstance().currentUser?.uid else null

    fun currentEmail(): String? = if (isFirebaseReady()) FirebaseAuth.getInstance().currentUser?.email else null

    fun isFirebaseSignedIn(): Boolean = isFirebaseReady() && FirebaseAuth.getInstance().currentUser != null

    suspend fun login(email: String, password: String): AuthResult {
        if (!isFirebaseReady()) {
            if (!email.contains('@') || password.length < 6) {
                return AuthResult.Error("Gunakan email valid dan password minimal 6 karakter untuk preview mode.")
            }
            settingsRepository.setDemoSession(email)
            return AuthResult.Success("demo", email)
        }
        return runAuthRequest("login") {
            val result = FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).await()
            result.user?.let { user -> syncProfile(user.uid, user.email, user.displayName, "password", false) }
            AuthResult.Success(result.user?.uid, result.user?.email)
        }
    }

    suspend fun register(email: String, password: String): AuthResult {
        if (!isFirebaseReady()) {
            if (!email.contains('@') || password.length < 6) {
                return AuthResult.Error("Gunakan email valid dan password minimal 6 karakter untuk preview mode.")
            }
            settingsRepository.setDemoSession(email)
            return AuthResult.Success("demo", email)
        }
        return runAuthRequest("register") {
            val result = FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).await()
            result.user?.let { user -> syncProfile(user.uid, user.email, user.displayName, "password", true) }
            AuthResult.Success(result.user?.uid, result.user?.email)
        }
    }

    suspend fun loginWithGoogleIdToken(idToken: String): AuthResult {
        if (!isFirebaseReady()) {
            settingsRepository.setDemoSession("demo.google@espbridge.local")
            return AuthResult.Success("demo-google", "demo.google@espbridge.local")
        }
        return runAuthRequest("google") {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = FirebaseAuth.getInstance().signInWithCredential(credential).await()
            result.user?.let { user -> syncProfile(user.uid, user.email, user.displayName, "google", false) }
            AuthResult.Success(result.user?.uid, result.user?.email)
        }
    }

    suspend fun demoGoogleLogin(): AuthResult {
        settingsRepository.setDemoSession("demo.google@espbridge.local")
        return AuthResult.Success("demo-google", "demo.google@espbridge.local")
    }

    suspend fun logout() {
        if (isFirebaseReady()) FirebaseAuth.getInstance().signOut()
        settingsRepository.clearLocalSession()
    }

    private fun syncProfile(
        uid: String,
        email: String?,
        displayName: String?,
        provider: String,
        isNew: Boolean
    ) {
        val now = System.currentTimeMillis()
        val values = mutableMapOf<String, Any?>(
            "email" to (email ?: ""),
            "displayName" to (displayName ?: email?.substringBefore('@').orEmpty()),
            "authProvider" to provider,
            "updatedAt" to now
        )
        values["photoUrl"] = null
        if (isNew) values["createdAt"] = now
        FirebaseDatabase.getInstance().reference
            .child("users")
            .child(uid)
            .child("profile")
            .updateChildren(values)
            .addOnFailureListener { error ->
                Log.w(TAG, "Profile sync failed after authentication", error)
            }
    }

    private suspend fun runAuthRequest(
        operation: String,
        request: suspend () -> AuthResult
    ): AuthResult = try {
        withTimeout(AUTH_TIMEOUT_MS) { request() }
    } catch (_: TimeoutCancellationException) {
        AuthResult.Error("Server terlalu lama merespons. Periksa internet lalu coba lagi.")
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        AuthResult.Error(friendlyMessage(error, operation))
    }

    private fun friendlyMessage(error: Throwable, operation: String): String {
        val code = (error as? FirebaseAuthException)?.errorCode?.lowercase()
            ?: error.message.orEmpty().lowercase()
            
        val isNetwork = error.javaClass.simpleName == "FirebaseNetworkException" || 
                        code.contains("network") || 
                        (error is FirebaseException && error.message?.contains("network", ignoreCase = true) == true)

        if (isNetwork) {
            return "Tidak ada koneksi internet. Login dan pendaftaran membutuhkan internet."
        }
        return when {
            code.contains("wrong-password") || code.contains("invalid-credential") || code.contains("invalid-login-credentials") ->
                "Email atau password salah."
            code.contains("user-not-found") -> "Akun belum terdaftar. Silakan pilih Register."
            code.contains("email-already-in-use") -> "Email sudah terdaftar. Silakan login."
            code.contains("invalid-email") -> "Format email tidak valid."
            code.contains("weak-password") -> "Password terlalu lemah. Gunakan minimal 6 karakter."
            code.contains("too-many-requests") -> "Terlalu banyak percobaan. Coba lagi beberapa saat."
            code.contains("operation-not-allowed") -> "Metode login ini belum diaktifkan di Firebase."
            operation == "google" -> "Google Sign-In gagal. Coba lagi."
            operation == "register" -> "Pendaftaran gagal. Periksa data dan coba lagi."
            else -> "Login gagal. Periksa email, password, dan koneksi internet."
        }
    }

    private companion object {
        const val TAG = "AuthRepository"
        const val AUTH_TIMEOUT_MS = 15_000L
    }
}
