package com.android.mirror.assisttouch.service

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.gson.Gson
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.log

class JsonData: Application() {

    var label:String? = null
    private val dtf: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss:SSS")
    var filename:File? = null

    private var accData:MutableList<MutableList<Float>> = mutableListOf()
    private var gyroData:MutableList<MutableList<Float>> = mutableListOf()

    fun saveAcc(data:FloatArray= floatArrayOf(), output:Boolean){
        val dt = LocalDateTime.now().format(dtf)
        if(!output) {
            val x:Float = data[0]
            val y:Float = data[0]
            val z:Float = data[0]
            accData.add(mutableListOf(x, y, z))
        } else {
            File(filename, "Acc-${dt}.csv").writer().use {
                var s:String = ""
                for(i in accData){
                    s += "$dt, ${i[0]}, ${i[1]}, ${i[2]}, ${label}\n"
                }
                it.write(s)
            }
            accData = mutableListOf()
        }
    }

    fun saveGyro(data:FloatArray= floatArrayOf(), output:Boolean) {
        val dt = LocalDateTime.now().format(dtf)
        if (!output) {
            val x: Float = data[0]
            val y: Float = data[0]
            val z: Float = data[0]
            gyroData.add(mutableListOf(x, y, z))
        } else {
            File(filename, "Gyro-${dt}.csv").writer().use {
                var s: String = ""
                for (i in gyroData) {
                    s += "$dt, ${i[0]}, ${i[1]}, ${i[2]}, ${label}\n"
                }
                it.write(s)
                gyroData = mutableListOf()
            }
        }
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