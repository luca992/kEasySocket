package co.spin.ezwsclient

import co.spin.utils.Log
import kotlinx.cinterop.*
import openssl.*

class WebSocketOpenSSL(url: Url, useMask : Boolean) : WebSocket(url, useMask) {


    var openSSLInitialized: Boolean = false
    var openSSLInitializationSuccessful: Boolean = false

    fun openSSLInitialize() : Boolean {
        if (openSSLInitialized) {
            return openSSLInitializationSuccessful
        }
        if (OPENSSL_init_ssl(OPENSSL_INIT_LOAD_CONFIG.toULong(), null) == 0) {
            Log.error {"OPENSSL_init_ssl failure"}
            openSSLInitializationSuccessful = false
            openSSLInitialized = true
            return openSSLInitializationSuccessful
        }

        OPENSSL_init_ssl(0u, null)//OpenSSL_add_ssl_algorithms()
        OPENSSL_init_ssl(OPENSSL_INIT_LOAD_SSL_STRINGS.or(OPENSSL_INIT_LOAD_CRYPTO_STRINGS).toULong(), null)//SSL_load_error_strings()
        openSSLInitializationSuccessful = true

        return openSSLInitializationSuccessful
    }

    override fun connect(hostname : String, port : Int): Boolean {

        if (!openSSLInitialize())
        {
            return false
        }




        return true
    }
}