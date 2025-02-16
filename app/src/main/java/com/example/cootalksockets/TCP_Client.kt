package com.example.cootalksockets

import java.net.Socket
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.Executors
import com.example.cootalksockets.EventPkg
import com.example.cootalksockets.Package
import java.net.InetAddress

open class TCP_Client {
    val PACKAGE_SIZE = 144


    val SERVERIP = "89.111.173.78"
    //val SERVERIP = "192.168.84.249"
    val PORT = 12345
    val MXPKGSZ = 65635
    val HDRSZ = 72

    val AUDIOPKGSZ = 1016
    val AUDIOHDRSZ = 56
    val AUDIOSZ = 960

    val T_RESPONSE = 'R'
    val T_REQUEST = 'r'
    val T_NOANS = 'N'
    val T_ERROR = 'E'
    val T_UDP = 'U'

    val ET_USREXIST = '1'
    val ET_USRINCORRECT = '2'

    val ST_SIP = 'S'
    val ST_AUTH = 'A'
    val ST_AUDIO = 'a'

    //constructor

    var address: String? = null
    //In Misha's code sock is int; if smth doesn't work, change type
    var sock: Socket? = null
    var port: Int? = null



    //we connect to server using tcp
    fun setup(server: String, port: Int): Boolean {

        sock = Socket(server, port)
        return true

    }

    private fun read(incomingData: ByteArray, n: Int) {

        sock!!.inputStream.read(incomingData, 0, n)

    }

    //don't forget to fill output byte array to 144 bytes in another func
    private fun write(outcomingData: ByteArray, n: Int) {

        sock!!.outputStream.write(outcomingData, 0, n)

    }

    fun recvPkg(socket: Socket): Package {

        val pkg = Package()

        var n = 0
        var d = 0
        val headerBytes = ByteArray(HDRSZ)

        read(headerBytes, HDRSZ)

//        if (n == 0) {
//            return
//        }

        if (!pkg.unpackHeader(headerBytes)) {
            throw Exception()
        }

        if (pkg.sizeData.toInt() > 0) {
            read(pkg.data, pkg.sizeData.toInt())
        }

        return pkg
    }

    fun sendPkg(socket: Socket, pkg: Package) {

        if (pkg.type == null) {
            //change debug method
            println("pkg.type is empty")
        }

        if (pkg.subtype == null) {
            //change debug method
            println("pkg.subtype is empty")
        }

        if (pkg.sizeData == null) {
            //change debug method
            println("pkg.sizeData is empty")
        }

        if (pkg.toUser == "") {
            //change debug method
            println("pkg.toUser is empty")
        }

        if (pkg.fromUser == "") {
            //change debug method
            println("pkg.fromUser is empty")
        }

        val pkgSize = HDRSZ + pkg.sizeData.toInt()

        //may cause problems (spoiled by Misha)
        write(pkg.uppack(), pkgSize)

    }

}