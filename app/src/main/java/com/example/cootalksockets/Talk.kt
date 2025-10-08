package com.example.cootalksockets
import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder.AudioSource
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService
import com.example.cootalksockets.UDP_Client
import com.theeasiestway.opus.Constants
import io.ktor.util.readShort
import io.ktor.utils.io.charsets.decodeUtf8Result
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.Executors
import kotlin.math.abs
import com.theeasiestway.opus.Opus
import java.nio.ByteBuffer
import java.nio.ByteOrder


class Talk(): UDP_Client() {

    var opus = Opus()
    var newUsr = ""
    var currUsr = ""
    var currCh = ChDes()

    //var userPkgs = mutableMapOf<String, Queue<APackage>>()
    var userPkgs = mutableMapOf<String, Queue<APackage>>()
    var countPkgs = 0L
    var lastPing: Long? = null

    var aFormat: AudioFormat? = null

    //may be a problem
    //var mContext = aContext

    fun start(channel: ChDes, user: String){
        // ПАРАМЕТРЫ: ССылка на структуру текущего канала из ПОЛЕЙ класса SIP, имя текущего пользователя
        // !!! ПЕРВЫМ ДЕЛОМ ВЫЗЫВАЕТ УНАСЛЕДОВАННУЮ ФУНКЦИЮ SETUP(Функция setup не принимет параметров (Сделай так))
        // А ЕЩЁ ПЕРЕНЕСИ ПОЛЯ ОТВЕЧАЮЩИЕ ЗА РАЗГОВОР В ЭТОТ КЛАСС, А ПОЛЯ ОТВЕЧАЮЩИЕ ЗА ПОДКЛЮЧЕНИЯ ОСТАВЬ В КЛАССЕ UDP_Client
        // создание send потока
        // sendThread отвечает за отправку полученных с микрофона данных
        // receiveThread принимает аудиоданные от других пользователей и раскладывает их по
        // очередям соответствующих пользователей
        // playThread извлекает первый пакет из очереди каждого опльзователя и смешивает их, создавая
        // единый звук

        currUsr = user
        currCh = channel

        setup(channel.port)
        setupFormat()
        setupOpus()

        Executors.newSingleThreadExecutor().execute() {
            playThread()
        }

        Executors.newSingleThreadExecutor().execute() {
            receiveThread()
        }

        Executors.newSingleThreadExecutor().execute() {
            sendThread()
        }

    }

    fun playThread()
    {
        // Возспроизводит смешанное аудио
        Log.i("TALK_THREAD_PLAY", "Play thread has been created")

        val audioAttributes = AudioAttributes.Builder()
        audioAttributes.setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
        audioAttributes.setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)

        var outDevice: AudioTrack? = null
        try {
            outDevice = AudioTrack(
                audioAttributes.build(),
                aFormat,
                AUDIOSZ,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )
        }
        catch (e: IllegalArgumentException) {
            Log.e("TALK_THREAD_PLAY", "Illegal args when creating OutDevice")
        }

        if (outDevice != null) {
            outDevice.setVolume(2f)
        }

        isFormat(outDevice!!, aFormat!!)

        var run = isFormat(outDevice!!, aFormat!!)
        if (run) {

            //may be a problem; try using play after outDevice.write
            outDevice.play()
            var prevPkg: UInt = 0u
            var mixedAudio = ByteArray(AUDIOSZ)

            while (true) {

                //if (countPkgs > 0) {
//                mix(mixedAudio, AUDIOSZ)
//                Log.i("TALK_THREAD_PLAY", "Length of package to play: ${mixedAudio.size}")
//                Log.i("TALK_THREAD_PLAY", "First byte: ${mixedAudio[0].toInt()}")
//                Log.i("TALK_THREAD_PLAY", "Second byte: ${mixedAudio[1].toInt()}")
                //outDevice.write(mixedAudio, 0, AUDIOSZ)
                val terms = mutableListOf<APackage>()
                userPkgs.forEach { entry ->
                    if (entry.value.isNotEmpty()) {
                        val aPkg = entry.value.peek()
                        terms.add(aPkg)
                        userPkgs[entry.key]!!.remove()
                        countPkgs--
                    } else {
                        //terms.add(APackage()) // Добавляем пустой пакет, если очередь пуста
                    }
                }
                if (terms.isNotEmpty()) {
                    outDevice.write(increaseVolume(terms[0].audio, 2.0f), 0, AUDIOSZ)
                    terms.removeAt(0)
                }
                //}
                //else {
                    //may be a problem
                    //Thread.sleep(30)
                //}

            }

        }

