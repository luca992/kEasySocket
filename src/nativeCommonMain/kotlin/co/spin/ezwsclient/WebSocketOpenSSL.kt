package co.spin.ezwsclient

import co.spin.utils.INVALID_SOCKET
import co.spin.utils.Log
import co.spin.Url
import co.spin.utils.SOCKET_EWOULDBLOCK
import kotlinx.cinterop.*
import platform.posix.*
import openssl.*

class WebSocketOpenSSL(url: Url, useMask : Boolean) : WebSocket(url, useMask) {


    var ssl_context : CPointer<SSL_CTX>? = null
    var ssl_connection : CPointer<SSL>? = null
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

    /* create new SSL connection state object */
    fun openssl_create_connection(ctx : CPointer<SSL_CTX>?, socket : Int) : CPointer<SSL>?
    {
        if (ctx == null) throw RuntimeException("ctx should not be null")
        if (socket < 1) throw RuntimeException("socket: $socket not valid")

        val ssl = SSL_new(ctx)
        if (ssl != null)
            SSL_set_fd(ssl, socket)
        return ssl
    }

    fun openSSLHandshake(host: String) : Boolean {
        while (true)
        {
            if (ssl_connection == null || ssl_context == null)
            {
                return false
            }

            /*SSL_set_verify(
                ssl_connection,
                SSL_VERIFY_PEER or SSL_VERIFY_FAIL_IF_NO_PEER_CERT,
                staticCFunction { i: Int, j: CPointer<X509_STORE_CTX>? ->
                    return@staticCFunction 1
                }
            )*/

            ERR_clear_error()
            val connectResult = SSL_connect(ssl_connection)
            if (connectResult == 1) {
                return openSSLCheckServerCert(ssl_connection, host)
            }
            val reason = SSL_get_error(ssl_connection, connectResult)

            var rc: Boolean
            if (reason == SSL_ERROR_WANT_READ || reason == SSL_ERROR_WANT_WRITE) {
                rc = true
            } else {
                Log.error {getSSLError(connectResult)}
                rc = false
            }

            if (!rc) {
                return false
            }
        }
    }

    fun openSSLCheckServerCert(ssl: CPointer<SSL>?, hostname : String) : Boolean {
        val server_cert = SSL_get_peer_certificate(ssl)
        if (server_cert == null)
        {
            Log.error {"OpenSSL failed - peer didn't present a X509 certificate."}
            return false
        }

        X509_free(server_cert)
        return true
    }

    override fun connect(hostname : String, port : Int): Boolean {
        var handshakeSuccessful = false
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

        ERR_clear_error()
        val cert_load_result = SSL_CTX_set_default_verify_paths(ssl_context)
        if (cert_load_result == 0) {
            val ssl_err = ERR_get_error()
            Log.error {"OpenSSL failed - SSL_CTX_default_verify_paths loading failed: ${ERR_error_string(ssl_err, null)}"}
        }

        ssl_connection = openssl_create_connection(ssl_context, sockfd.toInt())
        if (null == ssl_connection)
        {
            Log.error {"OpenSSL failed to connect"}
            SSL_CTX_free(ssl_context)
            ssl_context = null
            return false
        }

        // SNI support
        SSL_ctrl(ssl_connection,SSL_CTRL_SET_TLSEXT_HOSTNAME,TLSEXT_NAMETYPE_host_name,hostname.cstr) //SSL_set_tlsext_host_name(ssl_connection, hostname)

        // Support for server name verification
        val param = SSL_get0_param(ssl_connection)
        X509_VERIFY_PARAM_set1_host(param, hostname, 0)

        handshakeSuccessful = openSSLHandshake(hostname)

        if (!handshakeSuccessful) {
            close()
            return false
        }

        return true
    }

    fun getSSLError(ret: Int) : String
    {
        var e: ULong

        val err = SSL_get_error(ssl_connection, ret)

        if (err == SSL_ERROR_WANT_CONNECT || err == SSL_ERROR_WANT_ACCEPT)
        {
            return "OpenSSL failed - connection failure";
        }
        else if (err == SSL_ERROR_WANT_X509_LOOKUP)
        {
            return "OpenSSL failed - x509 error";
        }
        else if (err == SSL_ERROR_SYSCALL)
        {
            e = ERR_get_error()
            return if (e > 0uL) {
                "OpenSSL failed - ${ERR_error_string(e, null)?.toKString()}"
            } else if (e == 0uL && ret == 0) {
                "OpenSSL failed - received early EOF";
            } else {
                "OpenSSL failed - underlying BIO reported an I/O error"
            }
        }
        else if (err == SSL_ERROR_SSL)
        {
            e = ERR_get_error()
            return "OpenSSL failed - SSL_ERROR_SSL ${ERR_error_string(e, null)?.toKString()}"
        }
        else if (err == SSL_ERROR_NONE)
        {
            return "OpenSSL failed - err none"
        }
        else if (err == SSL_ERROR_ZERO_RETURN)
        {
            return "OpenSSL failed - err zero return"
        }
        else
        {
            return "OpenSSL failed - unknown error"
        }
    }

    override fun close() {
        if (ssl_connection != null) {
            SSL_free(ssl_connection);
            ssl_connection = null
        }
        if (ssl_context != null)
        {
            SSL_CTX_free(ssl_context);
            ssl_context = null
        }
        super.close()
    }

    override fun send(bufPtr: CPointer<UByteVar>?, len: ULong) : Long {
        val message = (bufPtr as? CPointer<ByteVar>?)?.toKString()
        //Log.debug{"Sending: ${message?.trim()}"}

        var nbyte = len.toInt()
        var sent: Long = 0

        while (nbyte > 0) {

            if (ssl_connection == null || ssl_context == null) {
                return 0
            }

            ERR_clear_error()
            val write_result = SSL_write(ssl_connection, bufPtr + sent, nbyte)
            val reason = SSL_get_error(ssl_connection, write_result)

            if (reason == SSL_ERROR_NONE) {
                nbyte -= write_result
                sent += write_result
            } else if (reason == SSL_ERROR_WANT_READ || reason == SSL_ERROR_WANT_WRITE) {
                set_posix_errno(SOCKET_EWOULDBLOCK)
                return -1
            } else if (reason == SSL_ERROR_SYSCALL) {
                val e = ERR_get_error()
                val r = if (e > 0uL) {
                    "OpenSSL failed - ${ERR_error_string(e, null)?.toKString()}"
                } else if (e == 0uL && reason == 0) {
                    "OpenSSL failed - received early EOF";
                } else {
                    "OpenSSL failed - underlying BIO reported an I/O error"
                }
                Log.debug { r }
                return -1
            } else {
                return -1
            }
        }
        return sent
    }

    override fun recv(buf: CPointer<UByteVar>?, len: ULong) : Long {
        var nbyte = len.toInt()
        while (true) {
            if (ssl_connection == null || ssl_context === null) {
                return 0
            }

            ERR_clear_error()
            val read_result = SSL_read(ssl_connection, buf, nbyte)

            if (read_result > 0) {
                return read_result.toLong()
            }

            val reason = SSL_get_error(ssl_connection, read_result)

            if (reason == SSL_ERROR_WANT_READ || reason == SSL_ERROR_WANT_WRITE) {
                set_posix_errno(SOCKET_EWOULDBLOCK)
                return -1
            } else {
                return -1
            }
        }
    }

}

fun openssl_verify_callback(preverify: Int, x509_ctx: CPointer<X509_STORE_CTX>?) : Int
{
    return preverify
}