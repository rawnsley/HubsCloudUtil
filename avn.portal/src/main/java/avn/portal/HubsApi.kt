package avn.portal

import org.phoenixframework.Payload
import java.net.URL

class HubsApi {
    companion object {
        @JvmStatic fun sendMessage(serverUrl : URL, authToken : String, hubId: String, event: String, payload: Payload, agentName: String) {
            ReticulumConnection(serverUrl, authToken).use { reticulumConnection ->
                RoomConnection(hubId, reticulumConnection, agentName).use { roomConnection ->
                    roomConnection.sendMessage(event, payload)
                }
            }
        }
    }
}