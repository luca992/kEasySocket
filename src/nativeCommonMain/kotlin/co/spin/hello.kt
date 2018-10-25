
import kotlin.test.*
import co.spin.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
//import com.soywiz.klogger.*
fun main(args: Array<String>) = runBlocking<Unit> {

    val threadPool = ThreadPool(1)
    //repeat(6) {threadPool.enqueue(GlobalScope.async(TDispatchers.Default){})}
    delay(3000L)

    return@runBlocking
}
