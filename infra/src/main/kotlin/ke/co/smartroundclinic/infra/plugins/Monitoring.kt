package ke.co.smartroundclinic.infra.plugins

import com.sksamuel.cohort.*
import com.sksamuel.cohort.cpu.*
import com.sksamuel.cohort.memory.*
import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.forwardedheaders.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.*
import io.ktor.util.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import org.slf4j.event.*

val RequestStartTimeKey = AttributeKey<Long>("RequestStartTime")

fun Application.configureMonitoring() {
    intercept(ApplicationCallPipeline.Monitoring) {
        call.attributes.put(RequestStartTimeKey, System.currentTimeMillis())
    }
    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    // Trust the reverse proxy's X-Forwarded-For / X-Forwarded-Proto headers so
    // call.request.origin.remoteHost reflects the real client IP, not the proxy's.
    install(XForwardedHeaders)

    install(MicrometerMetrics) {
        registry = appMicrometerRegistry
    }

    val healthchecks = HealthCheckRegistry(Dispatchers.Default) {
        register(FreememHealthCheck.mb(250), 10.seconds, 10.seconds)
        register(ProcessCpuHealthCheck(0.8), 10.seconds, 10.seconds)
    }
    
    install(Cohort) {
        operatingSystem = true
        jvmInfo = true
        sysprops = true
        heapDump = true
        threadDump = true
        verboseHealthCheckResponse = true
        healthcheck("/health", healthchecks)
    }
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path() != "/metrics-micrometer" }
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val userAgent = call.request.headers["User-Agent"]
            val uri = call.request.uri
            val clientIp = call.request.origin.remoteHost
            val startTime = call.attributes.getOrNull(RequestStartTimeKey)
            val duration = if (startTime != null) "${System.currentTimeMillis() - startTime}ms" else "unknown"
            
            val statusColor = when (status?.value ?: 0) {
                in 200..299 -> "\u001B[32m" // GREEN
                in 300..399 -> "\u001B[33m" // YELLOW
                in 400..499 -> "\u001B[31m" // RED
                in 500..599 -> "\u001B[31m" // RED
                else -> "\u001B[0m" // DEFAULT
            }
            
            val methodColor = when (httpMethod) {
                "GET" -> "\u001B[32m"    // GREEN
                "POST" -> "\u001B[33m"   // YELLOW
                "PUT" -> "\u001B[34m"    // BLUE
                "DELETE" -> "\u001B[31m" // RED
                else -> "\u001B[0m"
            }
            
            val reset = "\u001B[0m"
            
            "${statusColor}Path:$uri, status: $status$reset, ${methodColor}HTTP method: $httpMethod$reset, duration: $duration, IP: $clientIp, User agent: $userAgent"
        }
    }
    routing {
        get("/metrics-micrometer") {
            call.respond(appMicrometerRegistry.scrape())
        }
    }
}
