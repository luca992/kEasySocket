package co.spin

import kotlinx.coroutines.channels.*
import kotlinx.coroutines.*
import co.spin.utils.Queue

class ThreadPool(numWorkers: Int) {


    private var queue = Queue<() -> Unit>()

    init {
        val producer = distributeJobs()
        repeat(numWorkers) { launchWorker(it, producer) }
    }

    fun distributeJobs() = GlobalScope.produce<() -> Unit>(TDispatchers.Default) {
        while (true){
            if (!queue.isEmpty()) send(queue.dequeue()!!)
            delay(100)
        }

    }

    private fun launchWorker(id: Int, channel: ReceiveChannel<() -> Unit>)
    = GlobalScope.launch(TDispatchers.Default) {
        for (func in channel) {
            func()
        }
    }


    fun enqueue(func: () -> Unit){
        queue.enqueue(func)
    }

}
