package co.spin

import co.spin.utils.Log

class Network : PhxSocketDelegate() {
    // PhxSocketDelegate
    override fun phxSocketDidOpen(){
        Log.info {"phxSocketDidOpen"}
        channel.join()
                .onReceive("ok") {json->
                    Log.info { "Received OK on join:${json.jsonObject.toString()}"}
                }
                .onReceive("error") { error ->
                    Log.info { "Error joining: $error" }
                }
    }
    override fun phxSocketDidClose(event: String) {
        Log.info {"phxSocketDidClose: $event"}
    }

    override fun phxSocketDidReceiveError(error: String){
        Log.info {"phxSocketDidReceiveError: $error"}
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
    fun start(url:String, token: String, id: Long){
        val socket
        = PhxSocket(url/*"ws://localhost:4000/socket/websocket"*/, 1)
        socket.setDelegate(this)

        channel = PhxChannel(socket, "room:lobby", mapOf())
        channel.bootstrap()

        // Instantiate the PhxChannel first before connecting on the socket.
        // This is because the connection can happen before the channel
        // is done instantiating.
        socket.connect()
    }
}