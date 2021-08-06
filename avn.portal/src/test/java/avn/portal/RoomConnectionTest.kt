package avn.portal

import org.junit.Test

import org.junit.Assert.*
import java.net.URL

class RoomConnectionTest {

    private val HUB_SERVER = URL("https://hubs.mozilla.com")
    private val HUB_AUTH_TOKEN = "AUTH_NOT_REQUIRED_FOR_TEST"

    // Created for the test
    private val ROOM_ID = "w6QJKv9"

    @Test(timeout = 10000)
    fun joinAndLeave() {

        ReticulumConnection(HUB_SERVER, HUB_AUTH_TOKEN).use { instance ->

            val room = RoomConnection(ROOM_ID, instance, "unit-test")

            assertEquals(ReticulumConnectionState.OPEN, room.readyState)

            // Required to ensure notification for other users
            Thread.sleep(1000)

            room.close()

            assertEquals(ReticulumConnectionState.CLOSED, room.readyState)
        }
    }


}