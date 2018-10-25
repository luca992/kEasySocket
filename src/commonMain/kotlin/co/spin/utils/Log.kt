package co.spin.utils

import com.soywiz.klogger.*

object Log {

    val logger = Logger("Socket")


    fun error(e: String){
        logger.trace { e }
    }

    fun debug(e: String){
        logger.info { e }
    }
}