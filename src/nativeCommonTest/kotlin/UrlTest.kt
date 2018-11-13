import co.spin.ezwsclient.Url
import co.spin.ezwsclient.WebSocket

import kotlin.test.*

class UrlTest {


    init{

        //println("Started")
    }

    @Test
    fun parseUrlTestQueryInString() {

        val url = Url.parseUrl("ws://localhost:8126/foo?token=SFMyNTY.asdassdadsasdasdasd")
        assertEquals(
                Url(
                        "ws",
                        "localhost",
                        8126,
                        "/foo?token=SFMyNTY.asdassdadsasdasdasd",
                        "token=SFMyNTY.asdassdadsasdasdasd"),
                url
        )
    }

    @Test
    fun parseUrlTestAddedQuery() {
        val url = Url.parseUrl("ws://localhost:8126/foo", "token=SFMyNTY.asdassdadsasdasdasd")
        assertEquals(
                Url(
                        "ws",
                        "localhost",
                        8126,
                        "/foo?token=SFMyNTY.asdassdadsasdasdasd",
                        "token=SFMyNTY.asdassdadsasdasdasd"),
                url
        )
    }

}