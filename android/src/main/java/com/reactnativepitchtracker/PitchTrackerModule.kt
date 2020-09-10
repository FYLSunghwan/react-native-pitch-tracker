package com.reactnativepitchtracker

import android.Manifest
import android.content.res.AssetManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Process
import android.util.Log
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.facebook.react.bridge.Arguments;
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.HashMap

class PitchTrackerModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    private var context:ReactApplicationContext = reactContext

    private val SAMPLE_RATE = 16000
    private val RECORDING_LENGTH = 17920 //(SAMPLE_RATE * SAMPLE_DURATION_MS / 1000)
    private val MINIMUM_TIME_BETWEEN_SAMPLES_MS: Long = 30
    private val MODEL_FILENAME = "file:///android_asset/onsets_frames_wavinput.tflite"

    // Working variables.
    var recordingBuffer = ShortArray(RECORDING_LENGTH)
    var recordingOffset = 0
    var shouldContinue = true
    private var recordingThread: Thread? = null
    var shouldContinueRecognition = true
    private var recognitionThread: Thread? = null
    private val recordingBufferLock: ReentrantLock = ReentrantLock()

    private var tfLite: Interpreter? = null

    private val LOG_TAG: String = "PITCHTRACKER"

    override fun getName(): String {
        return "PitchTracker"
    }

    private fun sendEvent(withName: String, body: WritableMap?) {
        this.context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java).emit(withName, body)
    }

    @ReactMethod
    fun start() {
        startRecording()
        startRecognition()
        Log.v(LOG_TAG, "Audio Engine Start")
    }

    @ReactMethod
    fun stop() {
        stopRecognition()
        stopRecording()
        Log.v(LOG_TAG, "Audio Engine Stop")
    }

    @ReactMethod
    fun prepare() {
        Log.v(LOG_TAG, "Audio Engine Prepare")

        val actualModelFilename =
                MODEL_FILENAME.split("file:///android_asset/".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()[1]

        tfLite = try {
            Interpreter(loadModelFile(context.assets, actualModelFilename)!!)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

        tfLite!!.resizeInput(0, intArrayOf(RECORDING_LENGTH, 1))
    }

    // Pitch Tracker Impl
    /** Memory-map the model file in Assets.  */
    @Throws(IOException::class)
    private fun loadModelFile(
        assets: AssetManager,
        modelFilename: String
    ): ByteBuffer? {
        val fileDescriptor = assets.openFd(modelFilename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel: FileChannel = inputStream.getChannel()
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    @Synchronized
    fun startRecording() {
        if (recordingThread != null) {
            return
        }
        shouldContinue = true
        recordingThread = Thread(
            Runnable { record() })
        recordingThread?.start()
    }

    @Synchronized
    fun stopRecording() {
        if (recordingThread == null) {
            return
        }
        shouldContinue = false
        recordingThread = null
    }

    private fun record() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)

        // Estimate the buffer size we'll need for this device.
        var bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2
        }
        val audioBuffer = ShortArray(bufferSize / 2)
        val record = AudioRecord(
                MediaRecorder.AudioSource.DEFAULT,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(LOG_TAG, "Audio Record can't initialize!")
            return
        }
        record.startRecording()
        Log.v(LOG_TAG, "Start recording")

        // Loop, gathering audio data and copying it to a round-robin buffer.
        while (shouldContinue) {
            val numberRead = record.read(audioBuffer, 0, audioBuffer.size)
            val maxLength: Int = recordingBuffer.size
            val newRecordingOffset = recordingOffset + numberRead
            val secondCopyLength = Math.max(0, newRecordingOffset - maxLength)
            val firstCopyLength = numberRead - secondCopyLength
            // We store off all the data for the recognition thread to access. The ML
            // thread will copy out of this buffer into its own, while holding the
            // lock, so this should be thread safe.
            recordingBufferLock.lock()
            recordingOffset = try {
                System.arraycopy(
                        audioBuffer,
                        0,
                        recordingBuffer,
                        recordingOffset,
                        firstCopyLength
                )
                System.arraycopy(
                        audioBuffer,
                        firstCopyLength,
                        recordingBuffer,
                        0,
                        secondCopyLength
                )
                newRecordingOffset % maxLength
            } finally {
                recordingBufferLock.unlock()
            }
        }
        record.stop()
        record.release()
    }

    @Synchronized
    fun startRecognition() {
        if (recognitionThread != null) {
            return
        }
        shouldContinueRecognition = true
        recognitionThread = Thread(
                Runnable { recognize() })
        recognitionThread?.start()
    }

    @Synchronized
    fun stopRecognition() {
        if (recognitionThread == null) {
            return
        }
        shouldContinueRecognition = false
        recognitionThread = null
    }


    private fun recognize() {
        Log.v(LOG_TAG, "Start recognition")
        val inputBuffer = ShortArray(RECORDING_LENGTH)
        val floatInputBuffer =
                Array(RECORDING_LENGTH) { FloatArray(1) }
        val outputScores =
                Array(1) { Array(32){FloatArray(88) }}
        val prevResult = IntArray(88)

        // Loop, grabbing recorded data and running the recognition model on it.
        while (shouldContinueRecognition) {
            val startTime: Long = Date().getTime()
            // The recording thread places data in this round-robin buffer, so lock to
            // make sure there's no writing happening and then copy it to our own
            // local version.
            recordingBufferLock.lock()
            try {
                val maxLength: Int = recordingBuffer.size
                val firstCopyLength = maxLength - recordingOffset
                val secondCopyLength = recordingOffset
                System.arraycopy(
                        recordingBuffer,
                        recordingOffset,
                        inputBuffer,
                        0,
                        firstCopyLength
                )
                System.arraycopy(
                        recordingBuffer,
                        0,
                        inputBuffer,
                        firstCopyLength,
                        secondCopyLength
                )
            } finally {
                recordingBufferLock.unlock()
            }

            // We need to feed in float values between -1.0f and 1.0f, so divide the
            // signed 16-bit inputs.
            for (i in 0 until RECORDING_LENGTH) {
                floatInputBuffer[i][0] = inputBuffer[i] / 32767.0f
            }
            val inputArray =
                    arrayOf(floatInputBuffer)
            val outputMap: MutableMap<Int, Any> = HashMap()
            outputMap[0] = outputScores

            // Run the model.
            tfLite!!.runForMultipleInputsOutputs(inputArray, outputMap)

            val restemp = outputMap[0] as Array<Array<FloatArray>>

            val result = IntArray(88)

            for (i in 0 until 32) {
                for (j in 0 until 88) {
                    if(restemp[0][i][j]>0) {
                        result[j] = result[j] + 1
                    }
                }
            }

            // Send Event
            for (i in 0 until 88) {
                var body: WritableMap = Arguments.createMap()
                if(prevResult[i] == 0 && result[i] > 0) {
                    body.putInt("midiNum", i+21)
                    sendEvent("NoteOn", body)
                }
                if(prevResult[i] > 0 && result[i] == 0) {
                    body.putInt("midiNum", i+21)
                    sendEvent("NoteOff", body)
                }
                prevResult[i] = result[i]
            }

            try {
                // We don't need to run too frequently, so snooze for a bit.
                Thread.sleep(MINIMUM_TIME_BETWEEN_SAMPLES_MS)
            } catch (e: InterruptedException) {
                // Ignore
            }
        }
        Log.v(LOG_TAG, "End recognition")
    }
}
