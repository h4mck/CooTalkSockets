package com.example.cootalksockets

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder.AudioSource
import android.util.Log
import androidx.compose.ui.text.toLowerCase
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.theeasiestway.opus.Constants
import java.net.Socket
import java.util.concurrent.Executors
import java.util.Queue
import java.util.LinkedList
import com.theeasiestway.opus.Opus

class Client(): ViewModel() {

    val opus = Opus()
    //val callerName: MutableLiveData<String?> = MutableLiveData(null)

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
            //val socket = Socket("89.111.173.78", 12345)
            //socket.outputStream.write(name.toByteArray())

            //write signals from server to queue
//            Executors.newSingleThreadExecutor().execute() {
//                while (true) {
//                    socket.inputStream.read(answer, 0, 97)
//                    signalsQueue.add(answer)
//                    answer = ByteArray(97)
//                }
//            }

            //handle singnals from queue
            Executors.newSingleThreadExecutor().execute() {
                while (true) {
                    if (signalsQueue.isEmpty())
                        continue
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

    @SuppressLint("MissingPermission")
    fun soundCheck() {

        val sampleRate = 24000
        //val frameLength = 512
        val bufferSize = 3840
        val bufferSize2 = 1920
        var audioRecord: AudioRecord? = null
        var recordBuffer = ByteArray(bufferSize)
        var trackBuffer: ByteArray
        var encodedAudio: ByteArray

        setupOpus()

        //get rid of
        //var fileName = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/audio" + ".pcm"



        Log.i("CLIENT", "Minbuff is: ${AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT)}")
        audioRecord = AudioRecord(
            AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        //audioRecord.startRecording()

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

        audioRecord.startRecording()
        audioTrack.play()
        Executors.newSingleThreadExecutor().execute() {
            while (true) {
                audioRecord.read(recordBuffer, 0, bufferSize)

                encodedAudio = opus.encode(recordBuffer, Constants.FrameSize._960())!!
                Log.i("Client", "Length of audio in opus: ${encodedAudio.size}")

                trackBuffer = opus.decode(encodedAudio, Constants.FrameSize._960())!!
                Log.i("Client", "Length of decoded audio: ${trackBuffer.size}")

                audioTrack.write(trackBuffer, 0, trackBuffer.size)

            }
        }
    }

    fun setupOpus() {
        opus.encoderInit(Constants.SampleRate._24000(), Constants.Channels.stereo(), Constants.Application.audio())

        opus.decoderInit(Constants.SampleRate._24000(), Constants.Channels.stereo())
    }

    fun codecRelease() {
        opus.encoderRelease()
        opus.decoderRelease()
    }

}