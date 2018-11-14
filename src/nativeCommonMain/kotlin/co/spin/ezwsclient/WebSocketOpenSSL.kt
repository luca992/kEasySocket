package co.spin.ezwsclient

import co.spin.utils.INVALID_SOCKET
import co.spin.utils.Log
import kotlinx.cinterop.*
import openssl.*

class WebSocketOpenSSL(url: Url, useMask : Boolean) : WebSocket(url, useMask) {


    var ssl_context : CPointer<SSL_CTX>? = null
    val ssl_connection : SSL? = null
    var ssl_method :  CValuesRef<SSL_METHOD>? = null
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




    fun openSSLCreateContext() : CPointer<SSL_CTX>?
    {
        val method = TLS_client_method()
        if (method == null)
        {
            Log.error {"SSLv23_client_method failure"}
            return null
        }
        ssl_method = method

        val ctx = SSL_CTX_new(ssl_method)
        if (ctx!=null)
        {
            // To skip verification, pass in SSL_VERIFY_NONE
            val openssl_verify_callback_static = staticCFunction(::openssl_verify_callback)
            SSL_CTX_set_verify(ctx, SSL_VERIFY_PEER, openssl_verify_callback_static)
            SSL_CTX_set_verify_depth(ctx, 4)
            SSL_CTX_set_options(ctx, SSL_OP_NO_SSLv2.toULong().or(SSL_OP_NO_SSLv3.toULong()))
        }
        return ctx
    }

    override fun connect(hostname : String, port : Int): Boolean {

        if (!openSSLInitialize())
        {
            return false
        }
        this.sockfd = hostnameConnect(hostname, port)
        if (sockfd == INVALID_SOCKET) return false

        ssl_context = openSSLCreateContext()
        if (ssl_context == null)
        {
            return false
        }



        return true
    }
}

fun openssl_verify_callback(preverify: Int, x509_ctx: CPointer<X509_STORE_CTX>?) : Int
{
    return preverify
}