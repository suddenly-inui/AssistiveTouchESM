package com.android.mirror.assisttouch.service

import android.content.Intent
import android.content.IntentFilter

interface LockBroadcastReceiverListener {
    fun onScreenOn()
    fun onScreenOff()
    fun onUserPresent()
}

class LockService: LockBroadcastReceiverListener{
    var lbr: LockBroadcastReceiver = LockBroadcastReceiver(this)

    fun onCreate() {
        //レシーバーの登録
//        registerReceiver(lbr, IntentFilter(Intent.ACTION_SCREEN_ON))
//        registerReceiver(lbr, IntentFilter(Intent.ACTION_SCREEN_OFF))
//        registerReceiver(lbr, IntentFilter(Intent.ACTION_USER_PRESENT))
    }

    override fun onScreenOn() {
        //画面ON時の処理
    }

    override fun onScreenOff() {
        //画面OFF時の処理

    }

    override fun onUserPresent() {
        //スクリーンロック解除時の処理
    }
}