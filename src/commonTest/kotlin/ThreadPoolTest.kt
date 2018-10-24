import kotlin.test.*
import co.spin.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import timber.log.*

class ThreadPoolTest {


    init{
        initTimber()
        //println("Started")
    }

    @Test
    fun enqueMultipleTest() = runTest {
        val threadPool = ThreadPool(4)
        println("Started")
        repeat(6) {threadPool.enqueue({Timber.debug{"DO ITTTTTTT"}})}
        delay(2000L)
        return@runTest
    }

}