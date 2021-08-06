package avn.portal

import org.junit.Test

import java.net.URL

class HubsApiTest {

    // Tokens have been issued to email account X@Y.COM
    // Generate by signing in with X@Y.COM and copying the value of browser local storage > __hubs_store > credentials > token

    private val HUB_SERVER = URL("https://hubs.mozilla.com")
    private val HUB_AUTH_TOKEN = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"

    // Gathering hall
    private val SCENE_ASSET_URL1 = URL("https://data.avncloud.com/dev/GatheringHall.glb")
    // River Island
    private val SCENE_ASSET_URL2 = URL("https://data.avncloud.com/dev/RiverIslandScene.glb")

    // Created for the test by user X@Y.COm
    private val ROOM_ID = "ABC123"

    @Test
    fun sendMessageSimple() {
        HubsApi.sendMessage(HUB_SERVER, HUB_AUTH_TOKEN, ROOM_ID, "update_hub", mapOf(), "unit-test")
    }

    @Test
    fun sceneChange1() {
        HubsApi.sendMessage(HUB_SERVER, HUB_AUTH_TOKEN, ROOM_ID, "update_scene", mapOf(Pair("url", SCENE_ASSET_URL1)), "unit-test")
    }

    @Test
    fun sceneChange2() {
        HubsApi.sendMessage(HUB_SERVER, HUB_AUTH_TOKEN, ROOM_ID, "update_scene", mapOf(Pair("url", SCENE_ASSET_URL2)), "unit-test")
    }



}