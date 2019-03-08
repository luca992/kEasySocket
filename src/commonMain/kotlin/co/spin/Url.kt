package co.spin

import co.spin.utils.Log

data class Url(val protocol : String,
               val host : String,
               val port : Int,
               val path : String,
               val query: String?) {
        
    companion object {
        fun parseUrl(url: String, query: String? = null) : Url? {
            val ex = """(ws|wss)://([^/ :]+):?([^/ ]*)(/?[^ #?]*)\x3f?([^ #]*)#?([^ ]*)""".toRegex()
            val found = ex.find(url)

            if (found?.groups == null) {
                return null
            }

            val protocol = found.groups[1]?.value
            val host     = found.groups[2]?.value
            val portStr  = found.groups[3]?.value
            var path     = found.groups[4]?.value
            val _query   = if (!found.groups[5]?.value.isNullOrBlank()) found.groups[5]?.value else query

            Log.info {"$protocol : $host : $portStr : $path : $_query"}

            val port : Int = if (portStr.isNullOrEmpty()) {
                if (protocol == "wss") 443
                else 80
            } else portStr!!.toInt()


            if (path.isNullOrEmpty()) {
                path = "/"
            } else if (path!![0] != '/') {
                path = "/$path"
            }

            if (!_query.isNullOrEmpty()) {
                path += "?"
                path += _query
            }

            return Url(protocol!!,
                    host!!,
                    port,
                    path,
                    _query
            )
        }
    }
}