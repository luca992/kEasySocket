import co.spin.ezwsclient.WebSocket

import kotlin.test.*

class WebSocketTest {


    init{

        //println("Started")
    }

    @Test
    fun parseUrlTestQueryInString() {

        val url = WebSocket.parseUrl("ws://localhost:8126/foo?token=SFMyNTY.asdassdadsasdasdasd")
        assertEquals(
                WebSocket.Companion.Url(
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
        val url = WebSocket.parseUrl("ws://localhost:8126/foo", "token=SFMyNTY.asdassdadsasdasdasd")
        assertEquals(
                WebSocket.Companion.Url(
                        "ws",
                        "localhost",
                        8126,
                        "/foo?token=SFMyNTY.asdassdadsasdasdasd",
                        "token=SFMyNTY.asdassdadsasdasdasd"),
                url
        )
    }

}