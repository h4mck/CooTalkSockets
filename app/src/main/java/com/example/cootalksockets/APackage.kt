package com.example.cootalksockets

import android.util.Log

class APackage {

    var type = 'n'
    var subtype = 'n'
    var num: UShort = 0u
    var sizeAudio: UShort = 0u
    var time = 0L
    var fromUser = ""
    var audio = ByteArray(65536, {i -> 0x00.toByte()})


    //data must be length of AUDIOPKGSZ (1920)!
    fun unpack(data: ByteArray): Boolean {

        type = data[0].toInt().toChar()
        subtype = data[1].toInt().toChar()
        sizeAudio = readUShort(data, 2)
        //Log.i("APKG_UNPACK", "Size of audio: ${sizeAudio}")
        num = readUShort(data, 4)
        time = byteArrayToLong(data.sliceArray(8..15))

        var j = 24
        for (i in 0 .. 31) {
            if (data[j] == 0x00.toByte()) {
                break
            }
            fromUser += data[j].toInt().toChar()
            j++
        }

        j = 56
        if (sizeAudio > 0u) {
            for (i in 0 .. sizeAudio.toInt() - 1) {
                audio[i] = data[j+i]
            }
            Log.i("APKG_UNPACK", "sizeAudio: $sizeAudio audio length: ${data.size - 56}")
        }

        return true
    }

    fun uppack(pkg: ByteArray): Boolean {
        //sizeAudio
        pkg[0] = type.code.toByte()
        pkg[1] = subtype.code.toByte()
        writeUShort(pkg, 2, sizeAudio.toInt())
        writeUShort(pkg, 4, num!!.toInt())

        var timeAsByteArray = longToByteArray(time)

        for (i in 0..7) {
            pkg[8 + i] = timeAsByteArray[i]
        }

        var j = 24
        fromUser.toByteArray().copyInto(pkg, j, 0, fromUser.length)
        //may be a problem; check APackage.hpp
        pkg[j + fromUser.length] = 0x00.toByte()

        j = 56
        if (sizeAudio > 0u) {
            audio.copyInto(pkg, j, 0, sizeAudio.toInt())
        }

        return true
    }

    fun clear() {

        type = 'n'
        subtype = 'n'
        sizeAudio = 0u
        num = 0u
        fromUser = ""
        time = 0L
        audio.fill(0x00.toByte())

    }

    fun readUShort(buffer: ByteArray, offset: Int): UShort {
        return ((0x00 shl 24) or
                (0x00 shl 16) or
                (buffer[offset + 1].toInt() and 0xff shl 8) or
                (buffer[offset + 0].toInt() and 0xff)).toUShort()
    }

    fun writeUShort(buffer: ByteArray, offset: Int, data: Int) {
        buffer[offset + 0] = (data).toByte()
        buffer[offset + 1] = (data shr 8).toByte()
        //Log.i("APKG_WRITE_USHRT", "${data}")
    }

    fun byteArrayToLong(byteArray: ByteArray): Long {
        require(byteArray.size == 8) { "ByteArray must be of size 8" }
        return (byteArray[0].toLong() and 0xFF shl 56) or
                (byteArray[1].toLong() and 0xFF shl 48) or
                (byteArray[2].toLong() and 0xFF shl 40) or
                (byteArray[3].toLong() and 0xFF shl 32) or
                (byteArray[4].toLong() and 0xFF shl 24) or
                (byteArray[5].toLong() and 0xFF shl 16) or
                (byteArray[6].toLong() and 0xFF shl 8) or
                (byteArray[7].toLong() and 0xFF)
    }

    fun longToByteArray(value: Long): ByteArray {
        return byteArrayOf(
            (value shr 56).toByte(),
            (value shr 48).toByte(),
            (value shr 40).toByte(),
            (value shr 32).toByte(),
            (value shr 24).toByte(),
            (value shr 16).toByte(),
            (value shr 8).toByte(),
            value.toByte()
        )
    }

}