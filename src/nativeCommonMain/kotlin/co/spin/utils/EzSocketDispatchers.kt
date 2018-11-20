package kotlinx.coroutines

import kotlin.coroutines.*

public actual object EzSocketDispatchers {

    public actual val Default: CoroutineDispatcher get() = Dispatchers.Unconfined

    public actual val Main: CoroutineDispatcher get() = Dispatchers.Unconfined

    public actual val Unconfined: CoroutineDispatcher get() = Dispatchers.Unconfined // Avoid freezing
}
