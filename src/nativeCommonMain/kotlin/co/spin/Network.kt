package co.spin

import co.spin.utils.Log

class Network : PhxSocketDelegate() {
    // PhxSocketDelegate
    override fun phxSocketDidOpen(){
        Log.info {"phxSocketDidOpen"}
        channel.join()
                .onReceive("ok") {json->
                    Log.info { "Received OK on join:${json}"}
                }
                .onReceive("error") { error ->
                    Log.info { "Error joining: $error" }
                }
    }
    override fun phxSocketDidClose(event: String) {
        Log.info {"phxSocketDidClose"}
    }

    override fun phxSocketDidReceiveError(error: String){
        Log.info {"phxSocketDidReceiveError"}
    }
    // PhxSocketDelegate

    /*!< This is the main entry point of communication over Phoenix Channels. */
    lateinit var channel: PhxChannel

    /**
     *  \brief Trigger start of Network connection.
     *
     *  Detailed description
     *
     *  \param param
     *  \return return type
     */
    fun start(token: String, businessId: Long){
        val socket
        = PhxSocket("ws://app.talkshopapp.com:443/socket/websocket?token=$token", 1)
        socket.setDelegate(this)

        channel = PhxChannel(socket, "business:$businessId", mapOf())
        channel.bootstrap()

        // Instantiate the PhxChannel first before connecting on the socket.
        // This is because the connection can happen before the channel
        // is done instantiating.
        socket.connect()
    }
};