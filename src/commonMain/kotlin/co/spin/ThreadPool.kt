package co.spin

import co.spin.utils.Log
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.*

class ThreadPool(numWorkers: Int) {

    var feeder = Channel<() -> Unit>(0)

    init {
        repeat(numWorkers) { launchWorker(it, feeder) }
    }


    private fun launchWorker(id: Int, channel: ReceiveChannel<() -> Unit>)
    = GlobalScope.launch(TDispatchers.Default) {
        //Log.debug { "trying to consume" }
        for (func in channel) {
            //Log.debug { "consuming" }
            func()
            //Log.debug { "consumed" }
        }
    }


    fun enqueue(func: () -> Unit){
        //Log.debug { "trying to feed" }
        GlobalScope.launch(TDispatchers.Default) {
            //Log.debug { "feeding" }
            feeder.send(func)
            //Log.debug { "fed" }
        }
    }

}
