package avn.portal

import org.junit.Assert
import org.junit.Test

import java.net.URL

class ReticulumConnectionTest {

    private val HUB_SERVER = URL("https://hubs.mozilla.com")
    private val HUB_AUTH_TOKEN = "AUTH_NOT_REQUIRED_FOR_TEST"

    @Test(timeout = 10000)
    fun openAndClose() {

        val instance = ReticulumConnection(HUB_SERVER, HUB_AUTH_TOKEN)

        Assert.assertEquals(ReticulumConnectionState.OPEN, instance.readyState)

        Log.info("Server is open")

        Log.info("Closing server...")

        instance.close()

        while (instance.readyState != ReticulumConnectionState.CLOSED) {
            Thread.sleep(100)
        }

        Log.info("Server is closed")
    }


}