package ke.co.smartroundclinic.doctorchat.domain.service

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.LoggerFactory

/**
 * In-process registry of open `/doctor-chat/threads/{threadId}` WebSocket sessions — mirrors
 * [ke.co.smartroundclinic.consultation.domain.service.ConsultationSocketRegistry], keyed directly
 * by threadId (a real persisted id here, unlike consultation's synthetic "doctorId:patientId"
 * string) since it's used to relay call-invite ring/answer/decline/cancel events to the other
 * participant. Per-JVM-process state, same scaling caveat as the consultation registry.
 */
class DoctorChatSocketRegistry {
    private val log = LoggerFactory.getLogger(DoctorChatSocketRegistry::class.java)
    private val byThread = ConcurrentHashMap<String, ConcurrentHashMap<String, MutableSet<DefaultWebSocketServerSession>>>()

    fun register(threadId: String, userId: String, session: DefaultWebSocketServerSession) {
        val users = byThread.computeIfAbsent(threadId) { ConcurrentHashMap() }
        val sessions = users.computeIfAbsent(userId) { Collections.newSetFromMap(ConcurrentHashMap()) }
        sessions.add(session)
    }

    fun unregister(threadId: String, userId: String, session: DefaultWebSocketServerSession) {
        val users = byThread[threadId] ?: return
        val sessions = users[userId] ?: return
        sessions.remove(session)
        if (sessions.isEmpty()) users.remove(userId, sessions)
        if (users.isEmpty()) byThread.remove(threadId, users)
    }

    suspend fun sendToUser(threadId: String, userId: String, text: String) {
        val sessions = byThread[threadId]?.get(userId) ?: return
        sessions.toList().forEach { session ->
            try {
                session.send(Frame.Text(text))
            } catch (e: Exception) {
                log.warn("Dropped stale session for userId=$userId threadId=$threadId — ${e.message}")
            }
        }
    }
}
