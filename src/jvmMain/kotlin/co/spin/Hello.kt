import co.spin.Network
import co.spin.Url
import co.spin.Url.Companion.parseUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.EzSocketDispatchers
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) = runBlocking {
    if (args.isEmpty())
        return@runBlocking

    val n = Network()
    val url : Url = parseUrl(args[0], if (args.size > 1) args[1] else null) ?: throw Exception("Can't parse Url")
    EzSocketDispatchers.Default = Dispatchers.Default
    val j = n.start(url, if (args.size > 2) args[2] else "")
    j.join()
    println("DONE!!!!!!!!!")
    return@runBlocking
}

