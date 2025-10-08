package com.example.cootalksockets

import android.util.Log
import androidx.compose.ui.geometry.Offset
import kotlin.UShort
import com.example.cootalksockets.TCP_Client
import org.json.JSONException
import java.lang.Package
import java.time.LocalDateTime
//may be problems with import lower
import org.json.JSONObject

class Package {

    //



    val tcpClient = TCP_Client()

    //headerBytes
    var type: Char? = null
    var subtype: Char? = null
    var sizeData: UShort = 0u
    var toUser: String = ""
    var fromUser: String = ""
    //dataBytes
    val data = ByteArray(tcpClient.MXPKGSZ, {i -> 0x00.toByte()})


    //funcs

    //headerBytes is 72 bytes length (HDRSZ)
    fun unpackHeader(headerBytes: ByteArray): Boolean {

        type = headerBytes[0].toInt().toChar()
        subtype = headerBytes[1].toInt().toChar()
        //sizeData = headerBytes[2].
        sizeData = readDataSize(headerBytes.sliceArray(2..3), 0)
        //number of bytes
        var j = 8

        for(i in 0..31) {
            if (headerBytes[j] == 0x00.toByte())
                break
            toUser += headerBytes[j].toInt().toChar()
            j++
        }

        j = 40
        for(i in 0..31) {
            if (headerBytes[j] == 0x00.toByte())
                break
            fromUser += headerBytes[j].toInt().toChar()
            j++
        }

        return true

    }



    fun uppack(): ByteArray {

        val pkg = ByteArray(tcpClient.MXPKGSZ)

        //May cause troubles!!!
        pkg[0] = type!!.code.toByte()
        pkg[1] = subtype!!.code.toByte()
        writeDataSize(pkg, 2, sizeData!!.toInt())

        var j = 8
        for(i in 0..<toUser.length) {
            pkg[j] = toUser[i].code.toByte()
            ++j
        }
        pkg[j] = 0x00.toByte()

        j = 40
        for (i in 0..<fromUser.length) {
            pkg[j] = fromUser[i].code.toByte()
            ++j
        }
        pkg[j] = 0x00.toByte()

        j=72
        if (sizeData!!.toInt() > 0) {
            data.copyInto(pkg, j, 0, sizeData.toInt())
        }

        return pkg

    }


    fun dataToString(): String {
        var str = ""
        var i = 0
        while ((i < sizeData.toInt()) and (data[i] != 0x00.toByte())) {
            str += data[i].toInt().toChar()
            ++i
        }
        return str
    }

    fun clear() {
        type = null
        subtype = null
        sizeData = 0u
        toUser = ""
        fromUser = ""
        data.fill(0)
    }

    //May cause troubles!!!
    fun readDataSize(buffer: ByteArray, offset: Int): UShort {
        return ((0x00 shl 24) or
                (0x00 shl 16) or
                (buffer[offset + 1].toInt() and 0xff shl 8) or
                (buffer[offset + 0].toInt() and 0xff)).toUShort()
    }

    fun writeDataSize(buffer: ByteArray, offset: Int, data: Int) {
        buffer[offset + 0] = (data).toByte()
        buffer[offset + 1] = (data shr 8).toByte()
        //Log.i("PKG_WRDATASZ", "${data}")
    }

    fun dataToJson(): JSONObject {

        var jsonStr = dataToString()

        //uncomment if smth doesn't work
        //var JSON_Str = JSONObject.quote(pkgDataStr)

        var JSON_Object = JSONObject(jsonStr)

        return JSON_Object

    }

    //instead of method Sip.uppackSIPData
    fun jsonToData(JSON_Object: JSONObject) {

        var JSON_Str = JSON_Object.toString()
        Log.i("PKG_JSON_TO_DATA", JSON_Str)
        sizeData = JSON_Str.length.toUShort()
        for (i in 0..JSON_Str.length-1) {
            //String.code may cause troubles
            data[i] = JSON_Str[i].code.toByte()
        }


    }

}