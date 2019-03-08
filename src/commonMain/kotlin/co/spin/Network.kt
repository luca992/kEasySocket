package co.spin

import co.spin.utils.Log
import kotlinx.coroutines.Job

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
    fun start(url: Url, initialTopic: String) : Job{
        val socket
        = PhxSocket(url/*"ws://localhost:4000/socket/websocket"*/, 15)
        socket.setDelegate(this)
        socket.onMessage {
            Log.info {it}
        }
        channel = PhxChannel(socket, initialTopic, mapOf())

        // Instantiate the PhxChannel first before connecting on the socket.
        // This is because the connection can happen before the channel
        // is done instantiating.
        return socket.connect()
    }
}