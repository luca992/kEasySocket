package co.spin

import kotlinx.coroutines.*
import kotlin.coroutines.*

fun <T> runTest(block: suspend () -> T) {
    runBlocking { block() }
}

val Unconfined: CoroutineContext = Dispatchers.Unconfined

