package kotlinx.coroutines

import kotlin.coroutines.*

public actual object EzSocketDispatchers {

    public actual val Default: CoroutineDispatcher get() = Dispatchers.Default

    public actual val Main: CoroutineDispatcher get() = Dispatchers.Main

    public actual val Unconfined: CoroutineDispatcher get() = Dispatchers.Unconfined // Avoid freezing
}
