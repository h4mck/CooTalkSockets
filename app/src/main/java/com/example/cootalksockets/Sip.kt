package com.example.cootalksockets

import android.util.Log
import java.util.Queue
import com.example.cootalksockets.TCP_Client
import org.json.JSONObject
import com.example.cootalksockets.Package
import org.json.JSONException

class Sip {

    var tcpClient = TCP_Client()

    //may cause problems
    var curOutgoingPackages: Queue<Package>? = null
    var curUser = ""
    var toUserSip = ""
    var SIPState = ""

    var currCh = ChDes()

    fun handle(pkg: Package): Boolean {

        var jsonStr = pkg.dataToString()
        var jobj = dataToJson(jsonStr)

        if (jobj["Type"] == "REQUEST") {
            if (jobj["Method"] == "SESSION") {
                return sessionHandler(jobj)
            }
            else if (jobj["Method"] == "UPDATE") {
                return updateHandler(jobj)
            }
        }

        return true
    }

    private fun updateHandler(jsonObject: JSONObject): Boolean {

        if (currUDP.currChannel.id == jsonObject.getJSONObject("Talk-Des").getString("ID")) {

            if (jsonObject["Action"] == "connect") {
                currUDP.createNewRecieveThread(jsonObject["From"])
            }
            else if (jsonObject["Action"] == "disconnect") {
                Log.i("SIP-INFO", "User ${jsonObject["From"]} disconnected")
            }

        }
        return true

    }

    private fun sessionHandler(jsonObject: JSONObject): Boolean {

        var ch = ChDes()
        ch.unpackSession(jsonObject)
        currCh = ch

        Log.i("SIP-INFO", "UDP STARTS")

        return true

    }

    fun close() {
        TODO("Not yet implemented")
    }

    //redo
    fun init(User: String, outgoingPackages: Queue<Package>) {

    }

    fun sendConnect(talkID: String) {
        var connectPkg = createConnectPkg(talkID)
        try {
            curOutgoingPackages!!.add(connectPkg)
        } catch (e1: IllegalStateException) {
            Log.e("SIP-EXCP-CON", "No space available to add package")
        } catch (e2: NullPointerException) {
            Log.e("SIP-EXCP-CON", "Package is null! (object itself == null)")
        }
    }

    fun createConnectPkg(talkID: String): Package {
        var conPkg = Package()
        conPkg.type = tcpClient.T_REQUEST
        conPkg.subtype = tcpClient.ST_SIP
        //check toUserSip!
        conPkg.toUser = toUserSip
        conPkg.fromUser = curUser

        var jobj = JSONObject()
        //may cause problems
        jobj.accumulate("Type", "REQUEST")
        jobj.accumulate("Method", "CHANGE")
        //check curUser!
        jobj.accumulate("From", curUser)
        jobj.accumulate("Talk", "channel")
        //uncomment "Talk-ID", currCh.id and remove
        jobj.accumulate("Talk-ID", "unkwn")
        //jobj.accumulate("Talk-ID", currCh.id)
        //change "unkwn" to var action
        jobj.accumulate("Action", "unkwn")
        //change "unkwn" to var value
        jobj.accumulate("Value", "unkwn")
        uppackSIPData(conPkg, jobj)
        //DEBUG
        Log.i(null, "$jobj")
        Log.i(null, jobj.toString())


        return conPkg
    }

    fun createUpdatePkg(action: String, value: String) {

        var pkg = Package()
        pkg.type = tcpClient.T_NOANS
        pkg.subtype = tcpClient.ST_SIP
        pkg.toUser = toUserSip
        pkg.fromUser = curUser

        //may cause problems
        var jobj = JSONObject()
        jobj.accumulate("Type", "REQUEST")
        jobj.accumulate("Method", "CHANGE")
        jobj.accumulate("From", curUser)


    }

    private fun uppackSIPData(Pkg: Package, jsonObject: JSONObject) {

    }

    //redo!!!
    fun dataToJson(pkgDataStr: String): JSONObject {

        //uncomment if smth doesn't work
        //var JSON_Str = JSONObject.quote(pkgDataStr)
        var JSON_Object = JSONObject()
        try {
            JSON_Object = JSON_Object.getJSONObject(pkgDataStr)
        }
        catch (e: JSONException) {
            //may cause problems
            Log.e("PACKAGE_DATA_ERROR", "Package data is corrupted")
        }
        return JSON_Object

    }



}