package avn.portal

class RoomConnectionHubInfo(
    val id: String,
    val name: String,
    val slug: String,
    // WebRTC server host
    val host: String,
    val port: Int,
    //   val scene : SceneInfo,
    val entryMode: String,
    val entryCode: Int,
//    val topics : Array<HubTopic>
)