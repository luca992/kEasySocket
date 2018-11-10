
import kotlin.test.*
import co.spin.*
import co.spin.ezwsclient.WebSocket
import co.spin.ezwsclient.WebSocket.ReadyStateValues.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

//import com.soywiz.klogger.*
fun main(args: Array<String>) = runBlocking<Unit> {

    /*val ws = WebSocket.fromUrl("ws://localhost:8126/foo") ?: return@runBlocking
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
    }*/

    if (args.isEmpty())
        return@runBlocking

    val n = Network()
    n.start(args[0],args[1],13L)


    return@runBlocking
}