        Log.i("TALK_PLAY_THREAD", "PlayThread has been deleted")

    }
    fun sendThread()
    {

        //Log.i("TALK_SEND", "${AudioRecord.getMinBufferSize(24000, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT)}")
        Log.i("TALK_THREAD_SEND", "Send thread has been created")
        // Отправляет смешанное аудио
        //var sendBuff = ByteArray(AUDIOPKGSZ)
        val minRecBuffSize = AudioRecord.getMinBufferSize(24000, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT)
        var recordBuff = ByteArray(minRecBuffSize)
        var sendBuff: ByteArray? = ByteArray(AUDIOSZ)
        var aPkg = APackage()
        //aFormat!! may cause problems
//        var audioRecord = AudioRecord(
//            AudioSource.MIC,
//            aFormat!!.sampleRate,
//            aFormat!!.channelMask,
//            aFormat!!.encoding,
//            AUDIOSZ
//        )

//        var audioRecord = AudioRecord(
//            AudioSource.MIC,
//            24000,
//            AudioFormat.CHANNEL_IN_STEREO,
//            AudioFormat.ENCODING_PCM_16BIT,
//            AUDIOSZ
//        )

        var audioRecord = AudioRecord(
            AudioSource.MIC,
            24000,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
            minRecBuffSize
        )


        var run = isFormat(audioRecord, aFormat!!)
        if (run) {
            audioRecord.startRecording()
            while (true) {
                record(audioRecord, recordBuff)
                //sendBuff = opus.encode(recordBuff, Constants.FrameSize._1920())
                sendBuff = opus.encode(recordBuff, Constants.FrameSize._960())
                //Log.i("TALK_THREAD_SEND", "${sendBuff!!.size}")
                
                sendBuff!!.copyInto(aPkg.audio, 0, 0, sendBuff!!.size)
                fillAPkg(aPkg, sendBuff!!.size)
                sendAPkg(aPkg)
            }
        }

    }

    fun receiveThread() {
        Log.i("TALK_RCV_THREAD", "Receive thread has been created")
        var rcvdAPkg = APackage()
        var decodedAPkg = APackage()
        Log.i("TALK_RCV_THREAD", "APkg created")
        var rt: Long? = null
        var st: Long? = null
        var dif: Long? = null

        while (true) {
            recvAPkg(rcvdAPkg)
            //uptimeMillis may be a problem; try using another clock
            Log.i("TALK_RCV_THREAD", "APkg rcvd")
            rt = currCh.timeInfo.conTime + SystemClock.uptimeMillis() - currCh.timeInfo.cpuTime
            st = getTime(rcvdAPkg)
            dif = abs(rt - st)

//            Log.i("TALK_RECEIVE_THRD", "dif is: $st")
//            Log.i("TALK_RECEIVE_THRD", "dif is: $rt")
//            Log.i("TALK_RECEIVE_THRD", "dif is: $dif")
            //if (dif <= 100 && rcvdAPkg.fromUser != currUsr) {
            if (rcvdAPkg.fromUser != currUsr) {
                lastPing = dif

                decodeFromOpus(decodedAPkg, rcvdAPkg)

                try {
                    userPkgs[decodedAPkg.fromUser] = LinkedList<APackage>()
                    userPkgs[decodedAPkg.fromUser]!!.add(decodedAPkg)
                    Log.i("TALK_RCV_THREAD", "APkg added")
                }
                catch (e: IllegalStateException) {
                    Log.e("TALK_RCV_THREAD", "Cannot add APkg to a queue: it's full")
                }
                catch (e: NullPointerException) {
                    Log.e("TALK_RCV_THREAD", "Cannot add APkg to a queue: the APkg is null")
                }

                countPkgs++
            }

            rcvdAPkg.clear()
            decodedAPkg.clear()
        }
    }

    fun setupFormat(){
        // устанавливает поле отвечающее за формат класса  Talk
        var aFormatBuilder = AudioFormat.Builder()
        aFormatBuilder.setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        aFormatBuilder.setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
        aFormatBuilder.setSampleRate(24000)
        aFormat = aFormatBuilder.build()
        Log.i("FORMAT", aFormat!!.encoding.toString())

    }

    fun isFormat(trackDev: AudioTrack, aFormat: AudioFormat): Boolean {
        // в качестве параметра принимает формат
        // проверка подходит ли данный формат
        if (AudioTrack.getMinBufferSize(aFormat.sampleRate, aFormat.channelMask, aFormat.encoding) < 0) {
            Log.e("TALK_IS_FORMAT_TRACK", "Cannot play audio on the current device")
            return false
        }
        else {
            Log.i("TALK_IS_FORMAT_TRACK", "Audio can be played on the current device")
            return true
        }
    }

    fun isFormat(recDev: AudioRecord, aFormat: AudioFormat): Boolean {
        // в качестве параметра принимает формат
        // проверка подходит ли данный формат
        if (AudioRecord.getMinBufferSize(aFormat.sampleRate, aFormat.channelMask, aFormat.encoding) < 0) {
            Log.e("TALK_IS_FORMAT_RECORD", "Cannot record audio on the current device")
            return false
        }
        else {
            Log.i("TALK_IS_FORMAT_RECORD", "Audio can be recorded on the current device")
            return true
        }
    }

    fun setupOpus() {
        opus.encoderInit(Constants.SampleRate._24000(), Constants.Channels.stereo(), Constants.Application.audio())
        opus.decoderInit(Constants.SampleRate._24000(), Constants.Channels.stereo())
    }

    fun record(audioRecord: AudioRecord, buff: ByteArray) {
        // Функция вызывается в потоке sendThread
        // параметр: byteArray, который будет отправлен позже в sendThread

        //aFormat!! may cause problems

//        var aPkg = APackage()
//        var n = 0
//        var pkgNum: UInt = 0u
//
//        var suma = 0
//        var maxa = 0
//        var pred = 0
//
//
//        var temp = ByteArray(AUDIOSZ / 2)


        audioRecord.read(buff, 0, buff.size)

    }

    fun decodeFromOpus(targetAPkg: APackage, sourceAPkg: APackage) {

        targetAPkg.type = sourceAPkg.type
        targetAPkg.subtype = sourceAPkg.subtype
        targetAPkg.num = sourceAPkg.num
        targetAPkg.sizeAudio = AUDIOSZ.toUShort()
        targetAPkg.time = sourceAPkg.time
        targetAPkg.fromUser = sourceAPkg.fromUser

        Log.i("TALK_DECODE", "${sourceAPkg.sizeAudio} = ${sourceAPkg.audio.sliceArray(0..sourceAPkg.sizeAudio.toInt()-1).size}")
        var audioPCM = opus.decode(sourceAPkg.audio.sliceArray(0..sourceAPkg.sizeAudio.toInt()-1), Constants.FrameSize._960())
        if (audioPCM != null) {
            Log.i("TALK_DECODE", "Decoded audio size: ${audioPCM!!.size}")
            //may cause problems! you may need to use AUDIOSZ instead of audioPCM.size
            audioPCM!!.copyInto(targetAPkg.audio, 0, 0, AUDIOSZ)
        }

    }

    fun addUser(user: String) {
        newUsr = user
        //using {LinkedList<APackage>()} may cause problems!
        userPkgs[user] = userPkgs.getOrPut(user) {LinkedList<APackage>()}
    }

    fun delUser(user: String) {
        userPkgs.remove(user)
    }

