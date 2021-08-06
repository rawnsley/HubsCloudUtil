# ReadMe

An example library for accessing [Mozilla Hubs](https://github.com/mozilla/hubs) room via the WebSocket interface.

Library is written in Kotlin and contains some unit tests as examples of usage. For mutating operations the unit tests require updated authentication parameters.

## Room Open

This is most easily done with a HTTP POST rather than using this library as follows:

```
https://hubs.mozilla.com/api/v1/hubs
Content-Type: application/json
Authorization bearer <auth-token>

{
    "hub": {
        "name": "Some Name",
        "scene_id": "xxxxxxx"
        "default_environment_gltf_bundle_url": "Some Scene URL",
        "room_size": 30
    }
}
```

## Room Close

This can be done with the API using the _close_hub_ command and an empty payload.