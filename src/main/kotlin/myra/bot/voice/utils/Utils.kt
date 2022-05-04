package myra.bot.voice.utils

import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

inline fun <reified T> T.toJson(): String = json.encodeToString(this)

fun <T> asDeferred(scope: CoroutineScope = CoroutineScope(Dispatchers.Default), runnable: suspend () -> T): Deferred<T> {
    val future = CompletableDeferred<T>()
    scope.launch { future.complete(runnable.invoke()) }
    return future
}
