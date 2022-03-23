package avn.portal

import okhttp3.*
import org.junit.Test

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ScratchTest {

    companion object {
        const val RETICULUM_URL = "wss://focused-giant.reticulum.io/socket/websocket"
    }

    @Test(timeout = 10000)
    fun scratch() {
        Log.info("Test started")
        val httpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        val request = Request.Builder().url(RETICULUM_URL).build()

        val openLatch = CountDownLatch(1)
        val closeLatch = CountDownLatch(1)

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.info("Socket is open")
                openLatch.countDown()
                super.onOpen(webSocket, response)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.info("Socket is closed")
                closeLatch.countDown()
                super.onClosed(webSocket, code, reason)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.info("Socket failure: ${t.message}")
                openLatch.countDown()
                closeLatch.countDown()
                super.onFailure(webSocket, t, response)
            }
        }
        Log.info("Opening socket...")
        val connection = httpClient.newWebSocket(request, listener)
        assert(openLatch.await(10, TimeUnit.SECONDS))
        Log.info("Closing socket...")
        connection.close(1000, "TEST_CLOSE")
        assert(closeLatch.await(10, TimeUnit.SECONDS))
        Log.info("Test complete")
    }


}