package com.rojas.syncpass.data.location

import com.rojas.syncpass.domain.model.DeviceLocation
import java.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Consulta las fuentes en paralelo sin dejar solicitudes activas al terminar. */
internal suspend fun firstValidLocation(
    providers: List<String>,
    clock: Clock,
    timeoutMillis: Long = 20_000,
    request: suspend (String) -> DeviceLocation?,
): DeviceLocation? = withTimeoutOrNull(timeoutMillis) {
    coroutineScope {
        val results = Channel<DeviceLocation?>(Channel.UNLIMITED)
        val jobs = providers.map { provider ->
            launch {
                val candidate = try {
                    request(provider)?.takeIf { runCatching { it.validate(clock.millis()) }.isSuccess }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
                results.send(candidate)
            }
        }
        try {
            var selected: DeviceLocation? = null
            repeat(providers.size) {
                val candidate = results.receive()
                if (candidate != null) {
                    selected = candidate
                    return@coroutineScope selected
                }
            }
            selected
        } finally {
            jobs.forEach { it.cancel() }
            results.close()
        }
    }
}
