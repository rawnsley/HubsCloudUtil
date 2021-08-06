package avn.portal

import okhttp3.OkHttpClient
import okhttp3.Request
import org.phoenixframework.Defaults
import org.phoenixframework.Socket
import java.io.Closeable
import java.net.URL
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

class ReticulumConnection(private val serverUrl : URL, val authToken : String) : Closeable {

    companion object {
        private val httpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        }
    }

    private val metadataUrl = URL(serverUrl, "api/v1/meta")
    private val mediaURL = URL("$serverUrl/api/v1/media")

    var readyState = ReticulumConnectionState.CLOSED

    var phoenixSocket : Socket? = null; private set

    init {
        Log.info("ReticulumConnection $serverUrl connecting...")
        val request = Request.Builder().url(metadataUrl).build()
        httpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    // Example response: {"version":"1.0.20191126041226","pool":"earth","phx_port":"4000","phx_host":"heuristic-werewolf.reticulum.io"}
                    val metadata = Defaults.gson.fromJson(responseBody, ReticulumConnectionMetadata::class.java)

                    Log.info("ReticulumConnection $serverUrl metadata: $metadata")

                    val mutex = Semaphore(1)
                    mutex.acquire()

                    // Establish generic phoenix server connection

                    // The VSN parameter is sent in the web client, but doesn't work here for some reason. Possibly set by the phoenix library?
                    val phoenixSocket = Socket(url = "wss://${metadata.phx_host}/socket/websocket", client = httpClient)
                    phoenixSocket.logger = {
                        Log.info("ReticulumConnection $serverUrl socket: $it")
                    }
                    phoenixSocket.onOpen {
                        Log.info("ReticulumConnection $serverUrl socket opened")
                        if (this.phoenixSocket != null) {
                            Log.warn("ReticulumConnection $serverUrl unexpected non-null phoenixSocket")
                        }
                        this.phoenixSocket = phoenixSocket
                        readyState = ReticulumConnectionState.OPEN
                        mutex.release()
                    }
                    phoenixSocket.onClose {
                        Log.info("ReticulumConnection $serverUrl socket closed")
                        if (this.phoenixSocket == null) {
                            Log.warn("ReticulumConnection $serverUrl unexpected null phoenixSocket")
                        }
                        this.phoenixSocket = null
                        readyState = ReticulumConnectionState.CLOSED
                    }
                    phoenixSocket.onError { throwable, r ->
                        Log.error("ReticulumConnection $serverUrl socket error: ${throwable.message} ${r?.message}")
                        // The only obvious paths to onError imply the socket is closed
                        readyState = ReticulumConnectionState.CLOSED
                        mutex.release()
                    }

                    Log.info("ReticulumConnection $serverUrl connecting at endpoint ${phoenixSocket.endpoint}")
                    phoenixSocket.connect()

                    Log.info("ReticulumConnection $serverUrl waiting for phoenix socket connection...")
                    if(!mutex.tryAcquire(10, TimeUnit.SECONDS)) {
                        throw Exception("Timeout when connecting to reticulum server $serverUrl")
                    }

                    if(this.phoenixSocket != null) {
                        Log.info("ReticulumConnection $serverUrl phoenix channel connected")
                    } else {
                        throw Exception("Failed to connect to reticulum server $serverUrl")
                    }

                } else {
                    Log.error("ReticulumConnection $serverUrl unexpected null response body from $metadataUrl")
                }
            }
        }
    }

    override fun close() {
        Log.info("ReticulumConnection $serverUrl closing...")
        phoenixSocket?.disconnect(reason = "Client is closing")
        phoenixSocket = null
        Log.info("ReticulumConnection $serverUrl closed")
    }

    protected fun finalize() {
        if(phoenixSocket != null) {
            Log.warn("ReticulumConnection $serverUrl not closed (finalize trap)")
            close()
        }
    }

}

