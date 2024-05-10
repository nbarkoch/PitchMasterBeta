package com.example.pitchmasterbeta.bridge

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface INativeWaveBridge {
    suspend fun play()
    suspend fun pause()
    suspend fun stop()
    suspend fun isPlaying(): Boolean
}

class NativeWaveBridge: INativeWaveBridge, DefaultLifecycleObserver {
    // native refs
    private var bridgeNativeHandle: Long = 0L // will contain the address to the native bridge
    private val connectionMutex = Object()

    // main methods
    private external fun create(): Long
    private external fun delete(bridgeNativeHandle: Long)
    private external fun play(bridgeNativeHandle: Long)
    private external fun isPlaying(bridgeNativeHandle: Long): Boolean
    private external fun pause(bridgeNativeHandle: Long)
    private external fun stop(bridgeNativeHandle: Long)
    private external fun skipTo(bridgeNativeHandle: Long, timestamp: Double)
    companion object {
        private const val LIBRARY_NAME = "pitchmaster"
        init {
            System.loadLibrary(LIBRARY_NAME)
        }
    }

    // feature native methods

    /**
     *
     * */
    private external fun setPitchHeight(bridgeNativeHandle: Long, factor: Float)

    /**
     *
     * */
    private external fun setVolume(bridgeNativeHandle: Long, factor: Float)

    /**
     *
     * */
    private external fun setMicEcho(bridgeNativeHandle: Long, active: Boolean)


    // Lifecycle Observer
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
    }
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        synchronized(connectionMutex) {
            createBridgeIfNotExits()
        }
    }
    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
    }
    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
    }
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
    }
    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        synchronized(connectionMutex) {
            deleteBridgeIfExits()
        }
    }

    // helper functions
    private fun createBridgeIfNotExits() {
        // create if not exists
        if (bridgeNativeHandle != 0L) {
            return
        }
        bridgeNativeHandle = create()
    }

    private fun deleteBridgeIfExits() {
        // create if not exists
        if (bridgeNativeHandle == 0L) {
            return
        }
        delete(bridgeNativeHandle)
        bridgeNativeHandle = 0L
    }

    // bridge methods

    override suspend fun play() = withContext(Dispatchers.IO) {
        synchronized(connectionMutex) {
            createBridgeIfNotExits()
            play(bridgeNativeHandle)
        }
    }

    override suspend fun pause() = withContext(Dispatchers.IO) {
        synchronized(connectionMutex) {
            createBridgeIfNotExits()
            pause(bridgeNativeHandle)
        }
    }

    override suspend fun stop() = withContext(Dispatchers.IO) {
        synchronized(connectionMutex) {
            createBridgeIfNotExits()
            stop(bridgeNativeHandle)
        }
    }

    override suspend fun isPlaying() = withContext(Dispatchers.IO) {
        synchronized(connectionMutex) {
            createBridgeIfNotExits()
            return@withContext isPlaying(bridgeNativeHandle)
        }
    }


}