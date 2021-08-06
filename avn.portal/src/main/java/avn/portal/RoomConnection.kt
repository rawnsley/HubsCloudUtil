package avn.portal

import org.phoenixframework.Channel
import org.phoenixframework.Payload
import java.io.Closeable
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.random.nextULong

class RoomConnection(private val hubId: String, private val reticulumServer: ReticulumConnection, private val agentName: String) : Closeable {

    companion object {

        fun createNetworkId(): String {
            return Random.nextULong().toString(36).padStart(7, '0').substring(0, 7)
        }

        // NOTE: Not checking signatures currently to avoid additional library dependencies
        fun jwtDecode(jwt: String): String {
            // JWT is a base64 encoded triplet: <algorithm>.<body>.<signatures>
            val splitString = jwt.split('.')
            val decodedBytes = java.util.Base64.getDecoder().decode(splitString[1])
            return String(decodedBytes, Charsets.UTF_8)
        }
    }


    internal var hubInfo : RoomConnectionHubInfo? = null

    // Reticulum channel (general comms channel for the room)
    private var retChannel : Channel? = null
    // Player movement and other NAF telemetry
    private var hubChannel : Channel? = null


    // Reticulum ID of this user within the current session
    var sessionId : String? = null

    // JWT encoded permissions token (sent from Reticulum and used to authenticate with Janus)
    var permissionsTokenJwt : String? = null

    // UNUSED
    private var vapidPublicKey : String? = null

    var readyState = ReticulumConnectionState.CLOSED
        private set(value) {
            if(field != value) {
                field = value
            }
        }

    private var isJoined = false

    // Aquired when the channels are connected
    val retConnectionMutex = Semaphore(1)
    val hubConnectionMutex = Semaphore(1)

