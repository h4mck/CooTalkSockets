package com.example.cootalksockets

import android.util.Log
import java.net.Socket
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.Executors


class CT_Client: TCP_Client() {

    var currentUser: String? = null
    var sip = Sip()
    val outgoingPackages: Queue<Package> = LinkedList()
    var isAuth = false

    //constructor?


    fun init(username: String) {

        currentUser = username
        sip.init(currentUser!!, outgoingPackages)
        val handlerThread = Executors.newSingleThreadExecutor()
        val senderThread = Executors.newSingleThreadExecutor()

        handlerThread.execute { packageHandler() }
        senderThread.execute { packageSender() }

    }


    fun packageHandler() {

        var pkg = Package()
        var res: Int? = null


        while (true) {
            if (isAuth) {
                //if smth doesn't work, change servSoc to socket
                pkg = recvPkg(sock!!)

                //Log.i("I-CTC-PKG", "${pkg.type} ${pkg.subtype} ${pkg.toUser} ${pkg.fromUser}")
                //Log.i("I-CTC-PKG", pkg.data.toString())

                if (pkg.type == T_REQUEST ||
                    pkg.type == T_RESPONSE ||
                    pkg.type == T_NOANS
                ) {
                    if (pkg.subtype == ST_SIP) {
                        if (!sip.handle(pkg)) {
                            sip.close()
                            break
                        }
                    }
                }
            }

        }



    }

    //check for null?
    fun auth(): Boolean {


        var pkg = Package()
        pkg.type = T_REQUEST
        pkg.subtype = ST_AUTH
        pkg.fromUser = currentUser!!
        pkg.toUser = "SERVER"
        pkg.data[0] = 0

        //if smth doesn't work, change servSoc to socket
        sendPkg(sock!!, pkg)

        pkg = recvPkg(sock!!)

        //Log.i("I-CTC-AUTH-PKG", pkg.data.toString())

        if (pkg.type == T_RESPONSE && pkg.subtype == ST_AUTH) {
            isAuth = true
            return true
        } else {
            if (pkg.subtype == ET_USREXIST) {
                //Log.i("I-CTC-AUTH-RES", "User with the same name already exists")
            } else if (pkg.subtype == ET_USRINCORRECT) {
                //Log.i("I-CTC-AUTH-RES", "Username is incorrect")
            } else {
                //Log.i("I-CTC-AUTH-RES", "Error")
            }
            return false
        }


    }

    fun packageSender() {

        var pkg = Package()

        while (true) {
            if (isAuth) {

                if (!outgoingPackages.isEmpty()) {

                    pkg = outgoingPackages.first()
                    sendPkg(sock!!, pkg)
                    outgoingPackages.poll()
                    pkg.clear()

                }
            }

        }

    }

    fun release() {
        sip.release()
    }

}