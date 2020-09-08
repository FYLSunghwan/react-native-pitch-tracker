package com.reactnativepitchtracker

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.facebook.react.bridge.Arguments;

class PitchTrackerModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    private var context:ReactApplicationContext = reactContext

    override fun getName(): String {
        return "PitchTracker"
    }

    private fun sendEvent(withName: String, body: WritableMap?) {
        this.context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java).emit(withName, body)
    }

    @ReactMethod
    fun start() {
        print("Kotlin> start()");
        var body: WritableMap = Arguments.createMap()
        body.putInt("midiNum", 3)
        sendEvent("NoteOn", body)
    }

    @ReactMethod
    fun stop() {
        print("Kotlin> stop()")
    }

    @ReactMethod
    fun prepare() {
        print("Kotlin> prepare()")
    }
}
