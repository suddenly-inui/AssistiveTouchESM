package com.android.mirror.assisttouch.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Message

class SensorServiceListener : Service(), SensorEventListener {
    var sensors: List<Sensor> = mutableListOf()
    private var handler : Handler? = null

    override fun onCreate() {
        super.onCreate()
        val sensorManager: SensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        //sensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
        sensors = listOf(sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER))

        if (sensors.isNotEmpty()) {
            for (i in sensors.indices) {
                val s: Sensor = sensors[i]
                sensorManager.registerListener(this, s, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val sensorManager : SensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val msg = Message.obtain()
        msg.arg1 = 1                                        // センサー値取得イベントを示す値
        msg.arg2 = event.sensor.type                        // センサーの種類を渡す
        msg.obj = event.values.clone()                      // センサーの値をコピーして渡す
        if (handler != null) handler?.sendMessage(msg)
    }

    fun setHandler(handler  : Handler){
        this.handler = handler
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private val binder = LocalBinder()
    inner class LocalBinder : Binder() {
        fun getService(): SensorServiceListener = this@SensorServiceListener
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}