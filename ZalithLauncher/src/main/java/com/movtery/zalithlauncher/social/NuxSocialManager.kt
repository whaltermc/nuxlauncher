package com.movtery.zalithlauncher.social

import android.content.Intent
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.movtery.zalithlauncher.auth.NuxAuthManager
import com.movtery.zalithlauncher.auth.NuxUser
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Pengelola data sosial: Pertemanan, Direct Chat, dan Voice Room
 * Beroperasi langsung pada Firebase Realtime Database dan sinkron dengan versi Windows
 */
object NuxSocialManager {
    private const val RTDB_BASE = "https://nux-launcher-default-rtdb.asia-southeast1.firebasedatabase.app"
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // State Flows
    private val _friends = MutableStateFlow<List<NuxFriend>>(emptyList())
    val friends: StateFlow<List<NuxFriend>> = _friends.asStateFlow()

    private val _activeChatFriend = MutableStateFlow<NuxFriend?>(null)
    val activeChatFriend: StateFlow<NuxFriend?> = _activeChatFriend.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<NuxChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<NuxChatMessage>> = _chatMessages.asStateFlow()

    private val _isFriendTyping = MutableStateFlow(false)
    val isFriendTyping: StateFlow<Boolean> = _isFriendTyping.asStateFlow()

    private val _voiceRooms = MutableStateFlow<List<NuxVoiceRoom>>(emptyList())
    val voiceRooms: StateFlow<List<NuxVoiceRoom>> = _voiceRooms.asStateFlow()

    private val _activeVoiceRoom = MutableStateFlow<NuxVoiceRoom?>(null)
    val activeVoiceRoom: StateFlow<NuxVoiceRoom?> = _activeVoiceRoom.asStateFlow()

    private val _voiceMessages = MutableStateFlow<List<NuxVoiceMessage>>(emptyList())
    val voiceMessages: StateFlow<List<NuxVoiceMessage>> = _voiceMessages.asStateFlow()

    private val _searchResults = MutableStateFlow<List<NuxUserProfile>>(emptyList())
    val searchResults: StateFlow<List<NuxUserProfile>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var syncJob: Job? = null
    private var chatJob: Job? = null
    private var voiceRoomSyncJob: Job? = null
    private var typingJob: Job? = null
    private var presenceHeartbeatJob: Job? = null

    fun startPresenceHeartbeat() {
        if (presenceHeartbeatJob?.isActive == true) return
        presenceHeartbeatJob = scope.launch {
            while (isActive) {
                try {
                    val user = getCurrentUser()
                    if (user != null) {
                        updateMyPresence()
                    }
                } catch (_: Exception) {}
                delay(30_000L)
            }
        }
    }

    fun stopPresenceHeartbeat(setOffline: Boolean = true) {
        presenceHeartbeatJob?.cancel()
        presenceHeartbeatJob = null
        if (setOffline) {
            updateMyPresenceBlocking("offline")
        }
    }

    suspend fun updateMyPresence(forcedStatus: String? = null) = withContext(Dispatchers.IO) {
        val user = getCurrentUser() ?: return@withContext
        val token = getAuthToken() ?: return@withContext

        try {
            val isGameRunning = com.movtery.zalithlauncher.ui.activities.VMActivity.isGameRunning
            val currentStatus = forcedStatus ?: if (isGameRunning) "in_game" else "online"

            val payload = JSONObject().apply {
                put("status", currentStatus)
                put("lastOnline", System.currentTimeMillis())
            }.toString()

            val body = payload.toRequestBody(JSON_MEDIA)

            // 1. Update users/{uid}/profile
            try {
                val req1 = Request.Builder()
                    .url("$RTDB_BASE/users/${user.uid}/profile.json?auth=$token")
                    .patch(body)
                    .build()
                val resp1 = client.newCall(req1).execute()
                resp1.close()
            } catch (_: Exception) {}

            // 2. Also update android-users/{uid}/profile
            try {
                val req2 = Request.Builder()
                    .url("$RTDB_BASE/android-users/${user.uid}/profile.json?auth=$token")
                    .patch(body)
                    .build()
                val resp2 = client.newCall(req2).execute()
                resp2.close()
            } catch (_: Exception) {}

            Log.d("NuxPresence", "updateMyPresence ($currentStatus) updated for ${user.username}")
        } catch (_: Exception) {}
    }

    fun updateMyPresenceAsync(forcedStatus: String? = null) {
        scope.launch {
            try {
                updateMyPresence(forcedStatus)
            } catch (_: Exception) {}
        }
    }

