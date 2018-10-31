
import kotlin.test.*
import co.spin.*
import co.spin.ezwsclient.WebSocket
import co.spin.ezwsclient.WebSocket.ReadyStateValues.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
//import com.soywiz.klogger.*
fun main(args: Array<String>) = runBlocking<Unit> {



    val ws = WebSocket.fromUrl("ws://localhost:8126/foo")!!

    while (ws.readyState != CLOSED) {
        ws.poll()
        GlobalScope.launch(TDispatchers.Default) {
            ws.dispatchBinary().consumeEach {
                println(it.toByteArray().stringFromUtf8())
            }
        }
    }
    delay(30000L)

    return@runBlocking
}
