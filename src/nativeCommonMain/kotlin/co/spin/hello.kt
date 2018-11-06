
import kotlin.test.*
import co.spin.*
import co.spin.ezwsclient.WebSocket
import co.spin.ezwsclient.WebSocket.ReadyStateValues.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
//import com.soywiz.klogger.*
fun main(args: Array<String>) = runBlocking<Unit> {



    val ws = WebSocket.fromUrl("ws://localhost:8126/foo") ?: return@runBlocking
    var sendCount= 0
    while (ws.readyState != CLOSED) {
        ws.poll()
        if (sendCount<50) {
            ws.send("hello")
            sendCount++
        } else {
            ws.close()
        }
        GlobalScope.launch(TDispatchers.Default) {
            ws.dispatchBinary().consumeEach {
                println(it.toByteArray().stringFromUtf8())
            }
        }
    }
    return@runBlocking
}
