package com.example.data.repository

import com.example.data.local.ChatDao
import com.example.data.local.ChatMessageEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import android.util.Log

class ChatRepository(private val chatDao: ChatDao) {
    val allMessages: Flow<List<ChatMessageEntity>> = chatDao.getAllMessages()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun insertMessage(message: ChatMessageEntity) {
        chatDao.insertMessage(message)
        syncToCloud(message)
    }

    suspend fun updateMessage(message: ChatMessageEntity) {
        chatDao.updateMessage(message)
        syncToCloud(message)
    }

    suspend fun deleteMessageById(id: String) {
        chatDao.deleteMessageById(id)
        val uid = auth.currentUser?.uid ?: return
        try {
            firestore.collection("users").document(uid).collection("chats").document(id).delete()
        } catch (e: Exception) {
            Log.e("ChatRepository", "Failed to delete from cloud", e)
        }
    }
    
    suspend fun clearHistory() {
        chatDao.clearAll()
        // Note: deleting a collection from Android client is not recommended for large collections, 
        // but can be implemented individually if needed.
    }

    private fun syncToCloud(message: ChatMessageEntity) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).collection("chats").document(message.id)
            .set(message)
            .addOnFailureListener { e ->
                Log.e("ChatRepository", "Failed to sync message to cloud", e)
            }
    }

    // Call this upon login to restore messages from cloud
    suspend fun restoreFromCloud() {
        val uid = auth.currentUser?.uid ?: return
        try {
            val snapshot = firestore.collection("users").document(uid).collection("chats").get().await()
            val messages = snapshot.toObjects(ChatMessageEntity::class.java)
            messages.forEach { chatDao.insertMessage(it) }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Failed to restore from cloud", e)
        }
    }
}
