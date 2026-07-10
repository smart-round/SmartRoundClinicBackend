package ke.co.smartroundclinic.consultation.domain.service

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.LoggerFactory

/**
 * In-process registry of open `/consultation/{id}/chat` WebSocket sessions, keyed by conversation
 * thread (doctorId:patientId) rather than consultationId — a thread spans multiple past sessions,
 * and the read-receipt trigger (REST) has no consultationId at all. Used to relay transient events
 * (typing, live read-receipt flips) to a specific connected counterpart; a user may have more than
 * one open session (multiple devices), so each thread+user maps to a set of sessions.
 *
 * This is per-JVM-process state — it will not relay across multiple backend instances. Redis pub/sub
 * would be the natural upgrade if this backend ever scales horizontally; no such infra exists today,
 * so this is an intentionally minimal, single-instance-only design.
 */
class ConsultationSocketRegistry {
    private val log = LoggerFactory.getLogger(ConsultationSocketRegistry::class.java)
    private val byThread = ConcurrentHashMap<String, ConcurrentHashMap<String, MutableSet<DefaultWebSocketServerSession>>>()

    fun register(threadKey: String, userId: String, session: DefaultWebSocketServerSession) {
        val users = byThread.computeIfAbsent(threadKey) { ConcurrentHashMap() }
        val sessions = users.computeIfAbsent(userId) { Collections.newSetFromMap(ConcurrentHashMap()) }
        sessions.add(session)
    }

    fun unregister(threadKey: String, userId: String, session: DefaultWebSocketServerSession) {
        val users = byThread[threadKey] ?: return
        val sessions = users[userId] ?: return
        sessions.remove(session)
        if (sessions.isEmpty()) users.remove(userId, sessions)
        if (users.isEmpty()) byThread.remove(threadKey, users)
    }

    /** Best-effort — drops (and logs) any session that fails to send rather than propagating. */
    suspend fun sendToUser(threadKey: String, userId: String, text: String) {
        val sessions = byThread[threadKey]?.get(userId) ?: return
        sessions.toList().forEach { session ->
            try {
                session.send(Frame.Text(text))
            } catch (e: Exception) {
                log.warn("Dropped stale session for userId=$userId threadKey=$threadKey — ${e.message}")
            }
        }
    }

    fun hasOpenSession(threadKey: String, userId: String): Boolean =
        byThread[threadKey]?.get(userId)?.isNotEmpty() == true

    companion object {
        fun threadKey(doctorId: String, patientId: String) = "$doctorId:$patientId"
    }
}