    fun updateMyPresenceBlocking(forcedStatus: String? = null) {
        val user = getCurrentUser() ?: return
        val t = Thread {
            try {
                val token = runBlocking { getAuthToken() } ?: return@Thread
                val isGameRunning = com.movtery.zalithlauncher.ui.activities.VMActivity.isGameRunning
                val currentStatus = forcedStatus ?: if (isGameRunning) "in_game" else "online"

                val payload = JSONObject().apply {
                    put("status", currentStatus)
                    put("lastOnline", System.currentTimeMillis())
                }.toString()

                val body = payload.toRequestBody(JSON_MEDIA)

                // 1. Update users/{uid}/profile
                try {
                    val req1 = Request.Builder()
                        .url("$RTDB_BASE/users/${user.uid}/profile.json?auth=$token")
                        .patch(body)
                        .build()
                    val resp1 = client.newCall(req1).execute()
                    Log.d("NuxPresence", "updateMyPresenceBlocking (users): status=$currentStatus, code=${resp1.code}")
                    resp1.close()
                } catch (e: Exception) {
                    Log.w("NuxPresence", "updateMyPresenceBlocking (users) failed", e)
                }

                // 2. Update android-users/{uid}/profile
                try {
                    val req2 = Request.Builder()
                        .url("$RTDB_BASE/android-users/${user.uid}/profile.json?auth=$token")
                        .patch(body)
                        .build()
                    val resp2 = client.newCall(req2).execute()
                    Log.d("NuxPresence", "updateMyPresenceBlocking (android-users): status=$currentStatus, code=${resp2.code}")
                    resp2.close()
                } catch (e: Exception) {
                    Log.w("NuxPresence", "updateMyPresenceBlocking (android-users) failed", e)
                }
            } catch (e: Exception) {
                Log.e("NuxPresence", "updateMyPresenceBlocking error", e)
            }
        }
        t.start()
        try {
            t.join(2500)
        } catch (_: Exception) {}
    }

    fun startSync() {
        startPresenceHeartbeat()
        if (syncJob?.isActive == true) return
        syncJob = scope.launch {
            try {
                NuxAuthManager.syncProfileToPublic()
                updateMyPresence()
            } catch (_: Exception) {}

            while (isActive) {
                try {
                    fetchFriends()
                    fetchVoiceRooms()
                } catch (_: Exception) {}
                delay(3500)
            }
        }
    }

    fun stopSync() {
        syncJob?.cancel()
        syncJob = null
        closeChat()
    }

    private suspend fun getAuthToken(): String? {
        return NuxAuthManager.getValidIdToken()
    }

    private fun getCurrentUser(): NuxUser? {
        return NuxAuthManager.currentUser
    }

    /**
     * Memuat daftar teman dari node users/{myUid}/friends
     */
    suspend fun fetchFriends() = withContext(Dispatchers.IO) {
        val user = getCurrentUser() ?: return@withContext
        val token = getAuthToken() ?: return@withContext

        try {
            val url = "$RTDB_BASE/users/${user.uid}/friends.json?auth=$token"
            val req = Request.Builder().url(url).get().build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string() ?: ""
            resp.close()

            if (!resp.isSuccessful || body.isEmpty() || body == "null") {
                _friends.value = emptyList()
                return@withContext
            }

            val type = object : TypeToken<Map<String, String>>() {}.type
            val friendsMap: Map<String, String> = gson.fromJson(body, type) ?: emptyMap()

            // Muat profil dan chatMeta untuk setiap teman
            val friendList = mutableListOf<NuxFriend>()
            val now = System.currentTimeMillis()
            for ((fUid, status) in friendsMap) {
                var profile = fetchUserProfile(fUid, token)
                var unreadCount = 0
                var lastMsgTime: Long? = null

                try {
                    val metaUrl = "$RTDB_BASE/users/${user.uid}/chatMeta/$fUid.json?auth=$token"
                    val metaResp = client.newCall(Request.Builder().url(metaUrl).get().build()).execute()
                    val metaBody = metaResp.body?.string() ?: ""
                    metaResp.close()
                    if (metaBody.isNotEmpty() && metaBody != "null") {
                        val metaJson = JSONObject(metaBody)
                        unreadCount = metaJson.optInt("unreadCount", 0)
                        lastMsgTime = if (metaJson.has("lastMessageTime")) metaJson.optLong("lastMessageTime") else null
                    }
                } catch (_: Exception) {}

                val pStatus = profile?.status ?: "offline"
                val pLastOnline = profile?.lastOnline
                val isFresh = if (pLastOnline != null) (now - pLastOnline) < 75_000L else false
                val isOnline = (pStatus == "online" || pStatus == "in_game") && isFresh
                val isInGame = pStatus == "in_game" && isFresh

                friendList.add(
                    NuxFriend(
                        uid = fUid,
                        username = profile?.username ?: "Pengguna",
                        photoUrl = profile?.photoURL ?: "",
                        status = status,
                        lastMessageTime = lastMsgTime,
                        unreadCount = unreadCount,
                        isOnline = isOnline,
                        isInGame = isInGame,
                        lastOnline = pLastOnline,
                        presenceStatus = if (isOnline) pStatus else "offline",
                        isVerified = profile?.isVerified ?: false,
                        isPremium = profile?.isPremium ?: false
                    )
                )
            }

            _friends.value = friendList.sortedWith(
                compareByDescending<NuxFriend> { it.lastMessageTime ?: 0L }
                    .thenBy { it.username.lowercase() }
            )
        } catch (_: Exception) {}
    }

