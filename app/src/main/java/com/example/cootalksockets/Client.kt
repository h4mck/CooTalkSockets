package com.example.cootalksockets

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder.AudioSource
import androidx.compose.ui.text.toLowerCase
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.net.Socket
import java.util.concurrent.Executors
import java.util.Queue
import java.util.LinkedList

class Client(): ViewModel() {

    val callerName: MutableLiveData<String?> = MutableLiveData(null)

    private fun notificationHandler(signal: ByteArray?) {
        TODO("Not yet implemented")
    }

    private fun incomingCall(signal: ByteArray?) {
        TODO("Not yet implemented")

        //val caller: String = signal[33:65]

    }

    fun call() {
        TODO("Not yet implemented")
    }

    fun connect(name: String) {
        Executors.newSingleThreadExecutor().execute {



            val signalsQueue: Queue<ByteArray> = LinkedList<ByteArray>(mutableListOf())

            var answer = ByteArray(97)
            val socket = Socket("89.111.173.78", 1234)
            socket.outputStream.write(name.toByteArray())

            //write signals from server to queue
            Executors.newSingleThreadExecutor().execute() {
                while (true) {
                    socket.inputStream.read(answer, 0, 97)
                    signalsQueue.add(answer)
                    answer = ByteArray(97)
                }
            }

            //handle singnals from queue
            Executors.newSingleThreadExecutor().execute() {
                while (true) {
                    val currentSignal = signalsQueue.remove()
                    when (currentSignal[0].toString().lowercase()) {

                        "n" -> {
                            notificationHandler(currentSignal)
                            continue
                        }

                        "i" -> {
                            Executors.newSingleThreadExecutor().execute() {
                                incomingCall(currentSignal)
                            }
                            continue
                        }

                        "e" ->
                            break

                        else ->
                            continue

                    }
                }
            }

            //socket.close()
            //startRecording()
        }


    }

    fun soundCheck() {
        val executor = Executors.newSingleThreadExecutor()

        val sampleRate = 16000
        //val frameLength = 512
        val bufferSize = 960
        var audioRecord: AudioRecord? = null
        var buffer: ByteArray = ByteArray(bufferSize)

        //get rid of
        //var fileName = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/audio" + ".pcm"

        audioRecord = AudioRecord(
            AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        audioRecord.startRecording()

        val audioAttributes = AudioAttributes.Builder()
        audioAttributes.setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
        audioAttributes.setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)

        val audioFormat = AudioFormat.Builder()
        audioFormat.setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        audioFormat.setSampleRate(sampleRate)
        audioFormat.setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)

        val audioTrack = AudioTrack(
            audioAttributes.build(),
            audioFormat.build(),
            bufferSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )

        executor.execute() {
            while (true) {
                audioRecord.read(buffer, 0, bufferSize)

                audioTrack.write(buffer, 0, bufferSize)

                audioTrack.play()
            }
        }
    }


}