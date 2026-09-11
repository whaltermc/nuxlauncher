package com.movtery.zalithlauncher.social

import com.google.gson.annotations.SerializedName

/**
 * Model data profil publik user
 */
data class NuxUserProfile(
    val uid: String = "",
    val username: String = "",
    @SerializedName("photoURL") val photoURL: String = "",
    val email: String = "",
    val status: String = "offline",
    val lastOnline: Long? = null,
    val isVerified: Boolean = false,
    val isPremium: Boolean = false
)

/**
 * Model data teman & relasi pertemanan
 */
data class NuxFriend(
    val uid: String,
    val username: String = "",
    val photoUrl: String = "",
    val status: String = "", // "accepted", "pending_sent", "pending_received"
    val lastMessageTime: Long? = null,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val isInGame: Boolean = false,
    val lastOnline: Long? = null,
    val presenceStatus: String = "offline",
    val isTyping: Boolean = false,
    val isVerified: Boolean = false,
    val isPremium: Boolean = false
) {
    val isAccepted: Boolean get() = status == "accepted"
    val isPendingSent: Boolean get() = status == "pending_sent"
    val isPendingReceived: Boolean get() = status == "pending_received"
}

/**
 * Model kutipan reply chat
 */
data class NuxChatReply(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val isVerified: Boolean = false
)

/**
 * Model pesan direct chat
 */
data class NuxChatMessage(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val imageUrl: String? = null,
    val replyTo: NuxChatReply? = null,
    val isVerified: Boolean = false
)

/**
 * Model peserta voice room
 */
data class NuxParticipant(
    val uid: String = "",
    val username: String = "",
    @SerializedName("photoURL") val photoURL: String = "",
    val isSpeaking: Boolean = false,
    val isVerified: Boolean = false
)

/**
 * Model data voice room
 */
data class NuxVoiceRoom(
    val id: String = "",
    val name: String = "",
    val hostUid: String = "",
    val password: String = "",
    val maxUsers: Int = 5,
    val participants: Map<String, NuxParticipant> = emptyMap()
) {
    val isLocked: Boolean get() = password.isNotEmpty()
    val participantCount: Int get() = participants.size
    val isFull: Boolean get() = participantCount >= maxUsers
}

/**
 * Model pesan teks di dalam voice room
 */
data class NuxVoiceMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    @SerializedName("senderPhotoURL") val senderPhotoURL: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val isVerified: Boolean = false
)
