package com.movtery.zalithlauncher.auth

import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object NuxAuthManager {
    private const val API_KEY = "AIzaSyBm-KYO3cp7jmZBrSDrZp5LwvDTX9cSu_M"
    private const val RTDB_BASE = "https://nux-launcher-default-rtdb.asia-southeast1.firebasedatabase.app"

    private const val AUTH_SIGN_IN_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$API_KEY"
    private const val AUTH_SIGN_UP_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$API_KEY"
    private const val AUTH_UPDATE_URL = "https://identitytoolkit.googleapis.com/v1/accounts:update?key=$API_KEY"
    private const val AUTH_REFRESH_URL = "https://securetoken.googleapis.com/v1/token?key=$API_KEY"

    private const val UPLOAD_IMAGE_PRIMARY_URL = "https://server.nuxlauncher.site/upload/image"
    private const val UPLOAD_IMAGE_FALLBACK_URL = "https://api.imgbb.com/1/upload?key=c3257ef84dcc0d3a9e26f50c15579f06"

    // MMKV Keys
    private const val MMKV_ID = "NUX_AUTH_STORE"
    private const val KEY_ID_TOKEN = "id_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_TOKEN_EXPIRY = "token_expiry"
    private const val KEY_UID = "user_uid"
    private const val KEY_EMAIL = "user_email"
    private const val KEY_USERNAME = "user_username"
    private const val KEY_PHOTO_URL = "user_photo_url"
    private const val KEY_IS_ACTIVATED = "is_activated"
    private const val KEY_ACTIVATED_AT = "activated_at"
    private const val KEY_LICENSE_KEY = "license_key"
    private const val KEY_IS_VERIFIED = "is_verified"
    private const val KEY_IS_PREMIUM = "is_premium"

    private val mmkv: MMKV by lazy {
        MMKV.mmkvWithID(MMKV_ID, MMKV.MULTI_PROCESS_MODE)
    }

    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val _authState = MutableStateFlow<AuthGateState>(AuthGateState.Checking)
    val authState: StateFlow<AuthGateState> = _authState.asStateFlow()

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    init {
        scope.launch {
            checkSession()
        }
    }

    /**
     * Memeriksa sesi tersimpan saat aplikasi dibuka
     */
    suspend fun checkSession() = withContext(Dispatchers.IO) {
        val uid = mmkv.getString(KEY_UID, null)
        val email = mmkv.getString(KEY_EMAIL, null)

        if (uid.isNullOrEmpty() || email.isNullOrEmpty()) {
            _authState.value = AuthGateState.Unauthenticated
            return@withContext
        }

        val username = mmkv.getString(KEY_USERNAME, "") ?: ""
        val photoUrl = mmkv.getString(KEY_PHOTO_URL, "") ?: ""
        val cachedActivated = mmkv.getBoolean(KEY_IS_ACTIVATED, false)
        val activatedAt = if (mmkv.containsKey(KEY_ACTIVATED_AT)) mmkv.getLong(KEY_ACTIVATED_AT, 0L) else null
        val licenseKey = mmkv.getString(KEY_LICENSE_KEY, null)
        val cachedVerified = mmkv.getBoolean(KEY_IS_VERIFIED, false)
        val cachedPremium = mmkv.getBoolean(KEY_IS_PREMIUM, false)

        val currentUser = NuxUser(
            uid = uid,
            email = email,
            username = username,
            photoUrl = photoUrl,
            isActivated = cachedActivated,
            activatedAt = activatedAt,
            key = licenseKey,
            isVerified = cachedVerified,
            isPremium = cachedPremium
        )

        // Coba validasi token dan sinkronisasi status lisensi terkini dari server
        val validToken = getValidIdToken()
        if (validToken == null) {
            // Jika token tidak bisa diperbarui dan belum pernah aktivasi, arahkan login
            if (!cachedActivated) {
                _authState.value = AuthGateState.Unauthenticated
            } else {
                // Grace period: Jika sudah teraktivasi sebelumnya namun offline/token expired, tetap izinkan masuk
                _authState.value = AuthGateState.Authenticated(currentUser)
            }
            return@withContext
        }

        try {
            // Ambil data user dari Realtime DB node: android-users/{uid}
            val userUrl = "$RTDB_BASE/android-users/$uid.json?auth=$validToken"
            val req = Request.Builder().url(userUrl).get().build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string()
                if (resp.isSuccessful && !body.isNullOrEmpty() && body != "null") {
                    val userData = gson.fromJson(body, NuxUserData::class.java)
                    val serverUsername = userData?.profile?.username ?: username
                    val serverPhotoUrl = userData?.profile?.photoURL ?: photoUrl
                    val serverVerified = userData?.profile?.effectiveVerified ?: cachedVerified
                    val serverPremium = userData?.profile?.effectivePremium ?: cachedPremium
                    val sub = userData?.subscription
                    val isSubActive = sub != null && (sub.tier == "lifetime" || sub.activatedAt != null)

                    val updatedUser = currentUser.copy(
                        username = serverUsername,
                        photoUrl = serverPhotoUrl,
                        isActivated = isSubActive,
                        activatedAt = sub?.activatedAt ?: activatedAt,
                        key = sub?.key ?: licenseKey,
                        isVerified = serverVerified,
                        isPremium = serverPremium
                    )

                    // Simpan status terbaru ke MMKV
                    mmkv.putString(KEY_USERNAME, serverUsername)
                    mmkv.putString(KEY_PHOTO_URL, serverPhotoUrl)
                    mmkv.putBoolean(KEY_IS_ACTIVATED, isSubActive)
                    mmkv.putBoolean(KEY_IS_VERIFIED, serverVerified)
                    mmkv.putBoolean(KEY_IS_PREMIUM, serverPremium)
                    if (sub?.activatedAt != null) {
                        mmkv.putLong(KEY_ACTIVATED_AT, sub.activatedAt)
                    }
                    if (sub?.key != null) {
                        mmkv.putString(KEY_LICENSE_KEY, sub.key)
                    }

                    if (isSubActive) {
                        _authState.value = AuthGateState.Authenticated(updatedUser)
                    } else {
                        _authState.value = AuthGateState.NeedsActivation(updatedUser)
                    }
                    syncProfileToPublic(validToken)
                } else if (resp.code == 401) {
                    // Firebase DB permission denied - jika sudah aktivasi di cache, tetap berikan akses
                    if (cachedActivated) {
                        _authState.value = AuthGateState.Authenticated(currentUser)
                    } else {
                        _authState.value = AuthGateState.NeedsActivation(currentUser)
                    }
                } else {
                    if (cachedActivated) {
                        _authState.value = AuthGateState.Authenticated(currentUser)
                    } else {
                        _authState.value = AuthGateState.NeedsActivation(currentUser)
                    }
                }
            }
        } catch (e: Exception) {
            // Error koneksi: gunakan cache
            if (cachedActivated) {
                _authState.value = AuthGateState.Authenticated(currentUser)
            } else {
                _authState.value = AuthGateState.NeedsActivation(currentUser)
            }
        }
    }

    /**
     * Login menggunakan Email & Password
     */
    suspend fun login(email: String, pass: String): Result<NuxUser> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        val trimmedPass = pass.trim()

        if (trimmedEmail.isEmpty() || trimmedPass.isEmpty()) {
            return@withContext Result.failure(Exception("Email dan password tidak boleh kosong!"))
        }

        try {
            val payload = JSONObject().apply {
                put("email", trimmedEmail)
                put("password", trimmedPass)
                put("returnSecureToken", true)
            }.toString()

            val req = Request.Builder()
                .url(AUTH_SIGN_IN_URL)
                .post(payload.toRequestBody(JSON_MEDIA))
                .build()

            val response = client.newCall(req).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseFirebaseError(body)
                return@withContext Result.failure(Exception(errorMsg))
            }

            val authResp = gson.fromJson(body, FirebaseAuthResponse::class.java)
            val idToken = authResp.idToken ?: return@withContext Result.failure(Exception("Gagal memperoleh ID token"))
            val refreshToken = authResp.refreshToken ?: ""
            val expiresIn = (authResp.expiresIn?.toLongOrNull() ?: 3600L) * 1000L
            val uid = authResp.localId ?: return@withContext Result.failure(Exception("Gagal memperoleh UID akun"))
            val userEmail = authResp.email ?: trimmedEmail

            // 1. Verifikasi apakah akun ini merupakan akun versi Windows (terdaftar di users/{uid} dan bukan di android-users/{uid})
            try {
                val androidCheckUrl = "$RTDB_BASE/android-users/$uid.json?auth=$idToken"
                val androidCheckReq = Request.Builder().url(androidCheckUrl).get().build()
                val isAndroidUser = client.newCall(androidCheckReq).execute().use { aResp ->
                    val aBody = aResp.body?.string()?.trim()
                    aResp.isSuccessful && !aBody.isNullOrEmpty() && aBody != "null"
                }

                if (!isAndroidUser) {
                    val winUrl = "$RTDB_BASE/users/$uid.json?auth=$idToken"
                    val winReq = Request.Builder().url(winUrl).get().build()
                    val isWindowsUser = client.newCall(winReq).execute().use { winResp ->
                        val winBody = winResp.body?.string()?.trim()
                        winResp.isSuccessful && !winBody.isNullOrEmpty() && winBody != "null"
                    }
                    if (isWindowsUser) {
                        return@withContext Result.failure(
                            Exception("Akun ini terdaftar di NUX Launcher versi Windows. Akun versi Windows tidak dapat login di versi Android. Silakan buat akun baru dengan email berbeda khusus untuk Android.")
                        )
                    }
                }
            } catch (e: Exception) {
                if (e.message?.contains("versi Windows") == true) {
                    return@withContext Result.failure(e)
                }
            }

            // Simpan token awal setelah lolos verifikasi platform
            mmkv.putString(KEY_ID_TOKEN, idToken)
            mmkv.putString(KEY_REFRESH_TOKEN, refreshToken)
            mmkv.putLong(KEY_TOKEN_EXPIRY, System.currentTimeMillis() + expiresIn)
            mmkv.putString(KEY_UID, uid)
            mmkv.putString(KEY_EMAIL, userEmail)

            // Cek data user & status lisensi di Firebase Realtime DB node: android-users/{uid}
            var username = authResp.displayName ?: userEmail.substringBefore("@")
            var photoUrl = authResp.photoUrl ?: ""
            var isActivated = false
            var activatedAt: Long? = null
            var licenseKey: String? = null
            var isVerified = false
            var isPremium = false

            try {
                val userUrl = "$RTDB_BASE/android-users/$uid.json?auth=$idToken"
                val userReq = Request.Builder().url(userUrl).get().build()
                client.newCall(userReq).execute().use { userResp ->
                    val userBody = userResp.body?.string()
                    if (userResp.isSuccessful && !userBody.isNullOrEmpty() && userBody != "null") {
                        val userData = gson.fromJson(userBody, NuxUserData::class.java)
                        userData?.profile?.username?.let { if (it.isNotEmpty()) username = it }
                        userData?.profile?.photoURL?.let { if (it.isNotEmpty()) photoUrl = it }
                        isVerified = userData?.profile?.effectiveVerified == true
                        isPremium = userData?.profile?.effectivePremium == true
                        val sub = userData?.subscription
                        if (sub != null && (sub.tier == "lifetime" || sub.activatedAt != null)) {
                            isActivated = true
                            activatedAt = sub.activatedAt
                            licenseKey = sub.key
                        }
                    }
                }
            } catch (_: Exception) {
                // Ignore RTDB check failure on login, fallback to not activated or cached
            }

            mmkv.putString(KEY_USERNAME, username)
            mmkv.putString(KEY_PHOTO_URL, photoUrl)
            mmkv.putBoolean(KEY_IS_ACTIVATED, isActivated)
            mmkv.putBoolean(KEY_IS_VERIFIED, isVerified)
            mmkv.putBoolean(KEY_IS_PREMIUM, isPremium)
            val actAt = activatedAt
            if (actAt != null) mmkv.putLong(KEY_ACTIVATED_AT, actAt)
            if (licenseKey != null) mmkv.putString(KEY_LICENSE_KEY, licenseKey)

            val loggedInUser = NuxUser(
                uid = uid,
                email = userEmail,
                username = username,
                photoUrl = photoUrl,
                isActivated = isActivated,
                activatedAt = activatedAt,
                key = licenseKey,
                isVerified = isVerified,
                isPremium = isPremium
            )

            if (isActivated) {
                _authState.value = AuthGateState.Authenticated(loggedInUser)
            } else {
                _authState.value = AuthGateState.NeedsActivation(loggedInUser)
            }

            syncProfileToPublic(idToken)
            Result.success(loggedInUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Registrasi Akun Baru
     */
    suspend fun register(email: String, pass: String, username: String): Result<NuxUser> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        val trimmedPass = pass.trim()
        val trimmedName = username.trim()

        if (trimmedEmail.isEmpty() || trimmedPass.isEmpty() || trimmedName.isEmpty()) {
            return@withContext Result.failure(Exception("Semua kolom harus diisi!"))
        }
        if (trimmedPass.length < 6) {
            return@withContext Result.failure(Exception("Kata sandi minimal harus 6 karakter!"))
        }

        try {
            val payload = JSONObject().apply {
                put("email", trimmedEmail)
                put("password", trimmedPass)
                put("returnSecureToken", true)
            }.toString()

            val req = Request.Builder()
                .url(AUTH_SIGN_UP_URL)
                .post(payload.toRequestBody(JSON_MEDIA))
                .build()

            val response = client.newCall(req).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseFirebaseError(body)
                return@withContext Result.failure(Exception(errorMsg))
            }

            val authResp = gson.fromJson(body, FirebaseAuthResponse::class.java)
            val idToken = authResp.idToken ?: return@withContext Result.failure(Exception("Gagal memperoleh ID token"))
            val refreshToken = authResp.refreshToken ?: ""
            val expiresIn = (authResp.expiresIn?.toLongOrNull() ?: 3600L) * 1000L
            val uid = authResp.localId ?: return@withContext Result.failure(Exception("Gagal memperoleh UID akun"))
            val userEmail = authResp.email ?: trimmedEmail

            // Update displayName pada Firebase Auth
            try {
                val updatePayload = JSONObject().apply {
                    put("idToken", idToken)
                    put("displayName", trimmedName)
                    put("returnSecureToken", true)
                }.toString()
                val updateReq = Request.Builder()
                    .url(AUTH_UPDATE_URL)
                    .post(updatePayload.toRequestBody(JSON_MEDIA))
                    .build()
                client.newCall(updateReq).execute().close()
            } catch (_: Exception) {}

            // Buat record profil di Firebase Realtime DB node: android-users/{uid}
            try {
                val profilePayload = JSONObject().apply {
                    put("profile", JSONObject().apply {
                        put("username", trimmedName)
                        put("email", userEmail)
                        put("photoURL", "")
                        put("platform", "android")
                        put("isAndroid", true)
                    })
                    put("createdAt", System.currentTimeMillis())
                }.toString()

                val rtdbReq = Request.Builder()
                    .url("$RTDB_BASE/android-users/$uid.json?auth=$idToken")
                    .patch(profilePayload.toRequestBody(JSON_MEDIA))
                    .build()
                client.newCall(rtdbReq).execute().close()
            } catch (_: Exception) {}

            // Simpan ke MMKV
            mmkv.putString(KEY_ID_TOKEN, idToken)
            mmkv.putString(KEY_REFRESH_TOKEN, refreshToken)
            mmkv.putLong(KEY_TOKEN_EXPIRY, System.currentTimeMillis() + expiresIn)
            mmkv.putString(KEY_UID, uid)
            mmkv.putString(KEY_EMAIL, userEmail)
            mmkv.putString(KEY_USERNAME, trimmedName)
            mmkv.putString(KEY_PHOTO_URL, "")
            mmkv.putBoolean(KEY_IS_ACTIVATED, false)

            val newUser = NuxUser(
                uid = uid,
                email = userEmail,
                username = trimmedName,
                photoUrl = "",
                isActivated = false
            )

            _authState.value = AuthGateState.NeedsActivation(newUser)
            syncProfileToPublic(idToken)
            Result.success(newUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Aktivasi Permanent (Lifetime) License Key di node android-key/{key}
     */
    suspend fun activateKey(rawKey: String): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanKey = rawKey.trim().uppercase()
        if (cleanKey.isEmpty()) {
            return@withContext Result.failure(Exception("Key lisensi tidak boleh kosong!"))
        }

        val uid = mmkv.getString(KEY_UID, null)
            ?: return@withContext Result.failure(Exception("Sesi login tidak ditemukan. Silakan login kembali."))

        val idToken = getValidIdToken()
            ?: return@withContext Result.failure(Exception("Token sesi kedaluwarsa. Silakan login kembali."))

        try {
            // 1. Ambil data key dari database: android-key/{cleanKey}.json
            var targetKey = cleanKey
            var keyUrl = "$RTDB_BASE/android-key/$targetKey.json?auth=$idToken"
            var keyReq = Request.Builder().url(keyUrl).get().build()
            var keyResp = client.newCall(keyReq).execute()
            var keyBody = keyResp.body?.string()

            if (!keyResp.isSuccessful && keyResp.code == 401) {
                return@withContext Result.failure(Exception("Akses database ditolak (401). Pastikan Security Rules di Firebase Console telah mengizinkan node 'android-key' dan 'android-users'."))
            }

            // Jika tidak ditemukan pada percobaan pertama, coba format alternatif (dengan dash 4-4-4-4 atau tanpa dash)
            if ((keyBody.isNullOrEmpty() || keyBody == "null")) {
                val altKey = if (cleanKey.contains("-")) {
                    cleanKey.replace("-", "")
                } else if (cleanKey.length == 16) {
                    cleanKey.chunked(4).joinToString("-")
                } else null

                if (altKey != null) {
                    val altUrl = "$RTDB_BASE/android-key/$altKey.json?auth=$idToken"
                    val altReq = Request.Builder().url(altUrl).get().build()
                    val altResp = client.newCall(altReq).execute()
                    val altBody = altResp.body?.string()
                    if (altResp.isSuccessful && !altBody.isNullOrEmpty() && altBody != "null") {
                        keyResp = altResp
                        keyBody = altBody
                        targetKey = altKey
                        keyUrl = altUrl
                    }
                }
            }

            if (!keyResp.isSuccessful) {
                return@withContext Result.failure(Exception("Gagal memeriksa key (Kode ${keyResp.code}). Silakan coba lagi."))
            }

            if (keyBody.isNullOrEmpty() || keyBody == "null") {
                return@withContext Result.failure(Exception("Key lisensi tidak valid! Pastikan format key dimasukkan dengan benar."))
            }

            val keyData = gson.fromJson(keyBody, NuxKeyData::class.java)
            if (keyData?.used == true) {
                return@withContext Result.failure(Exception("Key lisensi ini sudah pernah digunakan oleh akun lain!"))
            }

            val now = System.currentTimeMillis()

            // 2. Tandai key sebagai used pada node android-key/{key}
            val keyPatchPayload = JSONObject().apply {
                put("used", true)
                put("usedBy", uid)
                put("usedAt", now)
            }.toString()

            val patchKeyReq = Request.Builder()
                .url(keyUrl)
                .patch(keyPatchPayload.toRequestBody(JSON_MEDIA))
                .build()

            val patchKeyResp = client.newCall(patchKeyReq).execute()
            if (!patchKeyResp.isSuccessful) {
                return@withContext Result.failure(Exception("Gagal mengupdate key lisensi. Silakan coba kembali."))
            }
            patchKeyResp.close()

            // 3. Update subscription user di node android-users/{uid}
            val userSubPayload = JSONObject().apply {
                put("subscription", JSONObject().apply {
                    put("tier", "lifetime")
                    put("activatedAt", now)
                    put("key", targetKey)
                })
            }.toString()

            val patchUserReq = Request.Builder()
                .url("$RTDB_BASE/android-users/$uid.json?auth=$idToken")
                .patch(userSubPayload.toRequestBody(JSON_MEDIA))
                .build()

            val patchUserResp = client.newCall(patchUserReq).execute()
            patchUserResp.close()

            // 4. Update status lokal MMKV
            mmkv.putBoolean(KEY_IS_ACTIVATED, true)
            mmkv.putLong(KEY_ACTIVATED_AT, now)
            mmkv.putString(KEY_LICENSE_KEY, targetKey)

            val updatedUser = NuxUser(
                uid = uid,
                email = mmkv.getString(KEY_EMAIL, "") ?: "",
                username = mmkv.getString(KEY_USERNAME, "") ?: "",
                photoUrl = mmkv.getString(KEY_PHOTO_URL, "") ?: "",
                isActivated = true,
                activatedAt = now,
                key = targetKey
            )

            _authState.value = AuthGateState.Authenticated(updatedUser)
            syncProfileToPublic(idToken)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Logout akun NUX Launcher
     */
    fun logout() {
        try {
            com.movtery.zalithlauncher.social.NuxSocialManager.stopPresenceHeartbeat(setOffline = true)
            com.movtery.zalithlauncher.social.NuxSocialManager.updateMyPresenceBlocking("offline")
        } catch (_: Exception) {}
        mmkv.remove(KEY_ID_TOKEN)
        mmkv.remove(KEY_REFRESH_TOKEN)
        mmkv.remove(KEY_TOKEN_EXPIRY)
        mmkv.remove(KEY_UID)
        mmkv.remove(KEY_EMAIL)
        mmkv.remove(KEY_USERNAME)
        mmkv.remove(KEY_PHOTO_URL)
        mmkv.remove(KEY_IS_ACTIVATED)
        mmkv.remove(KEY_ACTIVATED_AT)
        mmkv.remove(KEY_LICENSE_KEY)
        mmkv.remove(KEY_IS_VERIFIED)
        mmkv.remove(KEY_IS_PREMIUM)
        _authState.value = AuthGateState.Unauthenticated
    }

    /**
     * Upload foto profil pengguna ke server NUX (dengan fallback ke ImgBB)
     */
    suspend fun uploadProfileImage(
        imageBytes: ByteArray,
        mimeType: String = "image/jpeg"
    ): Result<String> = withContext(Dispatchers.IO) {
        if (imageBytes.isEmpty()) {
            return@withContext Result.failure(Exception("Data gambar kosong!"))
        }

        val targetMediaType = (mimeType.ifEmpty { "image/jpeg" }).toMediaTypeOrNull() ?: "image/jpeg".toMediaType()
        val fileName = "avatar_${System.currentTimeMillis()}.jpg"

        // 1. Coba upload langsung ke server NUX Launcher (server.nuxlauncher.site)
        try {
            val primaryBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "image",
                    fileName,
                    imageBytes.toRequestBody(targetMediaType)
                )
                .build()

            val primaryReq = Request.Builder()
                .url(UPLOAD_IMAGE_PRIMARY_URL)
                .post(primaryBody)
                .build()

            client.newCall(primaryReq).execute().use { resp ->
                val body = resp.body?.string() ?: ""
                if (resp.isSuccessful && body.isNotEmpty()) {
                    val json = JSONObject(body)
                    if (json.optBoolean("success", false) && json.has("url")) {
                        val uploadedUrl = json.getString("url")
                        if (uploadedUrl.isNotEmpty()) {
                            return@withContext Result.success(uploadedUrl)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Server NUX offline atau gagal, lanjutkan ke fallback ImgBB
        }

        // 2. Fallback ke ImgBB jika server utama belum dapat dihubungi
        try {
            val fallbackBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "image",
                    fileName,
                    imageBytes.toRequestBody(targetMediaType)
                )
                .build()

            val fallbackReq = Request.Builder()
                .url(UPLOAD_IMAGE_FALLBACK_URL)
                .post(fallbackBody)
                .build()

            client.newCall(fallbackReq).execute().use { resp ->
                val body = resp.body?.string() ?: ""
                if (resp.isSuccessful && body.isNotEmpty()) {
                    val json = JSONObject(body)
                    if (json.optBoolean("success", false) && json.has("data")) {
                        val dataObj = json.getJSONObject("data")
                        val uploadedUrl = dataObj.optString("url", dataObj.optString("display_url", ""))
                        if (uploadedUrl.isNotEmpty()) {
                            return@withContext Result.success(uploadedUrl)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            return@withContext Result.failure(Exception("Gagal mengunggah foto profil: ${e.message}"))
        }

        Result.failure(Exception("Gagal mengunggah gambar. Silakan periksa koneksi internet Anda."))
    }

    /**
     * Memperbarui username dan/atau foto profil user pada Firebase RTDB, Firebase Auth, dan cache lokal MMKV
     */
    suspend fun updateProfile(
        newUsername: String,
        newPhotoUrl: String? = null
    ): Result<NuxUser> = withContext(Dispatchers.IO) {
        val trimmedUsername = newUsername.trim()
        if (trimmedUsername.isEmpty()) {
            return@withContext Result.failure(Exception("Username tidak boleh kosong!"))
        }

        val uid = mmkv.getString(KEY_UID, null)
            ?: return@withContext Result.failure(Exception("Sesi login tidak ditemukan. Silakan login kembali."))

        val idToken = getValidIdToken()
            ?: return@withContext Result.failure(Exception("Token sesi kedaluwarsa. Silakan login kembali."))

        val currentPhotoUrl = mmkv.getString(KEY_PHOTO_URL, "") ?: ""
        val targetPhotoUrl = (newPhotoUrl ?: currentPhotoUrl).trim()

        try {
            // 1. Update displayName dan photoUrl di Firebase Auth REST API
            try {
                val authPayload = JSONObject().apply {
                    put("idToken", idToken)
                    put("displayName", trimmedUsername)
                    if (targetPhotoUrl.isNotEmpty()) {
                        put("photoUrl", targetPhotoUrl)
                    }
                    put("returnSecureToken", true)
                }.toString()

                val authReq = Request.Builder()
                    .url(AUTH_UPDATE_URL)
                    .post(authPayload.toRequestBody(JSON_MEDIA))
                    .build()

                client.newCall(authReq).execute().close()
            } catch (_: Exception) {}

            // 2. Update profil di node android-users/{uid}/profile pada Realtime Database
            val rtdbPayload = JSONObject().apply {
                put("username", trimmedUsername)
                put("photoURL", targetPhotoUrl)
            }.toString()

            val rtdbReq = Request.Builder()
                .url("$RTDB_BASE/android-users/$uid/profile.json?auth=$idToken")
                .patch(rtdbPayload.toRequestBody(JSON_MEDIA))
                .build()

            client.newCall(rtdbReq).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception("Gagal memperbarui profil di server database (Kode ${resp.code})."))
                }
            }

            // Cermin profil publik ke node users/{uid}/profile agar dapat dibaca oleh versi Windows
            syncProfileToPublic(idToken)

            // 3. Simpan pembaruan ke MMKV
            mmkv.putString(KEY_USERNAME, trimmedUsername)
            mmkv.putString(KEY_PHOTO_URL, targetPhotoUrl)

            // 4. Update in-memory user state
            val currentState = _authState.value
            val updatedUser = if (currentState is AuthGateState.Authenticated) {
                currentState.user.copy(
                    username = trimmedUsername,
                    photoUrl = targetPhotoUrl
                )
            } else {
                NuxUser(
                    uid = uid,
                    email = mmkv.getString(KEY_EMAIL, "") ?: "",
                    username = trimmedUsername,
                    photoUrl = targetPhotoUrl,
                    isActivated = mmkv.getBoolean(KEY_IS_ACTIVATED, false),
                    activatedAt = if (mmkv.containsKey(KEY_ACTIVATED_AT)) mmkv.getLong(KEY_ACTIVATED_AT, 0L) else null,
                    key = mmkv.getString(KEY_LICENSE_KEY, null),
                    isVerified = mmkv.getBoolean(KEY_IS_VERIFIED, false),
                    isPremium = mmkv.getBoolean(KEY_IS_PREMIUM, false)
                )
            }

            _authState.value = AuthGateState.Authenticated(updatedUser)
            Result.success(updatedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    val currentUser: NuxUser?
        get() {
            val uid = mmkv.getString(KEY_UID, null) ?: return null
            return NuxUser(
                uid = uid,
                email = mmkv.getString(KEY_EMAIL, "") ?: "",
                username = mmkv.getString(KEY_USERNAME, "") ?: "",
                photoUrl = mmkv.getString(KEY_PHOTO_URL, "") ?: "",
                isActivated = mmkv.getBoolean(KEY_IS_ACTIVATED, false),
                activatedAt = if (mmkv.containsKey(KEY_ACTIVATED_AT)) mmkv.getLong(KEY_ACTIVATED_AT, 0L) else null,
                key = mmkv.getString(KEY_LICENSE_KEY, null),
                isVerified = mmkv.getBoolean(KEY_IS_VERIFIED, false),
                isPremium = mmkv.getBoolean(KEY_IS_PREMIUM, false)
            )
        }

    /**
     * Menyinkronkan profil pengguna Android ke node users/{uid}/profile di Realtime Database
     * agar profil, username, avatar, dan status dapat dibaca langsung oleh NUX Launcher versi Windows.
     */
    suspend fun syncProfileToPublic(overrideToken: String? = null) = withContext(Dispatchers.IO) {
        val uid = mmkv.getString(KEY_UID, null) ?: return@withContext
        val username = mmkv.getString(KEY_USERNAME, "") ?: ""
        if (username.isEmpty()) return@withContext

        val token = overrideToken ?: getValidIdToken() ?: return@withContext
        val photoUrl = mmkv.getString(KEY_PHOTO_URL, "") ?: ""
        val email = mmkv.getString(KEY_EMAIL, "") ?: ""
        val isActivated = mmkv.getBoolean(KEY_IS_ACTIVATED, false)
        val activatedAt = if (mmkv.containsKey(KEY_ACTIVATED_AT)) mmkv.getLong(KEY_ACTIVATED_AT, 0L) else null
        val isVerified = mmkv.getBoolean(KEY_IS_VERIFIED, false)
        val isPremium = mmkv.getBoolean(KEY_IS_PREMIUM, false)

        try {
            val payload = JSONObject().apply {
                put("username", username)
                put("photoURL", photoUrl)
                put("email", email)
                put("status", "online")
                put("platform", "android")
                put("isAndroid", true)
                put("lastOnline", System.currentTimeMillis())
                put("verified", isVerified)
                if (isActivated || isPremium) {
                    put("isPremium", true)
                    put("subscription", JSONObject().apply {
                        put("tier", "lifetime")
                        if (activatedAt != null) put("activatedAt", activatedAt)
                    })
                }
            }.toString()

            val req = Request.Builder()
                .url("$RTDB_BASE/users/$uid/profile.json?auth=$token")
                .patch(payload.toRequestBody(JSON_MEDIA))
                .build()

            val resp = client.newCall(req).execute()
            resp.close()
            android.util.Log.d("NuxAuth", "syncProfileToPublic: SUCCESS for $uid ($username)")
        } catch (e: Exception) {
            android.util.Log.w("NuxAuth", "syncProfileToPublic: failed", e)
        }
    }

    /**
     * Memperoleh idToken yang masih valid (otomatis me-refresh jika hampir kedaluwarsa)
     */
    suspend fun getValidIdToken(): String? = withContext(Dispatchers.IO) {
        val idToken = mmkv.getString(KEY_ID_TOKEN, null) ?: return@withContext null
        val expiry = mmkv.getLong(KEY_TOKEN_EXPIRY, 0L)
        val refreshToken = mmkv.getString(KEY_REFRESH_TOKEN, null)

        // Jika token masih berlaku lebih dari 5 menit
        if (System.currentTimeMillis() < (expiry - 300_000L)) {
            return@withContext idToken
        }

        if (refreshToken.isNullOrEmpty()) {
            return@withContext idToken // gunakan token saat ini jika tidak ada refresh token
        }

        try {
            val formBody = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .build()

            val req = Request.Builder()
                .url(AUTH_REFRESH_URL)
                .post(formBody)
                .build()

            val resp = client.newCall(req).execute()
            val body = resp.body?.string() ?: ""
            if (resp.isSuccessful) {
                val tokenResp = gson.fromJson(body, FirebaseRefreshTokenResponse::class.java)
                val newIdToken = tokenResp.idToken
                val newRefreshToken = tokenResp.refreshToken
                val newExpiresIn = (tokenResp.expiresIn?.toLongOrNull() ?: 3600L) * 1000L

                if (!newIdToken.isNullOrEmpty()) {
                    mmkv.putString(KEY_ID_TOKEN, newIdToken)
                    if (!newRefreshToken.isNullOrEmpty()) {
                        mmkv.putString(KEY_REFRESH_TOKEN, newRefreshToken)
                    }
                    mmkv.putLong(KEY_TOKEN_EXPIRY, System.currentTimeMillis() + newExpiresIn)
                    return@withContext newIdToken
                }
            }
        } catch (_: Exception) {}

        return@withContext idToken
    }

    private fun parseFirebaseError(jsonStr: String): String {
        try {
            val wrapper = gson.fromJson(jsonStr, FirebaseErrorWrapper::class.java)
            val msg = wrapper?.error?.message ?: return "Terjadi kesalahan pada Firebase ($jsonStr)"
            return when {
                msg.contains("EMAIL_EXISTS") -> "Email ini sudah terdaftar (termasuk jika terdaftar di versi Windows). Akun versi Windows tidak dapat digunakan di Android. Silakan gunakan email lain khusus Android."
                msg.contains("EMAIL_NOT_FOUND") -> "Email tidak ditemukan. Silakan daftar terlebih dahulu."
                msg.contains("INVALID_PASSWORD") -> "Kata sandi salah. Silakan periksa kembali."
                msg.contains("INVALID_LOGIN_CREDENTIALS") -> "Email atau kata sandi tidak cocok."
                msg.contains("USER_DISABLED") -> "Akun ini telah dinonaktifkan oleh administrator."
                msg.contains("TOO_MANY_ATTEMPTS_TRY_LATER") -> "Terlalu banyak percobaan gagal. Silakan coba lagi beberapa saat lagi."
                msg.contains("WEAK_PASSWORD") -> "Kata sandi terlalu lemah. Minimal 6 karakter."
                msg.contains("INVALID_EMAIL") -> "Format alamat email tidak valid."
                else -> msg
            }
        } catch (_: Exception) {
            return jsonStr
        }
    }
}
