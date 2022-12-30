package com.android.mirror.assisttouch.main

import android.app.Activity
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
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.awareframework.android.core.db.Engine
import com.awareframework.android.sensor.aware_appusage.AppusageSensor
import com.awareframework.android.sensor.aware_appusage.model.AppusageData

class MainActivity : AppCompatActivity() {
    private var startBtn: Button? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        Log.d("Mirror", SystemsUtils.isRooted().toString() + " ")

        val componentName = ComponentName(this, MyAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
        startActivityForResult(intent, 0)  //TODO: 直す

        // Permissions
        if(checkOverlayPermission()){
            println("Overlay permission true")
        }else{
            requestOverlayPermission()
        }

        if(checkReadStatsPermission()){
            println("ReadStates permission true")
        }else{
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        // Start or Stop
        startBtn = findViewById<View>(R.id.startBtn) as Button
        startBtn!!.setOnClickListener {
            val intent1 = Intent(this@MainActivity, AssistiveTouchService::class.java)
            if (isMyServiceRunning(AssistiveTouchService::class.java)) {
                stopService(intent1)
            } else {
                startService(intent1)
            }
        }



    }

        //Aware pappusage plugin
//    private fun aware(){
//        AppusageSensor.start(applicationContext, AppusageSensor.Config().apply {
//
//            interval = 1000 //1sec
//            usageAppDisplaynames = mutableListOf("com.twitter.android", "com.facebook.orca", "com.facebook.katana", "com.instagram.android", "jp.naver.line.android", "com.ss.android.ugc.trill")
//            usageAppEventTypes = mutableListOf(UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_RESUMED)
//
//            awareUsageAppNotificationTitle = "studying now"
//            awareUsageAppNotificationDescription = "App usage history is being retrieved."
//            awareUsageAppNoticationId = "appusage_notification"
//
//            dbType = Engine.DatabaseType.ROOM
//
//            sensorObserver = object : AppusageSensor.Observer {
//                override fun onDataChanged(datas: MutableList<AppusageData>?) {
//                    println("ondatachanged in mainActivity $datas")
//                }
//            }
//        })
//    }


    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun checkReadStatsPermission():Boolean{
        val aom: AppOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode:Int = aom.checkOp(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        if(mode == AppOpsManager.MODE_DEFAULT){
            return checkPermission("android.permission.PACKAGE_USAGE_STATS", android.os.Process.myPid(), android.os.Process.myUid()) == PackageManager.PERMISSION_GRANTED
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun Context.checkOverlayPermission(): Boolean {
        if (Build.VERSION.SDK_INT < 23) {
            return true
        }
        return Settings.canDrawOverlays(this)
    }

    private fun Activity.requestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${packageName}"));
        startActivity(intent)
        launcher.launch(intent)
    }

    private var launcher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // 許可されたか再確認
        if(Settings.canDrawOverlays(this)){
            // Serviceに跳ぶ
            val intent1 = Intent(this@MainActivity, AssistiveTouchService::class.java)  //TODO バグかも？注意
            startService(intent1)
        }
        else{
            //
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }
}