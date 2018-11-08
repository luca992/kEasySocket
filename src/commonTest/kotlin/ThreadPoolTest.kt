import kotlin.test.*
import co.spin.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import co.spin.Utils.Log
import co.spin.utils.Log

class ThreadPoolTest {


    init{

        //println("Started")
    }

    @Test
    fun enqueMultipleTest() = runTest {
        val threadPool = ThreadPool(4)
        println("Started")
        repeat(6) {threadPool.enqueue({Log.debug{"DO ITTTTTTT"}})}
        delay(2000L)
        return@runTest
    }

}