    init {
        Log.info("RoomConnection $hubId opening channel to room...")

        if(readyState != ReticulumConnectionState.CLOSED) {
            Log.warn("RoomConnection $hubId opening room when state is $readyState")
        }
        readyState = ReticulumConnectionState.OPENING

        if(reticulumServer.readyState != ReticulumConnectionState.OPEN) {
            throw Exception("Reticulum server not open for $hubId")
        }

        val socket = reticulumServer.phoenixSocket
        if(socket != null) {

            // Mutex to sequence join operation
            val joinMutex = Semaphore(1)

            // Ret channel first

            joinMutex.acquire()

            var retResponse : Map<*, *>? = null
            val newRetChannel = socket.channel("ret", hashMapOf("hub_id" to hubId))
            retChannel = newRetChannel
            newRetChannel.onClose {
                Log.info("RoomConnection $hubId ret channel closed")
                retConnectionMutex.release()
            }
            newRetChannel.onError { m ->
                Log.error("RoomConnection $hubId ret channel error: ${m.payload}")
                retConnectionMutex.release()
            }
            newRetChannel.join()
                .receive("ok") { m ->
                    Log.info("RoomConnection $hubId ret channel joined: ${m.topic} ${m.event} ${m.status}")
                    // Example response
                    //        "response":{
                    //            "session_id":"aaaaaa-bbbb-cccc-dddd-eeeeeeeee",
                    //            "vapid_public_key":"seiorghuesriguershgiesrughersiguhesrgirpesuhgriu"
                    //        }
                    retResponse = m.payload["response"] as Map<*, *>
                    retConnectionMutex.acquire()
                    joinMutex.release()
                }
                .receive("error") {
                    Log.error("RoomConnection $hubId ret channel join error: $it")
                    joinMutex.release()
                }

            Log.info("RoomConnection $hubId waiting to join ret channel...")
            if(!joinMutex.tryAcquire(10, TimeUnit.SECONDS)) {
                throw Exception("Failed to join ret channel for $hubId")
            }

            if(retResponse == null) {
                throw Exception("Hub channel cannot continue (no ret response) for $hubId")
            }

            // Get session ID for this room
            sessionId = retResponse!!["session_id"].toString()
            Log.info("sessionId = $sessionId")

            vapidPublicKey = retResponse!!["vapid_public_key"].toString()
            Log.info("vapidPublicKey = $vapidPublicKey")

            // Now join the hub channel

            var hubResponse : Map<*, *>? = null
            val params = HashMap<String, Any>()
            params["context"] = hashMapOf("mobile" to false, "embed" to false)
            params["profile"] = hashMapOf("avatarId" to createNetworkId(), "displayName" to agentName)

            params["auth_token"] = reticulumServer.authToken

            val newHubChannel = socket.channel("hub:$hubId", params)
            hubChannel = newHubChannel
            newHubChannel.onClose() {
                Log.info("RoomConnection $hubId hub channel closed")
                hubConnectionMutex.release()
            }
            newHubChannel.onError() { m ->
                Log.error("RoomConnection $hubId hub channel error ${m.payload}")
                hubConnectionMutex.release()
            }
            Log.info("Joining hub channel ${newHubChannel.topic}...")
            newHubChannel.join()
                .receive("ok") { m ->
                    Log.info("RoomConnection $hubId hub channel joined: ${m.topic} ${m.event} ${m.status}")
                    hubResponse = m.payload["response"] as Map<*, *>
                    hubConnectionMutex.acquire()
                    joinMutex.release()
                }
                .receive("error") { m ->
                    Log.error("RoomConnection $hubId hub channel join error: ${m.payload}")
                    joinMutex.release()
                }

            Log.info("RoomConnection $hubId waiting to join hub channel...")
            if(!joinMutex.tryAcquire(10, TimeUnit.SECONDS)) {
                throw Exception("Failed to join hub channel $hubId")
            }

            if(hubResponse == null) {
                throw Exception("Hub channel cannot continue (no hub response) for $hubId")
            }

            // session_token is JWT encoded
            Log.info("session_token : ${hubResponse!!["session_token"]}")

            // perms_token is JWT encoded
            val permissionsTokenJwt = hubResponse?.get("perms_token")?.toString()
            if(permissionsTokenJwt != null) {
                val permissionsTokenJson = jwtDecode(permissionsTokenJwt)
                Log.info("RoomConnection $hubId permissions decoded: $permissionsTokenJson")
            }

            // Decode hubInfo info
            val hubsJson = hubResponse!!["hubs"] as ArrayList<*>
            Log.info("RoomConnection $hubId hubs info: $hubsJson")

            readyState = ReticulumConnectionState.OPEN

            isJoined = true

        } else {
            throw Exception("ReticulumServer socket not available for $hubId")
        }
    }

    fun sendMessage(event: String, payload: Payload) {
        hubChannel?.push(event, payload)
    }

    override fun close() {
        Log.info("Leaving room ${hubId}...")
        if(readyState != ReticulumConnectionState.OPEN) {
            Log.warn("RoomConnection $hubId closing room when state is $readyState")
        }
        readyState = ReticulumConnectionState.CLOSING
        hubChannel?.let {
            Log.info("RoomConnection $hubId leaving hubPhxChannel...")
            it.leave()
            hubChannel = null
            if(!hubConnectionMutex.tryAcquire(10, TimeUnit.SECONDS)) {
                throw Exception("Failed to leave hub channel for $hubId")
            }
        }
        retChannel?.let {
            Log.info("RoomConnection $hubId leaving retPhxChannel...")
            it.leave()
            retChannel = null
            if(!retConnectionMutex.tryAcquire(10, TimeUnit.SECONDS)) {
                throw Exception("Failed to leave ret channel for $hubId")
            }
        }
        isJoined = false
        readyState = ReticulumConnectionState.CLOSED
        Log.info("Left room ${hubId}")
    }

    protected fun finalize() {
        if(isJoined) {
            Log.warn("RoomConnection $hubId not closed (finalize trap)")
            close()
        }
    }

}