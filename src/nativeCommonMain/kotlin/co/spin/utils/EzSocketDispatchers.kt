package kotlinx.coroutines

import co.spin.utils.Log
import kotlin.coroutines.*
import platform.Foundation.*
import platform.darwin.*
import kotlinx.cinterop.*

@kotlin.native.ThreadLocal
internal actual object EzSocketDispatchers {

    public actual var Default: CoroutineDispatcher = MainLoopDispatcher

    public actual val Main: CoroutineDispatcher get() = MainLoopDispatcher

    public actual val Unconfined: CoroutineDispatcher get() = Dispatchers.Unconfined // Avoid freezing

    @UseExperimental(InternalCoroutinesApi::class)
    private object MainLoopDispatcher: CoroutineDispatcher(), Delay {

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            dispatch_async(dispatch_get_main_queue()) {
                try {
                    block.run()
                } catch (err: Throwable) {
                    Log.error {err.message}
                    throw err
                }
            }
        }



        @InternalCoroutinesApi
        override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
            dispatch_after(dispatch_time(DISPATCH_TIME_NOW, timeMillis * 1_000_000), dispatch_get_main_queue()) {
                try {
                    with(continuation) {
                        resumeUndispatched(Unit)
                    }
                } catch (err: Throwable) {
                    Log.error {err.message}
                    throw err
                }
            }
        }

        @InternalCoroutinesApi
        override fun invokeOnTimeout(timeMillis: Long, block: Runnable): DisposableHandle {
            val handle = object : DisposableHandle {
                var disposed = false
                    private set

                override fun dispose() {
                    disposed = true
                }
            }
            dispatch_after(dispatch_time(DISPATCH_TIME_NOW, timeMillis * 1_000_000), dispatch_get_main_queue()) {
                try {
                    if (!handle.disposed) {
                        block.run()
                    }
                } catch (err: Throwable) {
                    Log.error {err.message}
                    throw err
                }
            }

            return handle
        }

    }
}
