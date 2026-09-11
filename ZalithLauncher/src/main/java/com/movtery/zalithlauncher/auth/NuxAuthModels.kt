package com.movtery.zalithlauncher.auth

import com.google.gson.annotations.SerializedName

/**
 * Representasi user NUX Launcher
 */
data class NuxUser(
    val uid: String,
    val email: String,
    val username: String = "",
    val photoUrl: String = "",
    val isActivated: Boolean = false,
    val activatedAt: Long? = null,
    val key: String? = null,
    val isVerified: Boolean = false,
    val isPremium: Boolean = false
)

/**
 * State navigasi Auth Gate pada Launcher
 */
sealed interface AuthGateState {
    object Checking : AuthGateState
    object Unauthenticated : AuthGateState
    data class NeedsActivation(val user: NuxUser) : AuthGateState
    data class Authenticated(val user: NuxUser) : AuthGateState
}

// Model Firebase Auth REST API
data class FirebaseAuthResponse(
    @SerializedName("idToken") val idToken: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("refreshToken") val refreshToken: String? = null,
    @SerializedName("expiresIn") val expiresIn: String? = null,
    @SerializedName("localId") val localId: String? = null,
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("photoUrl") val photoUrl: String? = null,
    @SerializedName("error") val error: FirebaseErrorDetails? = null
)

data class FirebaseRefreshTokenResponse(
    @SerializedName("id_token") val idToken: String? = null,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("expires_in") val expiresIn: String? = null,
    @SerializedName("user_id") val userId: String? = null
)

data class FirebaseErrorWrapper(
    @SerializedName("error") val error: FirebaseErrorDetails? = null
)

data class FirebaseErrorDetails(
    @SerializedName("code") val code: Int? = null,
    @SerializedName("message") val message: String? = null
)

// Model Firebase Realtime Database: android-users/{uid}
data class NuxUserProfile(
    @SerializedName("username") val username: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("photoURL") val photoURL: String? = null,
    @SerializedName("verified") val verified: Boolean? = null,
    @SerializedName("isVerified") val isVerified: Boolean? = null,
    @SerializedName("premium") val premium: Boolean? = null,
    @SerializedName("isPremium") val isPremium: Boolean? = null
) {
    val effectiveVerified: Boolean
        get() = verified == true || isVerified == true
    val effectivePremium: Boolean
        get() = premium == true || isPremium == true
}

data class NuxSubscription(
    @SerializedName("tier") val tier: String? = null, // "lifetime"
    @SerializedName("activatedAt") val activatedAt: Long? = null,
    @SerializedName("key") val key: String? = null
)

data class NuxUserData(
    @SerializedName("profile") val profile: NuxUserProfile? = null,
    @SerializedName("subscription") val subscription: NuxSubscription? = null,
    @SerializedName("createdAt") val createdAt: Long? = null
)

// Model Firebase Realtime Database: android-key/{key}
data class NuxKeyData(
    @SerializedName("used") val used: Boolean? = false,
    @SerializedName("usedBy") val usedBy: String? = null,
    @SerializedName("usedAt") val usedAt: Long? = null,
    @SerializedName("type") val type: String? = null // "lifetime"
)
