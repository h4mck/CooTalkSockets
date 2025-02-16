package com.example.cootalksockets

class EventPkg {



    var type: Char? = null//1 byte
    var subtype: Char? = null//1 byte
    var dataSize: UShort? = null//2 bytes
    var msgType: Char? = null//1 byte
    var toUsr: String? = null//32 bytes
    var fromUsr: String? = null//32 bytes
    var dataBytes = ByteArray(64)


}