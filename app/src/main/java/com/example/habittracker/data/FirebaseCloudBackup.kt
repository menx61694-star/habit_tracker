package com.example.habittracker.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirebaseCloudBackup {
    private const val BACKUP_DOCUMENT = "latest"

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    val currentUser get() = auth.currentUser

    suspend fun signIn(email: String, password: String): String {
        validateCredentials(email, password)
        return auth.signInWithEmailAndPassword(email.trim(), password).await().user?.uid
            ?: error("Sign-in succeeded but no user was returned")
    }

    suspend fun register(email: String, password: String): String {
        validateCredentials(email, password)
        return auth.createUserWithEmailAndPassword(email.trim(), password).await().user?.uid
            ?: error("Registration succeeded but no user was returned")
    }

    suspend fun uploadBackup(backupJson: String) {
        val uid = auth.currentUser?.uid ?: error("Sign in before uploading a backup")
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
    }

    suspend fun downloadBackup(): String {
        val uid = auth.currentUser?.uid ?: error("Sign in before downloading a backup")
        val snapshot = firestore.collection("users")
            .document(uid)
            .collection("backups")
            .document(BACKUP_DOCUMENT)
            .get()
            .await()

        return snapshot.getString("backup")
            ?: error("No cloud backup was found for this account")
    }

    fun signOut() {
        auth.signOut()
    }

    private fun validateCredentials(email: String, password: String) {
        require(email.trim().contains("@")) { "Enter a valid email address" }
        require(password.length >= 6) { "Password must be at least 6 characters" }
    }
}
