package ke.co.smartroundclinic.infra.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.milliseconds

interface BackgroundTask {
    val name: String
    val intervalMs: Long
    suspend fun execute()
}

fun Application.configureBackgroundTasks(tasks: List<BackgroundTask>) {
    if (tasks.isEmpty()) return
    val log = LoggerFactory.getLogger("BackgroundTasks")
    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    monitor.subscribe(ApplicationStopped) { scope.coroutineContext[Job]?.cancel() }

    tasks.forEach { task ->
        scope.launch {
            log.info("[BG] Registered task '${task.name}' — interval ${task.intervalMs}ms")
            while (isActive) {
                try {
                    task.execute()
                } catch (e: Exception) {
                    log.error("[BG] Task '${task.name}' threw — ${e.message}", e)
                }
                delay(task.intervalMs.milliseconds)
            }
        }
    }
}
