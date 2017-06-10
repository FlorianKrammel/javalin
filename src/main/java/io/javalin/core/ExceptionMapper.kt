/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core

import io.javalin.Context
import io.javalin.ExceptionHandler
import io.javalin.HaltException
import org.slf4j.LoggerFactory
import java.util.*
import javax.servlet.http.HttpServletResponse

class ExceptionMapper {

    private val log = LoggerFactory.getLogger(ExceptionMapper::class.java)

    private val exceptionMap: MutableMap<Class<out Exception>, ExceptionHandler<Exception>?> = HashMap()

    fun put(exceptionClass: Class<out Exception>, handler: ExceptionHandler<Exception>?) {
        this.exceptionMap[exceptionClass] = handler
    }

    fun clear() = this.exceptionMap.clear()

    internal fun handle(exception: Exception, ctx: Context) {
        if (exception is HaltException) {
            ctx.status(exception.statusCode)
            ctx.body(exception.body)
            return
        }
        val exceptionHandler = this.getHandler(exception.javaClass)
        if (exceptionHandler != null) {
            exceptionHandler.handle(exception, ctx)
        } else {
            log.warn("Uncaught exception", exception)
            ctx.body("Internal server error")
            ctx.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
        }
    }

    fun getHandler(exceptionClass: Class<out Exception>): ExceptionHandler<Exception>? {
        if (this.exceptionMap.containsKey(exceptionClass)) {
            return this.exceptionMap[exceptionClass]
        }
        var superclass: Class<*>? = exceptionClass.superclass
        while (superclass != null) {
            if (this.exceptionMap.containsKey(superclass)) {
                val matchingHandler = this.exceptionMap[superclass]
                this.exceptionMap[exceptionClass] = matchingHandler // superclass was found, avoid search next time
                return matchingHandler
            }
            superclass = superclass.superclass
        }
        this.exceptionMap[exceptionClass] = null // nothing was found, avoid search next time
        return null
    }

}