//    fun mix(mixedAudio: ByteArray, audioSize: Int){
//        mixedAudio.fill(0)
//        var terms = mutableListOf<APackage>()
//
//        userPkgs.forEach { entry ->
//            if (!entry.value.isEmpty()) {
//                var aPkg = entry.value.peek()
//                terms.add(aPkg)
//                //may cause problems
//                userPkgs[entry.key]!!.remove()
//                countPkgs--
//            }
//            else {
//                var aPkg = APackage()
//                terms.add(aPkg)
//            }
//        }
//
//        var temp = ByteArray(2)
//        for (i in 0..AUDIOSZ-1 step 2) {
//            var mixedSample: Short = 0
//            for (q in 0..terms.size-1) {
//                temp[0] = terms[q].audio[i]
//                temp[1] = terms[q].audio[i+1]
//                mixedSample = (mixedSample + readShort(temp, 0)).toShort()
//            }
//            var tempByteArray = shortToByteArray(mixedSample)
//            mixedAudio[i] = tempByteArray[0]
//            mixedAudio[i+1] = tempByteArray[1]
//        }
//    }

//    fun mix(mixedAudio: ByteArray, audioSize: Int) {
//        // Очищаем mixedAudio
//        mixedAudio.fill(0)
//
//        // Собираем пакеты для микширования
//        val terms = mutableListOf<APackage>()
//        userPkgs.forEach { entry ->
//            if (entry.value.isNotEmpty()) {
//                val aPkg = entry.value.peek()
//                terms.add(aPkg)
//                Log.i("TALK_MIX", "APackage has been added to terms")
//                userPkgs[entry.key]!!.remove()
//                countPkgs--
//            } else {
//                terms.add(APackage()) // Добавляем пустой пакет, если очередь пуста
//            }
//        }
//
//        // Микшируем семплы
//        val temp = ByteArray(2)
//        for (i in 0 until audioSize step 2) {
//            var mixedSample = 0
//            for (q in terms.indices) {
//                temp[0] = terms[q].audio[i]
//                temp[1] = terms[q].audio[i + 1]
//                mixedSample += readShort(temp, 0).toInt() // Преобразуем в Int для избежания переполнения
//            }
//
//            // Ограничиваем значение семпла в диапазоне Short
//            mixedSample = mixedSample.coerceIn(-32768, 32767)
//
//            // Преобразуем обратно в ByteArray и записываем в mixedAudio
//            val tempByteArray = shortToByteArray(mixedSample.toShort())
//            mixedAudio[i] = tempByteArray[0]
//            mixedAudio[i + 1] = tempByteArray[1]
//        }
//    }

    fun increaseVolume(audioData: ByteArray, volumeGain: Float): ByteArray {
        val buffer = ByteArray(audioData.size)
        val shortBuffer = ShortArray(audioData.size / 2)

        // Преобразуем ByteArray в ShortArray (little-endian)
        for (i in shortBuffer.indices) {
            val byte1 = audioData[i * 2].toInt() and 0xFF // Младший байт
            val byte2 = audioData[i * 2 + 1].toInt() and 0xFF // Старший байт
            shortBuffer[i] = ((byte2 shl 8) or byte1).toShort() // Объединяем байты в short
        }

        // Увеличиваем громкость
        for (i in shortBuffer.indices) {
            var sample = (shortBuffer[i] * volumeGain).toInt()

            // Проверяем, чтобы значение не вышло за пределы
            sample = when {
                sample > Short.MAX_VALUE.toInt() -> Short.MAX_VALUE.toInt()
                sample < Short.MIN_VALUE.toInt() -> Short.MIN_VALUE.toInt()
                else -> sample
            }

            shortBuffer[i] = sample.toShort()
        }

        // Преобразуем ShortArray обратно в ByteArray (little-endian)
        for (i in shortBuffer.indices) {
            buffer[i * 2] = (shortBuffer[i].toInt() and 0xFF).toByte() // Младший байт
            buffer[i * 2 + 1] = ((shortBuffer[i].toInt() shr 8) and 0xFF).toByte() // Старший байт
        }

        return buffer
    }

    fun getTime(aPkg: APackage): Long{
        // возвращает время отправки пакета с другого конца
        // парметры: APackage (В котором содержится время отправки)

        //change!!!
        //may be a problem
        var ms = aPkg.time
        return ms
    }

    fun setTime(aPkg: APackage){
        // нужно понять передаётся ли APackage по ссылки или по значению в Kotlin (Нужно по ссылке)
        // устанавливает время отправки в APackage
        // парметры: APackage (В который устанавливается текущее время)
        var st = currCh.timeInfo.conTime + SystemClock.uptimeMillis() - currCh.timeInfo.cpuTime
        //may be a problem
        aPkg.time = st
    }

    fun fillAPkg(aPkg: APackage, size: Int) {

        aPkg.type = T_UDP
        aPkg.subtype = ST_AUDIO
        //might be a problem
        aPkg.sizeAudio = size.toUShort()
        aPkg.fromUser = currUsr
        setTime(aPkg)
        //("Where is num changing?")

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

    //may be a problem
//    fun readShort(buffer: ByteArray, offset: Int): Short {
//        return ((0x00 shl 24) or
//                (0x00 shl 16) or
//                (buffer[offset + 0].toInt() and 0xff shl 8) or
//                (buffer[offset + 1].toInt() and 0xff)).toShort()
//    }
//
//    fun shortToByteArray(value: Short): ByteArray {
//        return byteArrayOf(
//            (value.toInt() shr 8).toByte(),
//            value.toByte()
//        )
//    }

    // Функция для чтения Short из ByteArray
//    fun readShort(bytes: ByteArray, offset: Int): Short {
//        return ((bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)).toShort()
//    }

    fun readShort(bytes: ByteArray, offset: Int): Short {
        return ((bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)).toShort()
    }

    // Функция для преобразования Short в ByteArray
    fun shortToByteArray(value: Short): ByteArray {
        return byteArrayOf(
            (value.toInt() and 0xFF).toByte(),
            ((value.toInt() shr 8) and 0xFF).toByte()
        )
    }

    fun byteArrayToShortArray(byteArray: ByteArray): ShortArray {
        // Проверяем, что длина массива байтов чётная
        //require(byteArray.size % 2 == 0) { "ByteArray size must be even" }

        // Создаём массив short'ов
        val shortArray = ShortArray(byteArray.size / 2)

        // Преобразуем байты в short'ы
        for (i in shortArray.indices) {
            val byte1 = byteArray[i * 2].toInt() and 0xFF // Младший байт
            val byte2 = byteArray[i * 2 + 1].toInt() and 0xFF // Старший байт
            shortArray[i] = ((byte2 shl 8) or byte1).toShort()
        }

        return shortArray
    }

//    fun codecRelease() {
//        opus.encoderRelease()
//        opus.decoderRelease()
//        //Log.i("")
//    }

}