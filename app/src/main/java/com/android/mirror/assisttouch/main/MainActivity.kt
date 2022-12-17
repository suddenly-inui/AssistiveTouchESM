package com.android.mirror.assisttouch.main

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.android.mirror.assisttouch.R
import com.android.mirror.assisttouch.utils.SystemsUtils
import android.content.ComponentName
import com.android.mirror.assisttouch.MyAdminReceiver
import android.content.Intent
import android.app.admin.DevicePolicyManager
import com.android.mirror.assisttouch.service.AssistiveTouchService
import android.app.ActivityManager
import android.util.Log
import android.view.View
import android.widget.Button

class MainActivity : AppCompatActivity() {
    private var startBtn: Button? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("Mirror", SystemsUtils.isRooted().toString() + " ")
        val componentName = ComponentName(this, MyAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
        startActivityForResult(intent, 0)
        startBtn = findViewById<View>(R.id.startBtn) as Button
        startBtn!!.setOnClickListener { v: View? ->
            val intent1 = Intent(this@MainActivity, AssistiveTouchService::class.java)
            if (isMyServiceRunning(AssistiveTouchService::class.java)) stopService(intent1) else startService(
                intent1
            )
        }
    }

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }
}