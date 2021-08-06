package avn.portal

class ReticulumConnectionMetadata(val version : String, val pool : String, val phx_host : String, val phx_port : String) {
    override fun toString(): String {
        return "version: $version; pool: $pool; phx_host: $phx_host' phx_port: $phx_port"
    }
}