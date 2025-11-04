package com.example.cootalksockets

import org.json.JSONObject
import org.json.JSONArray

class ChDes {

    var id = ""
    var port = -1
    var timeInfo = TimeInfo()
    var users: MutableSet<String> = mutableSetOf<String>()

    fun unpackSession(jsonObject: JSONObject) {

        timeInfo.conTime = jsonObject.getLong("ConnectTime")
        var talkDes: JSONObject = jsonObject.getJSONObject("Talk-Des")
        id = talkDes.getString("ID")
        port = talkDes.getInt("Port")
        var result: JSONArray = talkDes.getJSONArray("Users")

        //may cause problems
        for (i in 0..result.length()-1) {
            users.add(result[i].toString())
        }

    }

    fun clear() {
        id = ""
        port = -1
        users.clear()
    }

}

class TimeInfo {

    var conTime = 0L
    var cpuTime = 0L

}