package com.peakdevcol.project.hotelmanagement.ui

import com.peakdevcol.project.hotelmanagement.ui.main.OrdersRoute
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * RNF-01: on the JVM, navigation finds the serializer of a route by reflection, and the JVM refuses it
 * for a class that is private in Kotlin. Android allows it, so only the desktop app got stuck loading.
 */
class NavigationRoutesTest {
    @OptIn(InternalSerializationApi::class)
    @Test
    fun everyRouteFindsItsSerializerByReflection() {
        val routes = serializableClassesOf(packageName = "com.peakdevcol.project.hotelmanagement.ui")
        assertTrue(OrdersRoute::class.java in routes)

        val unreachable = routes.filter { route -> runCatching { route.kotlin.serializer() }.isFailure }

        assertEquals(emptyList(), unreachable.map { it.name })
    }

    private fun serializableClassesOf(packageName: String): List<Class<*>> {
        val classesRoot = File(AppViewModel::class.java.protectionDomain.codeSource.location.toURI())
        return File(classesRoot, packageName.replace('.', File.separatorChar)).walk()
            .filter { it.extension == "class" }
            .map { it.relativeTo(classesRoot).path.removeSuffix(".class").replace(File.separatorChar, '.') }
            .map { Class.forName(it, false, javaClass.classLoader) }
            .filter { it.isAnnotationPresent(Serializable::class.java) }
            .toList()
    }
}
