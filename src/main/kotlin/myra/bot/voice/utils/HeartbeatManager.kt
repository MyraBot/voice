package myra.bot.voice.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ForkJoinPool

private val scope = CoroutineScope(ForkJoinPool().asCoroutineDispatcher())

fun startInterval(interval:Long) {
    scope.launch {
        delay(interval)

    }
}