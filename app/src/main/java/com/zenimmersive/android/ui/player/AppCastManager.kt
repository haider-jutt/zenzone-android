package com.zenimmersive.android.ui.player

import android.content.Context
import com.zenimmersive.android.helper.LogSystem
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.Session
import com.google.android.gms.cast.framework.SessionManagerListener

object AppCastManager {
    private val TAG = "PlayerManagement"
    private val sessionManagerListener = object : SessionManagerListener<Session> {
        override fun onSessionEnded(session: Session, p1: Int) {
            LogSystem.e(TAG, "RemoteMedia onSessionEnded Invoked ${session.sessionId}")
            playerListener?.onSessionEnded()
        }

        override fun onSessionEnding(session: Session) {
            LogSystem.e(TAG, "RemoteMedia onSessionEnding Invoked ${session.sessionId}")
        }

        override fun onSessionResumeFailed(session: Session, p1: Int) {
            LogSystem.e(TAG, "RemoteMedia onSessionResumeFailed Invoked ${session.sessionId}")
            playerListener?.onCastSessionDisconnected()
        }

        override fun onSessionResumed(session: Session, p1: Boolean) {
            LogSystem.e(TAG, "RemoteMedia onSessionResumed Invoked ${session.sessionId}")
            playerListener?.onCastSessionConnected()
            sessionListener?.startCastingContent("onSessionResumed")
        }

        override fun onSessionResuming(session: Session, p1: String) {
            LogSystem.e(TAG, "RemoteMedia onSessionResuming Invoked ${session.sessionId}")
        }

        override fun onSessionStartFailed(session: Session, p1: Int) {
            LogSystem.e(TAG, "RemoteMedia onSessionStartFailed Invoked ${session.sessionId}")
            playerListener?.onCastSessionDisconnected()
        }

        override fun onSessionStarted(session: Session, p1: String) {
            LogSystem.e(TAG, "RemoteMedia onSessionStarted Invoked ${session.sessionId}")
            sessionListener?.startCastingContent("onSessionStarted")

        }

        override fun onSessionStarting(session: Session) {
            LogSystem.e(TAG, "RemoteMedia onSessionStarting Invoked ${session.sessionId}")
            playerListener?.onCastSessionConnected()
        }

        override fun onSessionSuspended(session: Session, p1: Int) {
            LogSystem.e(TAG, "RemoteMedia onSessionSuspended Invoked ${session.sessionId}")
            playerListener?.onCastSessionDisconnected()
        }

    }


    var castContext: CastContext? = null
    fun setupCastSession(context: Context) {
        if (castContext == null) {
            castContext = CastContext.getSharedInstance(context)
            castContext?.sessionManager?.removeSessionManagerListener(sessionManagerListener)
            castContext?.sessionManager?.addSessionManagerListener(sessionManagerListener)
        }
    }

    var playerListener: PlayerListener? = null

    var sessionListener : AppCastSessionListener? = null


    interface AppCastSessionListener {
        fun startCastingContent(caller : String)
    }
}