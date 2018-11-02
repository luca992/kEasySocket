package co.spin.utils

import com.soywiz.klogger.*

object Log {

    val logger = Logger("socket")


    fun error(e: String){
        logger.error { e }
    }

    fun debug(e: String){
        logger.info { e }
    }
}