package kotlinx.coroutines

import kotlin.coroutines.*

internal actual object EzSocketDispatchers {

    public actual var Default: CoroutineDispatcher = Dispatchers.Default

    public actual val Main: CoroutineDispatcher get() = Dispatchers.Main

    public actual val Unconfined: CoroutineDispatcher get() = Dispatchers.Unconfined // Avoid freezing
}
