package co.spin.utils

import com.soywiz.klogger.Logger


internal val Log = Logger("kSocket").apply {
    level = Logger.Level.DEBUG
}

fun setKEasySocketLogLevel(level: Logger.Level){
    Log.level = level
}