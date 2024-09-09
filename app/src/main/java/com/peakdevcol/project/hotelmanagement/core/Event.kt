package com.peakdevcol.project.hotelmanagement.core

/**
 * *
 * In this class you can control the Events that occur in the application and that these events
 * only occur once. For example when the screen is turn to other orientation you can be sure that
 * the observers don't reload again
 *
 * T is any provider of data that you require
 */

open class Event<out T>(private val content: T) {

    private var hasBeenHandled = false

    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    fun getContent(): T? {
        return content
    }

    fun peekContent(): T = content

}