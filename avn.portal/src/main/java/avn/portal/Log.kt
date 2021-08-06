package avn.portal

object Log {

    fun info(message: String) {
        System.err.println("[INFO] $message")
    }
    fun warn(message: String) {
        System.err.println("[WARNING] $message")
    }
    fun error(message: String) {
        System.err.println("[ERROR] $message")
    }
}