//    Authenticated Login
//
//    ["1","1","ret","phx_join",{"hub_id":"rXdWGK8"}]	47
//    ["1","1","ret","phx_reply",{"response":{"session_id":"xxxxxxxx-uuuuuu-ssss-aaaa-drfgergerger","vapid_public_key":"ewrgifjerogejrigop;eirjgerojgi"},"status":"ok"}]	217
//
//    ["2","2","hub:rXdWGK8","phx_join",{"profile":{"avatarId":"eroifgjr","displayName":"Pellinore"},"push_subscription_endpoint":null,"auth_token":"eorpgijergoiejrgoerijg.rstogijersogijesrgoijesrg","perms_token":null,"context":{"mobile":false,"embed":false},"hub_invite_id":null}]	568
//    [   "2","2","hub:rXdWGK8","phx_reply",{"response":{"hub_requires_oauth":false,"hubs":[{"allow_promotion":false,"description":null,"entry_code":974141,"entry_mode":"allow","host":"lucid-ardent.reticulum.io","hub_id":"rXdWGK8","lobby_count":0,"member_count":0,"member_permissions":{"fly":true,"pin_objects":true,"spawn_and_move_media":true,"spawn_camera":true,"spawn_drawing":true,"spawn_emoji":true},"name":"SOME NAME","port":443,"room_size":24,"slug":"some-name","topics":[{"assets":[{"asset_type":"gltf_bundle","src":"https://someurl"}],"janus_room_id":3048tu340t934,"topic_id":"rXdWGK8/some-name"}],"turn":{"credential":"wopgijeropgij=","enabled":true,"transports":[{"port":80}],"username":"34958734958347:coturn"},"user_data":null}],"perms_token":"dgrsgergergerg.ergaergergaerg.ergergerger-ergerger-d-f-enHaPlqSn---egrergegr-eagerwger-egerger","session_id":"1e7b6ergegrerc6f-2328-430b-934d-ergerg","session_token":"ergerger.gergergegr.ergergegr-erergerg","subscriptions":{"favorites":false,"web_push":null}},"status":"ok"}]	2556
//
//    [null,null,"hub:rXdWGK8","presence_diff",{"joins":{"1e7b6c6f-2328-430b-934d-e3b0d36b5146":{"metas":[{"context":{"embed":false,"mobile":false},"permissions":{"close_hub":true,"embed_hub":true,"fly":true,"join_hub":true,"kick_users":true,"mute_users":true,"pin_objects":true,"spawn_and_move_media":true,"spawn_camera":true,"spawn_drawing":true,"spawn_emoji":true,"update_hub":true,"update_hub_promotion":false,"update_roles":true},"phx_ref":"I+wLzj9Eu+4=","presence":"lobby","profile":{"avatarId":"5XpdBeF","displayName":"Pellinore"},"roles":{"creator":true,"owner":true,"signed_in":true}}]}},"leaves":{}}]	604
//    [null,null,"hub:rXdWGK8","presence_state",{"1e7b6c6f-2328-430b-934d-e3b0d36b5146":{"metas":[{"context":{"embed":false,"mobile":false},"permissions":{"close_hub":true,"embed_hub":true,"fly":true,"join_hub":true,"kick_users":true,"mute_users":true,"pin_objects":true,"spawn_and_move_media":true,"spawn_camera":true,"spawn_drawing":true,"spawn_emoji":true,"update_hub":true,"update_hub_promotion":false,"update_roles":true},"phx_ref":"I+wLzj9Eu+4=","presence":"lobby","profile":{"avatarId":"5XpdBeF","displayName":"Pellinore"},"roles":{"creator":true,"owner":true,"signed_in":true}}]}}]	583
//
//    ["2","3","hub:rXdWGK8","get_host",null]	39
//    ["2","3","hub:rXdWGK8","phx_reply",{"response":{"host":"lucid-ardent.reticulum.io","port":443,"turn":{"credential":"egergergerger=","enabled":true,"transports":[{"port":80}],"username":"453345:coturn"}},"status":"ok"}]	236
//
//    [null,"4","phoenix","heartbeat",{}]	35
//    [null,"4","phoenix","phx_reply",{"response":{},"status":"ok"}]
//
//
//    Scene Change:
//
//    ["2","6","hub:8a8F3jE","update_scene",{"url":"https://someurl"}]
//    [null,null,"hub:8a8F3jE","hub_refresh",{"hubs":[{"allow_promotion":false,"description":null,"entry_code":45645456,"entry_mode":"allow","host":"lucid-ardent.reticulum.io","hub_id":"34344d","lobby_count":1,"member_count":0,"member_permissions":{"fly":true,"pin_objects":true,"spawn_and_move_media":true,"spawn_camera":true,"spawn_drawing":true,"spawn_emoji":true},"name":"Stimulating Honored Tract","port":443,"room_size":24,"slug":"stimulating-honored-tract","topics":[{"assets":[{"asset_type":"gltf_bundle","src":"https://someurl"}],"janus_room_id":3453534345,"topic_id":"8a8F3jE/stimulating-honored-tract"}],"turn":{"credential":"wfefewewfwef=","enabled":true,"transports":[{"port":80}],"username":"234234234:coturn"},"user_data":null}],"session_id":"2323423-7b2f-23243-9d16-e09a7b39dce2","stale_fields":["scene"]}]
