package com.android.mirror.assisttouch.service

import android.app.Application
import android.content.Context
import android.util.Log
import com.awareframework.android.sensor.aware_appusage.model.AppusageData
import com.google.gson.Gson
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.log

class JsonData: Application() {

    var label:String? = null
    private val dtf: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss:SSS")
    var filename:File? = null

    var accData:MutableList<MutableList<Float>> = mutableListOf()
    var accTime:MutableList<String> = mutableListOf()
    var gyroData:MutableList<MutableList<Float>> = mutableListOf()
    var gyroTime:MutableList<String> = mutableListOf()
    var appData:MutableList<AppusageData> = mutableListOf()
    var appTime:MutableList<String> = mutableListOf()


    fun saveAcc(data:FloatArray= floatArrayOf(), output:Boolean){
        val dt = LocalDateTime.now().format(dtf)
        if(!output) {
            val x:Float = data[0]
            val y:Float = data[0]
            val z:Float = data[0]
            accData.add(mutableListOf(x, y, z))
            accTime.add(dt)
        } else {
            File(filename, "Acc-${dt}.csv").writer().use {
                var s:String = ""
                for(i in 0 until accData.size){
                    s += "${accTime[i]}, ${accData[i][0]}, ${accData[i][1]}, ${accData[i][2]}, ${label}\n"
                }
                it.write(s)
            }
            accData = mutableListOf()
            println("Acc saved")
        }
    }

    fun saveGyro(data:FloatArray= floatArrayOf(), output:Boolean) {
        val dt = LocalDateTime.now().format(dtf)
        if (!output) {
            val x: Float = data[0]
            val y: Float = data[0]
            val z: Float = data[0]
            gyroData.add(mutableListOf(x, y, z))
            gyroTime.add(dt)
        } else {
            File(filename, "Gyro-${dt}.csv").writer().use {
                var s: String = ""
                for (i in 0 until gyroData.size) {
                    s += "${gyroTime[i]}, ${gyroData[i][0]}, ${gyroData[i][1]}, ${gyroData[i][2]}, ${label}\n"
                }
                it.write(s)
                gyroData = mutableListOf()
            }
            println("Gyro saved")
        }
    }

    fun saveApp(data:MutableList<AppusageData>?=null, output: Boolean){
        val dt = LocalDateTime.now().format(dtf)
        val listSize = 2  // Pixelの場合？要確認
        if(!output){
            if(data!!.size == listSize){
                for (i in 0 until listSize){
                    if(data[i].appPackageName != "com.google.android.apps.nexuslauncher"){
                        appData.add(data[i])
                        appTime.add(dt)
                    }
                }
            }
        } else {
            File(filename, "App-${dt}.csv").writer().use {
                var s: String = ""
                for (i in 0 until appData.size) {
                    s += "${appTime[i]},${appData[i].appPackageName}, ${appData[i].eventType} ${label}\n"
                }
                it.write(s)
                appData = mutableListOf()
            }
            println("App saved")
        }
    }

    fun resetData(){
        accData = mutableListOf()
        accTime = mutableListOf()
        gyroData = mutableListOf()
        gyroTime = mutableListOf()
        appData = mutableListOf()
        appTime = mutableListOf()
    }

    companion object {
        private var instance : JsonData? = null

        fun  getInstance(): JsonData {
            if (instance == null)
                instance = JsonData()

            return instance!!
        }
    }
}