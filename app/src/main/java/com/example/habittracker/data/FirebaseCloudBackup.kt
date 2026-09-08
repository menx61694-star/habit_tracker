package com.example.habittracker.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirebaseCloudBackup {
    private const val BACKUP_DOCUMENT = "latest"

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val crashlytics: FirebaseCrashlytics by lazy { FirebaseCrashlytics.getInstance() }

    val currentUser get() = auth.currentUser

    suspend fun signIn(email: String, password: String): String {
        validateCredentials(email, password)
        crashlytics.log("Cloud backup sign-in started")
        return try {
            val uid = auth.signInWithEmailAndPassword(email.trim(), password).await().user?.uid
                ?: error("Sign-in succeeded but no user was returned")
            crashlytics.setUserId(uid)
            crashlytics.log("Cloud backup sign-in succeeded")
            uid
        } catch (error: Exception) {
            crashlytics.recordException(error)
            throw error
        }
    }

    suspend fun register(email: String, password: String): String {
        validateCredentials(email, password)
        crashlytics.log("Cloud backup account creation started")
        return try {
            val uid = auth.createUserWithEmailAndPassword(email.trim(), password).await().user?.uid
                ?: error("Registration succeeded but no user was returned")
            crashlytics.setUserId(uid)
            crashlytics.log("Cloud backup account creation succeeded")
            uid
        } catch (error: Exception) {
            crashlytics.recordException(error)
            throw error
        }
    }

    suspend fun uploadBackup(backupJson: String) {
        val uid = auth.currentUser?.uid ?: error("Sign in before uploading a backup")
        crashlytics.log("Cloud backup upload started")
        crashlytics.setCustomKey("backup_size_bytes", backupJson.toByteArray(Charsets.UTF_8).size)
        try {
            firestore.collection("users")
                .document(uid)
                .collection("backups")
                .document(BACKUP_DOCUMENT)
                .set(
                    mapOf(
                        "backupVersion" to 1,
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "backup" to backupJson
                    )
                )
                .await()
            crashlytics.log("Cloud backup upload succeeded")
        } catch (error: Exception) {
            crashlytics.recordException(error)
            throw error
        }
    }

    suspend fun downloadBackup(): String {
        val uid = auth.currentUser?.uid ?: error("Sign in before downloading a backup")
        crashlytics.log("Cloud backup download started")
        return try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .collection("backups")
                .document(BACKUP_DOCUMENT)
                .get()
                .await()

            val backup = snapshot.getString("backup")
                ?: error("No cloud backup was found for this account")
            crashlytics.setCustomKey("downloaded_backup_size_bytes", backup.toByteArray(Charsets.UTF_8).size)
            crashlytics.log("Cloud backup download succeeded")
            backup
        } catch (error: Exception) {
            crashlytics.recordException(error)
            throw error
        }
    }

    fun signOut() {
        auth.signOut()
        crashlytics.setUserId("")
        crashlytics.log("Cloud backup signed out")
    }

    private fun validateCredentials(email: String, password: String) {
        require(email.trim().contains("@")) { "Enter a valid email address" }
        require(password.length >= 6) { "Password must be at least 6 characters" }
    }
}
