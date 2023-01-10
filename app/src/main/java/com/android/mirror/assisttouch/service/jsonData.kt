package com.android.mirror.assisttouch.service

import android.app.Application
import android.content.Context
import com.google.gson.Gson
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class JsonData: Application() {
    val gson:Gson = Gson()

    var label:String? = null
    var sensorData:MutableMap<String, MutableList<MutableMap<String, String>>> = mutableMapOf()
    private val dtf: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss:SSS")
    var filename:File? = null


    fun saveJson(){
        val wholeJson: MutableMap<String, MutableMap<String, MutableList<MutableMap<String, String>>>> = mutableMapOf()
        wholeJson[label.toString()] = sensorData
        val json = gson.toJson(wholeJson)

        File(filename, "data${LocalDateTime.now().format(dtf)}.json").writer().use {
            it.write(json)
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