package com.example.travelallowance.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.YearMonth

class TaRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    fun isAnonymous(): Boolean = auth.currentUser?.isAnonymous ?: true
    suspend fun linkEmailPassword(email: String, password: String) {
        val user = auth.currentUser ?: error("No active user")
        val credential = EmailAuthProvider.getCredential(email.trim(), password)
        user.linkWithCredential(credential).await()
    }
    suspend fun signInEmailPassword(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }
    suspend fun ensureUser(): String {
        if (auth.currentUser == null) auth.signInAnonymously().await()
        return auth.currentUser!!.uid
    }
    private suspend fun uid() = ensureUser()
    suspend fun saveProfile(profile: EmployeeProfile) {
        db.collection("users").document(uid()).collection("profile").document("main").set(profile).await()
    }
    suspend fun loadProfile(): EmployeeProfile? = db.collection("users").document(uid()).collection("profile").document("main").get().await().toObject(EmployeeProfile::class.java)
    suspend fun saveSettings(settings: TaSettings) {
        db.collection("users").document(uid()).collection("settings").document("ta").set(settings).await()
    }
    suspend fun loadSettings(): TaSettings = db.collection("users").document(uid()).collection("settings").document("ta").get().await().toObject(TaSettings::class.java) ?: TaSettings()
    suspend fun saveJourney(journey: Journey) {
        val month = journey.date.take(7)
        val ref = db.collection("users").document(uid()).collection("months").document(month)
        ref.set(mapOf("month" to month), com.google.firebase.firestore.SetOptions.merge()).await()
        ref.collection("journeys").document(journey.id).set(journey).await()
    }
    suspend fun loadMonth(month: YearMonth): List<Journey> {
        val snap = db.collection("users").document(uid()).collection("months").document(month.toString()).collection("journeys").get().await()
        return snap.documents.mapNotNull { it.toObject(Journey::class.java) }.sortedByDescending { it.date }
    }
    suspend fun deleteJourney(month: String, id: String) {
        db.collection("users").document(uid()).collection("months").document(month).collection("journeys").document(id).delete().await()
    }
}
