package com.android.mirror.assisttouch.main

import android.app.Activity
import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.*
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.android.mirror.assisttouch.MyAdminReceiver
import com.android.mirror.assisttouch.R
import com.android.mirror.assisttouch.service.AssistiveTouchService
import com.android.mirror.assisttouch.service.SensorService
import com.android.mirror.assisttouch.service.SensorServiceInterface
import com.android.mirror.assisttouch.utils.SystemsUtils
import com.google.gson.Gson
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class MainActivity : AppCompatActivity(){

    private var startBtn: Button? = null
    private var sensorManager: SensorManager? = null
    val sensorService = SensorService(this)
    var screenStatus:Boolean = false
    var sensorData:MutableMap<String?, MutableList<MutableMap<String, String>>> = mutableMapOf()
    val dtf: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS")
    val gson:Gson = Gson()

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

        // Sensors
        sensorService.setListener(sensorListener)
        sensorService.start()

        //screen
        val intentFilter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        intentFilter.addAction(Intent.ACTION_SCREEN_ON)
        val receiver = object: BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent!!.action
                Log.d("Test", "receive : $action")

                when (action) {
                    Intent.ACTION_SCREEN_ON -> {
                        screenStatus = true
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        screenStatus = false
                    }
                }
            }
        }
        registerReceiver(receiver, intentFilter)
        }

    private val sensorListener = object : SensorServiceInterface {
        override fun onSensorChanged(sensorType: Int, values: FloatArray) {
            var idx = -1
            for (i in 0 until sensorService.sensors.size) {
                if (sensorService.sensors[i].type == sensorType) {
                    idx = i
                }
            }
            if (idx != -1) {
                val date = LocalDateTime.now()
                val li:MutableList<MutableMap<String, String>> = mutableListOf()
                for(i in values.indices){
                    //Log.d("SensorData", "$sensorType ${sensorService.sensors[idx].name} ${listOf("X", "Y", "Z")[i]} ${values[i]}")
                    li.add(mutableMapOf("${sensorService.sensors[idx].name} ${listOf("X", "Y", "Z")[i]}" to "${values[i]}"))

                }
                sensorData[date.format(dtf)] = li
            }
        }

        override fun onAccuracyChanged(sensorType: Int, accuracy: Int) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorService.stop()
        val json = gson.toJson(sensorData)
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