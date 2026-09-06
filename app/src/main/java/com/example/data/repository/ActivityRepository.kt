package com.example.data.repository

import com.example.data.local.ActivityLogDao
import com.example.data.local.ActivityLogEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import android.util.Log

class ActivityRepository(private val activityLogDao: ActivityLogDao) {
    val allLogs: Flow<List<ActivityLogEntity>> = activityLogDao.getAllLogs()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun insertLog(log: ActivityLogEntity) {
        activityLogDao.insertLog(log)
        syncToCloud(log)
    }

    suspend fun clearHistory() {
        activityLogDao.deleteAllLogs()
    }

    private fun syncToCloud(log: ActivityLogEntity) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).collection("activity").document(log.id)
            .set(log)
            .addOnFailureListener { e ->
                Log.e("ActivityRepository", "Failed to sync log to cloud", e)
            }
    }

    suspend fun restoreFromCloud() {
        val uid = auth.currentUser?.uid ?: return
        try {
            val snapshot = firestore.collection("users").document(uid).collection("activity").get().await()
            val logs = snapshot.toObjects(ActivityLogEntity::class.java)
            logs.forEach { activityLogDao.insertLog(it) }
        } catch (e: Exception) {
            Log.e("ActivityRepository", "Failed to restore logs from cloud", e)
        }
    }
}
