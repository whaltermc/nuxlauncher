package com.movtery.zalithlauncher.social

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

/**
 * Service pendamping sesi Voice Room.
 * Berfungsi membersihkan partisipan dari Firebase Realtime Database saat aplikasi
 * ditutup atau di-swipe dari Recent Apps (onTaskRemoved).
 */
class NuxVoiceSessionService : Service() {
    companion object {
        private const val TAG = "NuxVoiceSessionService"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.i(TAG, "onTaskRemoved: App task removed from recent apps, cleaning up presence and voice room...")
        try {
            NuxSocialManager.leaveVoiceRoomBlocking()
            NuxSocialManager.stopPresenceHeartbeat(setOffline = false)
            NuxSocialManager.updateMyPresenceBlocking("offline")
        } catch (_: Exception) {}
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy: Service destroyed, ensuring voice room is cleaned up...")
        try {
            NuxSocialManager.leaveVoiceRoomBlocking()
        } catch (_: Exception) {}
    }
}