    private data class CachedProfile(val profile: NuxUserProfile, val timestamp: Long)
    private val profileCache = java.util.concurrent.ConcurrentHashMap<String, CachedProfile>()

    /**
     * Mengambil profil publik pengguna dari users/{uid}/profile atau android-users/{uid}/profile
     */
    private suspend fun fetchUserProfile(uid: String, token: String): NuxUserProfile? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cached = profileCache[uid]
        if (cached != null && (now - cached.timestamp) < 8000L) {
            return@withContext cached.profile
        }

        try {
            // Coba dari users/{uid}/profile terlebih dahulu
            val url = "$RTDB_BASE/users/$uid/profile.json?auth=$token"
            val resp = client.newCall(Request.Builder().url(url).get().build()).execute()
            val body = resp.body?.string() ?: ""
            resp.close()

            if (resp.isSuccessful && body.isNotEmpty() && body != "null") {
                val json = JSONObject(body)
                val username = json.optString("username", "")
                val photoURL = json.optString("photoURL", json.optString("photoUrl", ""))
                val status = json.optString("status", "offline")
                val lastOnline = if (json.has("lastOnline")) json.optLong("lastOnline") else null
                val isVerified = json.optBoolean("verified", false) ||
                    json.optBoolean("isVerified", false) ||
                    json.optString("badge", "").equals("verified", ignoreCase = true) ||
                    json.optString("role", "").equals("verified", ignoreCase = true) ||
                    json.optBoolean("is_verified", false)
                val sub = json.optJSONObject("subscription")
                val isPremium = json.optBoolean("isPremium", false) || (sub != null && sub.has("tier"))
                if (username.isNotEmpty()) {
                    val profile = NuxUserProfile(
                        uid = uid,
                        username = username,
                        photoURL = photoURL,
                        status = status,
                        lastOnline = lastOnline,
                        isVerified = isVerified,
                        isPremium = isPremium
                    )
                    profileCache[uid] = CachedProfile(profile, now)
                    return@withContext profile
                }
            }

            // Fallback ke android-users/{uid}/profile
            val altUrl = "$RTDB_BASE/android-users/$uid/profile.json?auth=$token"
            val altResp = client.newCall(Request.Builder().url(altUrl).get().build()).execute()
            val altBody = altResp.body?.string() ?: ""
            altResp.close()

            if (altResp.isSuccessful && altBody.isNotEmpty() && altBody != "null") {
                val json = JSONObject(altBody)
                val username = json.optString("username", "")
                val photoURL = json.optString("photoURL", json.optString("photoUrl", ""))
                val status = json.optString("status", "offline")
                val lastOnline = if (json.has("lastOnline")) json.optLong("lastOnline") else null
                val isVerified = json.optBoolean("verified", false) ||
                    json.optBoolean("isVerified", false) ||
                    json.optString("badge", "").equals("verified", ignoreCase = true) ||
                    json.optString("role", "").equals("verified", ignoreCase = true) ||
                    json.optBoolean("is_verified", false)
                val sub = json.optJSONObject("subscription")
                val isPremium = json.optBoolean("isPremium", false) || (sub != null && sub.has("tier"))
                val profile = NuxUserProfile(
                    uid = uid,
                    username = username,
                    photoURL = photoURL,
                    status = status,
                    lastOnline = lastOnline,
                    isVerified = isVerified,
                    isPremium = isPremium
                )
                profileCache[uid] = CachedProfile(profile, now)
                return@withContext profile
            }
        } catch (_: Exception) {}
        null
    }

    /**
     * Mencari user berdasarkan username (pada node users dan android-users)
     */
    suspend fun searchUser(query: String) = withContext(Dispatchers.IO) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            _searchResults.value = emptyList()
            return@withContext
        }

        val myUid = getCurrentUser()?.uid ?: ""
        val token = getAuthToken() ?: return@withContext
        _isSearching.value = true

        val results = mutableListOf<NuxUserProfile>()
        try {
            // Cari di node users
            val winUrl = "$RTDB_BASE/users.json?auth=$token"
            val winResp = client.newCall(Request.Builder().url(winUrl).get().build()).execute()
            val winBody = winResp.body?.string() ?: ""
            winResp.close()

            if (winResp.isSuccessful && winBody.isNotEmpty() && winBody != "null") {
                val json = JSONObject(winBody)
                for (key in json.keys()) {
                    if (key == myUid) continue
                    val uObj = json.optJSONObject(key) ?: continue
                    val prof = uObj.optJSONObject("profile") ?: continue
                    val uname = prof.optString("username", "")
                    if (uname.lowercase().contains(q)) {
                        val isVerified = prof.optBoolean("verified", false) ||
                            prof.optBoolean("isVerified", false) ||
                            prof.optString("badge", "").equals("verified", ignoreCase = true) ||
                            prof.optString("role", "").equals("verified", ignoreCase = true) ||
                            prof.optBoolean("is_verified", false)
                        results.add(
                            NuxUserProfile(
                                uid = key,
                                username = uname,
                                photoURL = prof.optString("photoURL", prof.optString("photoUrl", "")),
                                isVerified = isVerified
                            )
                        )
                    }
                }
            }

            // Cari di node android-users
            val androidUrl = "$RTDB_BASE/android-users.json?auth=$token"
            val androidResp = client.newCall(Request.Builder().url(androidUrl).get().build()).execute()
            val androidBody = androidResp.body?.string() ?: ""
            androidResp.close()

            if (androidResp.isSuccessful && androidBody.isNotEmpty() && androidBody != "null") {
                val json = JSONObject(androidBody)
                for (key in json.keys()) {
                    if (key == myUid || results.any { it.uid == key }) continue
                    val uObj = json.optJSONObject(key) ?: continue
                    val prof = uObj.optJSONObject("profile") ?: continue
                    val uname = prof.optString("username", "")
                    if (uname.lowercase().contains(q)) {
                        val isVerified = prof.optBoolean("verified", false) ||
                            prof.optBoolean("isVerified", false) ||
                            prof.optString("badge", "").equals("verified", ignoreCase = true) ||
                            prof.optString("role", "").equals("verified", ignoreCase = true) ||
                            prof.optBoolean("is_verified", false)
                        results.add(
                            NuxUserProfile(
                                uid = key,
                                username = uname,
                                photoURL = prof.optString("photoURL", prof.optString("photoUrl", "")),
                                isVerified = isVerified
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {} finally {
            _searchResults.value = results
            _isSearching.value = false
        }
    }

    /**
     * Kirim permintaan pertemanan ke targetUid
     */
    suspend fun sendFriendRequest(targetUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        val myUid = getCurrentUser()?.uid ?: return@withContext Result.failure(Exception("Belum login"))
        val token = getAuthToken() ?: return@withContext Result.failure(Exception("Token tidak valid"))

        try {
            // Target menerima pending_received
            val targetReq = Request.Builder()
                .url("$RTDB_BASE/users/$targetUid/friends/$myUid.json?auth=$token")
                .put("\"pending_received\"".toRequestBody(JSON_MEDIA))
                .build()
            client.newCall(targetReq).execute().close()

            // Pengirim mencatat pending_sent
            val myReq = Request.Builder()
                .url("$RTDB_BASE/users/$myUid/friends/$targetUid.json?auth=$token")
                .put("\"pending_sent\"".toRequestBody(JSON_MEDIA))
                .build()
            client.newCall(myReq).execute().close()

            fetchFriends()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Terima permintaan pertemanan
     */
    suspend fun acceptFriendRequest(targetUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        val myUid = getCurrentUser()?.uid ?: return@withContext Result.failure(Exception("Belum login"))
        val token = getAuthToken() ?: return@withContext Result.failure(Exception("Token tidak valid"))

        try {
            val myReq = Request.Builder()
                .url("$RTDB_BASE/users/$myUid/friends/$targetUid.json?auth=$token")
                .put("\"accepted\"".toRequestBody(JSON_MEDIA))
                .build()
            client.newCall(myReq).execute().close()

            val targetReq = Request.Builder()
                .url("$RTDB_BASE/users/$targetUid/friends/$myUid.json?auth=$token")
                .put("\"accepted\"".toRequestBody(JSON_MEDIA))
                .build()
            client.newCall(targetReq).execute().close()

            fetchFriends()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Tolak atau batalkan pertemanan
     */
    suspend fun removeFriend(targetUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        val myUid = getCurrentUser()?.uid ?: return@withContext Result.failure(Exception("Belum login"))
        val token = getAuthToken() ?: return@withContext Result.failure(Exception("Token tidak valid"))

        try {
            val myReq = Request.Builder()
                .url("$RTDB_BASE/users/$myUid/friends/$targetUid.json?auth=$token")
                .delete()
                .build()
            client.newCall(myReq).execute().close()

            val targetReq = Request.Builder()
                .url("$RTDB_BASE/users/$targetUid/friends/$myUid.json?auth=$token")
                .delete()
                .build()
            client.newCall(targetReq).execute().close()

            fetchFriends()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Buka ruang obrolan direct message dengan teman tertentu
     */
    fun openChat(friend: NuxFriend) {
        _activeChatFriend.value = friend
        val myUid = getCurrentUser()?.uid ?: return
        val friendUid = friend.uid
        val chatId = if (myUid < friendUid) "${myUid}_${friendUid}" else "${friendUid}_${myUid}"

        chatJob?.cancel()
        chatJob = scope.launch {
            // Reset unread count di server
            val token = getAuthToken()
            if (token != null) {
                try {
                    val resetReq = Request.Builder()
                        .url("$RTDB_BASE/users/$myUid/chatMeta/$friendUid/unreadCount.json?auth=$token")
                        .put("0".toRequestBody(JSON_MEDIA))
                        .build()
                    client.newCall(resetReq).execute().close()
                } catch (_: Exception) {}
            }

            while (isActive) {
                fetchChatMessages(chatId)
                fetchFriendTypingStatus(chatId, friendUid)
                delay(1200)
            }
        }
    }

    fun closeChat() {
        chatJob?.cancel()
        chatJob = null
        _activeChatFriend.value = null
        _chatMessages.value = emptyList()
        _isFriendTyping.value = false
    }

    private suspend fun fetchChatMessages(chatId: String) = withContext(Dispatchers.IO) {
        val token = getAuthToken() ?: return@withContext
        try {
            val url = "$RTDB_BASE/chats/$chatId.json?auth=$token"
            val resp = client.newCall(Request.Builder().url(url).get().build()).execute()
            val body = resp.body?.string() ?: ""
            resp.close()

            if (!resp.isSuccessful || body.isEmpty() || body == "null") {
                _chatMessages.value = emptyList()
                return@withContext
            }

            val json = JSONObject(body)
            val msgs = mutableListOf<NuxChatMessage>()
            for (key in json.keys()) {
                val item = json.getJSONObject(key)
                val replyObj = item.optJSONObject("replyTo")
                val reply = if (replyObj != null) {
                    NuxChatReply(
                        id = replyObj.optString("id", ""),
                        senderId = replyObj.optString("senderId", ""),
                        senderName = replyObj.optString("senderName", ""),
                        text = replyObj.optString("text", "")
                    )
                } else null

                msgs.add(
                    NuxChatMessage(
                        id = key,
                        senderId = item.optString("senderId", ""),
                        text = item.optString("text", ""),
                        timestamp = item.optLong("timestamp", 0L),
                        imageUrl = if (item.has("imageUrl")) item.optString("imageUrl", null) else null,
                        replyTo = reply
                    )
                )
            }

            msgs.sortBy { it.timestamp }
            _chatMessages.value = msgs
        } catch (_: Exception) {}
    }

    private suspend fun fetchFriendTypingStatus(chatId: String, friendUid: String) = withContext(Dispatchers.IO) {
        val token = getAuthToken() ?: return@withContext
        try {
            val url = "$RTDB_BASE/typing/$chatId/$friendUid.json?auth=$token"
            val resp = client.newCall(Request.Builder().url(url).get().build()).execute()
            val body = resp.body?.string() ?: ""
            resp.close()
            _isFriendTyping.value = body.trim() == "true"
        } catch (_: Exception) {
            _isFriendTyping.value = false
        }
    }

    /**
     * Update status mengetik user di typing/{chatId}/{myUid}
     */
    fun setMyTyping(isTyping: Boolean) {
        val myUid = getCurrentUser()?.uid ?: return
        val friend = _activeChatFriend.value ?: return
        val chatId = if (myUid < friend.uid) "${myUid}_${friend.uid}" else "${friend.uid}_${myUid}"

        typingJob?.cancel()
        typingJob = scope.launch {
            val token = getAuthToken() ?: return@launch
            try {
                val url = "$RTDB_BASE/typing/$chatId/$myUid.json?auth=$token"
                val req = if (isTyping) {
                    Request.Builder().url(url).put("true".toRequestBody(JSON_MEDIA)).build()
                } else {
                    Request.Builder().url(url).delete().build()
                }
                client.newCall(req).execute().close()

                if (isTyping) {
                    delay(2500)
                    // Auto reset typing setelah 2.5 detik jika tidak mengetik lagi
                    val resetReq = Request.Builder().url(url).delete().build()
                    client.newCall(resetReq).execute().close()
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Kirim pesan direct chat (teks, kutipan balasan, dan/atau gambar)
     */
    suspend fun sendChatMessage(
        text: String,
        imageBytes: ByteArray? = null,
        mimeType: String = "image/jpeg",
        replyTo: NuxChatReply? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val myUid = getCurrentUser()?.uid ?: return@withContext Result.failure(Exception("Belum login"))
        val friend = _activeChatFriend.value ?: return@withContext Result.failure(Exception("Ruang obrolan belum terbuka"))
        val friendUid = friend.uid
        val token = getAuthToken() ?: return@withContext Result.failure(Exception("Token tidak valid"))
        val chatId = if (myUid < friendUid) "${myUid}_${friendUid}" else "${friendUid}_${myUid}"

        var uploadedUrl: String? = null
        if (imageBytes != null && imageBytes.isNotEmpty()) {
            val uploadRes = NuxAuthManager.uploadProfileImage(imageBytes, mimeType)
            if (uploadRes.isSuccess) {
                uploadedUrl = uploadRes.getOrNull()
            } else {
                return@withContext Result.failure(uploadRes.exceptionOrNull() ?: Exception("Gagal mengunggah gambar"))
            }
        }

        val now = System.currentTimeMillis()
        val payload = JSONObject().apply {
            put("senderId", myUid)
            put("text", text.trim())
            put("timestamp", now)
            if (!uploadedUrl.isNullOrEmpty()) {
                put("imageUrl", uploadedUrl)
            }
            if (replyTo != null) {
                put("replyTo", JSONObject().apply {
                    put("id", replyTo.id)
                    put("senderId", replyTo.senderId)
                    put("senderName", replyTo.senderName)
                    put("text", replyTo.text.take(120))
                })
            }
        }

        try {
            // POST ke chats/{chatId}.json
            val postReq = Request.Builder()
                .url("$RTDB_BASE/chats/$chatId.json?auth=$token")
                .post(payload.toString().toRequestBody(JSON_MEDIA))
                .build()
            client.newCall(postReq).execute().close()

            // Update chatMeta untuk kedua pihak
            val myMetaReq = Request.Builder()
                .url("$RTDB_BASE/users/$myUid/chatMeta/$friendUid/lastMessageTime.json?auth=$token")
                .put(now.toString().toRequestBody(JSON_MEDIA))
                .build()
            client.newCall(myMetaReq).execute().close()

            val friendMetaReq = Request.Builder()
                .url("$RTDB_BASE/users/$friendUid/chatMeta/$myUid.json?auth=$token")
                .patch(JSONObject().apply {
                    put("lastMessageTime", now)
                }.toString().toRequestBody(JSON_MEDIA))
                .build()
            client.newCall(friendMetaReq).execute().close()

            setMyTyping(false)
            fetchChatMessages(chatId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Memuat daftar voice rooms aktif dari voice_rooms.json
     */
    suspend fun fetchVoiceRooms() = withContext(Dispatchers.IO) {
        val token = getAuthToken() ?: return@withContext
        try {
            val url = "$RTDB_BASE/voice_rooms.json?auth=$token"
            val resp = client.newCall(Request.Builder().url(url).get().build()).execute()
            val body = resp.body?.string() ?: ""
            resp.close()
            android.util.Log.d("NuxSocial", "fetchVoiceRooms resp=${resp.code} body=$body")

            if (!resp.isSuccessful || body.isEmpty() || body == "null") {
                _voiceRooms.value = emptyList()
                return@withContext
            }

            val json = JSONObject(body)
            val rooms = mutableListOf<NuxVoiceRoom>()
            val currentFriends = _friends.value
            val currentUser = getCurrentUser()
            for (roomId in json.keys()) {
                val rObj = json.getJSONObject(roomId)
                val participantsJson = rObj.optJSONObject("participants")
                val participants = mutableMapOf<String, NuxParticipant>()
                if (participantsJson != null) {
                    for (pUid in participantsJson.keys()) {
                        val pObj = participantsJson.optJSONObject(pUid)
                        if (pObj != null) {
                            val pLastSeen = pObj.optLong("lastSeen", 0L)
                            val isStaleAndroid = pObj.optString("platform") == "android" && pLastSeen > 0L && (System.currentTimeMillis() - pLastSeen > 30000L)
                            if (isStaleAndroid) {
                                try {
                                    val staleDelUrl = "$RTDB_BASE/voice_rooms/$roomId/participants/$pUid.json?auth=$token"
                                    client.newCall(Request.Builder().url(staleDelUrl).delete().build()).execute().close()
                                } catch (_: Exception) {}
                                continue
                            }

                            var isVerified = pObj.optBoolean("verified", false) || pObj.optBoolean("isVerified", false)
                            var photoURL = pObj.optString("photoURL", pObj.optString("photoUrl", ""))
                            var username = pObj.optString("username", "Peserta")

                            val curUser = currentUser
                            if (curUser != null && curUser.uid == pUid) {
                                if (photoURL.isEmpty()) photoURL = curUser.photoUrl
                                if (!isVerified) isVerified = curUser.isVerified
                                if (username == "Peserta" || username.isEmpty()) username = curUser.username
                            } else {
                                val friend = currentFriends.find { it.uid == pUid }
                                if (friend != null) {
                                    if (photoURL.isEmpty()) photoURL = friend.photoUrl
                                    if (!isVerified) isVerified = friend.isVerified
                                    if (username == "Peserta" || username.isEmpty()) username = friend.username
                                } else if (photoURL.isEmpty() || !isVerified) {
                                    try {
                                        val fetchedProfile = fetchUserProfile(pUid, token)
                                        if (fetchedProfile != null) {
                                            if (photoURL.isEmpty()) photoURL = fetchedProfile.photoURL
                                            if (!isVerified) isVerified = fetchedProfile.isVerified
                                            if (username == "Peserta" || username.isEmpty()) username = fetchedProfile.username
                                        }
                                    } catch (_: Exception) {}
                                }
                            }

                            participants[pUid] = NuxParticipant(
                                uid = pUid,
                                username = username,
                                photoURL = photoURL,
                                isVerified = isVerified
                            )
                        }
                    }
                }

                rooms.add(
                    NuxVoiceRoom(
                        id = roomId,
                        name = rObj.optString("name", "Voice Room"),
                        hostUid = rObj.optString("hostUid", ""),
                        password = rObj.optString("password", ""),
                        maxUsers = rObj.optInt("maxUsers", 5),
                        participants = participants
                    )
                )
            }

            _voiceRooms.value = rooms.filter { it.participantCount > 0 }

            // Jika ada room aktif, perbarui status peserta di active room
            val curActive = _activeVoiceRoom.value
            if (curActive != null) {
                val updated = rooms.find { it.id == curActive.id }
                if (updated != null) {
                    _activeVoiceRoom.value = updated
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Buat room baru di voice_rooms/{roomId}
     */
    suspend fun createVoiceRoom(name: String, password: String, maxUsers: Int): Result<NuxVoiceRoom> = withContext(Dispatchers.IO) {
        val user = getCurrentUser() ?: run {
            android.util.Log.e("NuxSocial", "createVoiceRoom: user is null!")
            return@withContext Result.failure(Exception("Belum login"))
        }
        val token = getAuthToken() ?: run {
            android.util.Log.e("NuxSocial", "createVoiceRoom: token is null!")
            return@withContext Result.failure(Exception("Token tidak valid"))
        }

        android.util.Log.d("NuxSocial", "createVoiceRoom: user=${user.uid}, token=${token.take(15)}")

        val payload = JSONObject().apply {
            put("name", name.trim())
            put("hostUid", user.uid)
            put("password", password.trim())
            put("maxUsers", maxUsers.coerceIn(2, 20))
            put("participants", JSONObject().apply {
                put(user.uid, JSONObject().apply {
                    put("uid", user.uid)
                    put("username", user.username)
                    put("photoURL", user.photoUrl)
                    put("photoUrl", user.photoUrl)
                    put("verified", user.isVerified)
                    put("isVerified", user.isVerified)
                    put("platform", "android")
                    put("isAndroid", true)
                    put("lastSeen", System.currentTimeMillis())
                })
            })
        }

        try {
            val req = Request.Builder()
                .url("$RTDB_BASE/voice_rooms.json?auth=$token")
                .post(payload.toString().toRequestBody(JSON_MEDIA))
                .build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string() ?: ""
            resp.close()

            android.util.Log.d("NuxSocial", "createVoiceRoom resp=${resp.code} body=$body")

            if (resp.isSuccessful && body.isNotEmpty()) {
                val nameKey = JSONObject(body).optString("name", "")
                val newRoom = NuxVoiceRoom(
                    id = nameKey,
                    name = name.trim(),
                    hostUid = user.uid,
                    password = password.trim(),
                    maxUsers = maxUsers,
                    participants = mapOf(
                        user.uid to NuxParticipant(
                            uid = user.uid,
                            username = user.username,
                            photoURL = user.photoUrl,
                            isVerified = user.isVerified
                        )
                    )
                )
                joinVoiceRoomInternal(newRoom)
                fetchVoiceRooms()
                Result.success(newRoom)
            } else {
                Result.failure(Exception("Gagal ($resp.code): $body"))
            }
        } catch (e: Exception) {
            android.util.Log.e("NuxSocial", "createVoiceRoom error", e)
            Result.failure(e)
        }
    }

    /**
     * Masuk ke voice room yang ada
     */
    suspend fun joinVoiceRoom(room: NuxVoiceRoom, inputPassword: String = ""): Result<Unit> = withContext(Dispatchers.IO) {
        val user = getCurrentUser() ?: return@withContext Result.failure(Exception("Belum login"))
        val token = getAuthToken() ?: return@withContext Result.failure(Exception("Token tidak valid"))

        if (room.isLocked && room.password != inputPassword) {
            return@withContext Result.failure(Exception("Password voice room salah!"))
        }

        if (room.isFull && !room.participants.containsKey(user.uid)) {
            return@withContext Result.failure(Exception("Voice room sudah penuh!"))
        }

        try {
            val partPayload = JSONObject().apply {
                put("uid", user.uid)
                put("username", user.username)
                put("photoURL", user.photoUrl)
                put("photoUrl", user.photoUrl)
                put("verified", user.isVerified)
                put("isVerified", user.isVerified)
                put("platform", "android")
                put("isAndroid", true)
                put("lastSeen", System.currentTimeMillis())
            }

            val req = Request.Builder()
                .url("$RTDB_BASE/voice_rooms/${room.id}/participants/${user.uid}.json?auth=$token")
                .put(partPayload.toString().toRequestBody(JSON_MEDIA))
                .build()
            client.newCall(req).execute().close()

            joinVoiceRoomInternal(room)
            fetchVoiceRooms()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun joinVoiceRoomInternal(room: NuxVoiceRoom) {
        _activeVoiceRoom.value = room
        try {
            val ctx = com.movtery.zalithlauncher.context.GlobalContext
            ctx.startService(Intent(ctx, NuxVoiceSessionService::class.java))
        } catch (_: Exception) {}

        voiceRoomSyncJob?.cancel()
        voiceRoomSyncJob = scope.launch {
            while (isActive) {
                fetchVoiceRoomMessages(room.id)
                fetchVoiceRooms()

                val curU = getCurrentUser()
                val tok = getAuthToken()
                if (curU != null && tok != null) {
                    try {
                        val hbUrl = "$RTDB_BASE/voice_rooms/${room.id}/participants/${curU.uid}.json?auth=$tok"
                        val hbBody = JSONObject().apply {
                            put("lastSeen", System.currentTimeMillis())
                        }.toString().toRequestBody(JSON_MEDIA)
                        client.newCall(Request.Builder().url(hbUrl).patch(hbBody).build()).execute().close()
                    } catch (_: Exception) {}
                }

                delay(1500)
            }
        }
        val user = getCurrentUser()
        if (user != null) {
            android.util.Log.i("NuxSocial", "Delegating joinVoiceRoom to NuxVoiceManager for room ${room.id} user=${user.username}")
            NuxVoiceManager.joinVoiceRoom(room.id, user)
        } else {
            android.util.Log.e("NuxSocial", "Cannot join NuxVoiceManager: user is null")
        }
    }

    /**
     * Keluar dari voice room aktif secara blocking/synchronous (digunakan saat app ditutup atau crash)
     */
    fun leaveVoiceRoomBlocking() {
        try {
            NuxVoiceManager.leaveVoiceRoom()
        } catch (_: Exception) {}

        try {
            val ctx = com.movtery.zalithlauncher.context.GlobalContext
            ctx.stopService(Intent(ctx, NuxVoiceSessionService::class.java))
        } catch (_: Exception) {}

        val user = getCurrentUser()
        val room = _activeVoiceRoom.value
        val token = runBlocking { getAuthToken() }

        voiceRoomSyncJob?.cancel()
        voiceRoomSyncJob = null
        _activeVoiceRoom.value = null
        _voiceMessages.value = emptyList()

        if (user != null && room != null) {
            val t = Thread {
                try {
                    val token = runBlocking { getAuthToken() } ?: return@Thread
                    val req = Request.Builder()
                        .url("$RTDB_BASE/voice_rooms/${room.id}/participants/${user.uid}.json?auth=$token")
                        .delete()
                        .build()
                    client.newCall(req).execute().close()

                    // Cek sisa peserta, jika kosong hapus room
                    val checkUrl = "$RTDB_BASE/voice_rooms/${room.id}/participants.json?auth=$token"
                    val checkResp = client.newCall(Request.Builder().url(checkUrl).get().build()).execute()
                    val checkBody = checkResp.body?.string() ?: ""
                    checkResp.close()

                    if (checkBody.isEmpty() || checkBody == "null" || checkBody == "{}") {
                        val delReq = Request.Builder()
                            .url("$RTDB_BASE/voice_rooms/${room.id}.json?auth=$token")
                            .delete()
                            .build()
                        client.newCall(delReq).execute().close()
                    }
                } catch (_: Exception) {}
            }
            t.start()
            try {
                t.join(2500)
            } catch (_: Exception) {}
        }
    }

    /**
     * Keluar dari voice room aktif
     */
    suspend fun leaveVoiceRoom() = withContext(Dispatchers.IO) {
        leaveVoiceRoomBlocking()
        fetchVoiceRooms()
    }

    private suspend fun fetchVoiceRoomMessages(roomId: String) = withContext(Dispatchers.IO) {
        val token = getAuthToken() ?: return@withContext
        try {
            val url = "$RTDB_BASE/voice_rooms/$roomId/messages.json?auth=$token"
            val resp = client.newCall(Request.Builder().url(url).get().build()).execute()
            val body = resp.body?.string() ?: ""
            resp.close()

            if (!resp.isSuccessful || body.isEmpty() || body == "null") {
                _voiceMessages.value = emptyList()
                return@withContext
            }

            val json = JSONObject(body)
            val msgs = mutableListOf<NuxVoiceMessage>()
            for (key in json.keys()) {
                val item = json.getJSONObject(key)
                val isVerified = item.optBoolean("verified", false) || item.optBoolean("isVerified", false)
                msgs.add(
                    NuxVoiceMessage(
                        id = key,
                        senderId = item.optString("senderId", ""),
                        senderName = item.optString("senderName", "Peserta"),
                        senderPhotoURL = item.optString("senderPhotoURL", ""),
                        text = item.optString("text", ""),
                        timestamp = item.optLong("timestamp", 0L),
                        isVerified = isVerified
                    )
                )
            }
            msgs.sortBy { it.timestamp }
            _voiceMessages.value = msgs
        } catch (_: Exception) {}
    }

    /**
     * Kirim pesan teks di dalam voice room
     */
    suspend fun sendVoiceRoomMessage(text: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = getCurrentUser() ?: return@withContext Result.failure(Exception("Belum login"))
        val room = _activeVoiceRoom.value ?: return@withContext Result.failure(Exception("Tidak dalam voice room"))
        val token = getAuthToken() ?: return@withContext Result.failure(Exception("Token tidak valid"))

        val payload = JSONObject().apply {
            put("senderId", user.uid)
            put("senderName", user.username)
            put("senderPhotoURL", user.photoUrl)
            put("text", text.trim())
            put("timestamp", System.currentTimeMillis())
            put("verified", user.isVerified)
        }

        try {
            val req = Request.Builder()
                .url("$RTDB_BASE/voice_rooms/${room.id}/messages.json?auth=$token")
                .post(payload.toString().toRequestBody(JSON_MEDIA))
                .build()
            client.newCall(req).execute().close()
            fetchVoiceRoomMessages(room.id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
