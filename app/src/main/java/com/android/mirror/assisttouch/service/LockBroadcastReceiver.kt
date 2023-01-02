package com.android.mirror.assisttouch.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class LockBroadcastReceiver constructor(_listener: LockBroadcastReceiverListener): BroadcastReceiver() {

    private lateinit var listener: LockBroadcastReceiverListener

    init{
        listener = _listener
    }

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        val action: String? = intent.getAction()
        if(action.equals(Intent.ACTION_SCREEN_ON)){
            listener.onScreenOn()

        }else if(action.equals(Intent.ACTION_SCREEN_OFF)){
            listener.onScreenOff()

        }else if(action.equals(Intent.ACTION_USER_PRESENT)){
            listener.onUserPresent()
        }
    }
}

