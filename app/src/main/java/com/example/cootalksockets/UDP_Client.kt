package com.example.cootalksockets

import android.media.AudioFormat
import android.util.Log
import io.ktor.utils.io.core.toByteArray
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import java.util.Queue

open class UDP_Client {

    val servAddrRaw = "89.111.173.78"
    //val servAddrRaw = "192.168.235.249"
    val servInetAddress = InetAddress.getByName(servAddrRaw)
    //val servInetAddress2 = InetAddress.getByAddress(servAddrRaw)
    val MXAUDIOPKGSZ = 65536
    val AUDIOPKGSZ = 3896
    val AUDIOHDRSZ = 56
    val MXAUDIOSZ = 65480
    val AUDIOSZ = 3840
    val MXPKGSZ = 65536
    val T_RESPONSE = 'R'
    val T_REQUEST = 'r'
    val T_NOANS = 'N'
    val T_ERROR = 'E'
    val T_UDP = 'U'
    val ST_SIP = 'S'
    val ST_AUTH = 'A'
    val ST_AUDIO = 'a'


    var servDataSock: DatagramSocket? = null
    var udpPort: Int? = null
    //instead of mutex use this: https://stackoverflow.com/questions/5291041/is-there-a-mutex-in-java
    //or read this: https://innovationcampus.ru/lms/mod/book/view.php?id=1042
    //may be a problem; try using another class
    val sendBuff = ByteArray(MXAUDIOPKGSZ)
    val rcvBuff = ByteArray(MXPKGSZ)
    //maybe u need to create here





    //creation of builder, but not an instance of class, may cause problems



    fun setup(port: Int): Int {

        //just testing
        //formatBuild.setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        Log.i("UDP_TALK_SETUP", "Setup started")
        Log.i("UDP_TALK_SETUP", "Inet Address: ${servInetAddress}")
        Log.i("UDP_TALK_SETUP", "Port: ${port}")

        udpPort = port

        try {
            //servDataSock = DatagramSocket(udpPort!!, servInetAddress)
            servDataSock = DatagramSocket()
        }
        catch (e: SocketException) {
            Log.e("UDP_TALK_SETUP", "Socket could not be opened or the socket could not bind")
            return -1
        }
        catch (e: SecurityException) {
            Log.e("UDP_TALK_SETUP", "Doesn't have necessary security rights")
            return -2
        }

        Log.e("UDP_TALK_SETUP", "Succesfully!")


        //if not working uncomment and complete connect
        //servAddr.connect()
        val h = "Hello".toByteArray()
        servDataSock!!.connect(servInetAddress, udpPort!!)
        var dataPacket = DatagramPacket(h,5, servInetAddress, udpPort!!)
        try {
           servDataSock!!.send(dataPacket)
        }
        catch (e: IllegalArgumentException) {
            Log.e("UDP_TALK_SETUP", "Socket is connected, and connected address and packet address differ")
            return -1
        }
        catch (e: SecurityException) {
            Log.e("UDP_TALK_SETUP", "Security manager exists and its checkMulticast or checkConnect method doesn't allow the send")
            return -2
        }
        catch (e: IOException) {
            Log.e("UDP_TALK_SETUP", "An I/O error occured")
            return -2
        }

        Log.i("UDP_TALK_SETUP", "UDP connection complete; port ${udpPort}")

        return 1
    }

    fun send(data: ByteArray, dataSize: Int): Boolean {

        val sendPacket = DatagramPacket(data, dataSize, servInetAddress, udpPort!!)
        try {
            servDataSock!!.send(sendPacket)
        }
        catch (e: IllegalArgumentException) {
            Log.e("UDP_TALK_SEND", "Socket is connected, and connected address and packet address differ")
            return false
        }
        catch (e: SecurityException) {
            Log.e("UDP_TALK_SEND", "Security manager exists and its checkMulticast or checkConnect method doesn't allow the send")
            return false
        }
        catch (e: IOException) {
            Log.e("UDP_TALK_SEND", "An I/O error occured")
            return false
        }

        return true
    }

    fun receive(buff: ByteArray): Int {

        //for better optimization you may create rcvPacket as a field, when creating an object of UDP_Talk
        var rcvPacket = DatagramPacket(buff, MXPKGSZ)
        try {
            servDataSock!!.receive(rcvPacket)
        }
        catch (e: Exception) {
            Log.e("UDP_TALK_RECEIVE", "An exception (error?) occured")
            return -1
        }
        //Log.i("UDP_TALK_RECEIVE", "${rcvPacket.length}")

        return rcvPacket.length
    }

    fun sendAPkg(aPkg: APackage) {
        //pkgNum is already in aPkg
        var pkg = ByteArray(AUDIOHDRSZ + aPkg.sizeAudio.toInt())
        aPkg.uppack(pkg)
        //Log.i("UDP_CLIENT_SENDAPKG", "Size of packed pkg: ${pkg.size}")
        send(pkg, AUDIOHDRSZ + aPkg.sizeAudio.toInt())
        aPkg.clear()
    }

    fun recvAPkg(aPkg: APackage) {
        var n: Int
        var pkg = ByteArray(MXPKGSZ)
        n = receive(pkg)

        if (n > AUDIOHDRSZ) {
            aPkg.unpack(pkg)
        }
        else {
            Log.i("UDP_CLIENT_RECV_APKG", "recvAPkg error")
        }
    }

}