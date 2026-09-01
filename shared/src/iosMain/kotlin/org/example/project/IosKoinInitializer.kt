package org.example.project

import org.example.project.di.initKoin

/**
 * Swift can't construct a `KoinAppDeclaration` lambda, so iOS gets this
 * thin, argument-less wrapper around the shared `initKoin()` instead.
 * Because this file is named `IosKoinInitializer.kt`, Kotlin/Native exposes
 * it to Swift as `IosKoinInitializerKt.doInitKoin()`.
 */
fun doInitKoin() {
    initKoin()
}
