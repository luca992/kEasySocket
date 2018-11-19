# Intro
   kEasySocket is a multiplatform kotlin macOs, windows, linux(untested) websocket client. Also supports communication with a phoenix framework web project through the use of channels over websocket.

  Learn more about the Phoenix Framework at http://www.phoenixframework.org/
  
# Basic Websocket Example Usage

```kotlin
fun main(args: Array<String>) = runBlocking<Unit> {
    val ws = WebSocket.fromUrl("ws://localhost:8126/foo") ?: return@runBlocking
    var callback = { s:UByteArray ->
        println(s.toByteArray().stringFromUtf8())
    }
    var sendCount= 0
    while (ws.readyState != CLOSED) {
        ws.poll()
        if (sendCount<50) {
            ws.send("hello")
            sendCount++
        } else {
            ws.close()
        }
        ws.dispatchBinary(callback)
    }
    return@runBlocking
}
```

# Phoenix Channels Example Usage

```kotlin
fun main(args: Array<String>) = runBlocking<Unit> {
    if (args.isEmpty())
        return@runBlocking

    val n = Network()
    val url : Url = parseUrl(args[0], if (args.size > 1) args[1] else null) ?: throw Exception("Can't parse Url")
    //val url : Url = parseUrl("ws://localhost:8126/foo", null) ?: throw Exception("Can't parse Url")

    val j = n.start(url, if (args.size > 2) args[2] else "")
    j.join()


    return@runBlocking
}
```


```kotlin
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

        channel = PhxChannel(socket, initialTopic, mapOf())
        channel.bootstrap()

        // Instantiate the PhxChannel first before connecting on the socket.
        // This is because the connection can happen before the channel
        // is done instantiating.
        return socket.connect()
    }
}
```

# Credit
## Websocket Client
   https://github.com/dhbaird/easywsclient
## Websocket over OpenSSL
  https://github.com/machinezone/IXWebSocket
