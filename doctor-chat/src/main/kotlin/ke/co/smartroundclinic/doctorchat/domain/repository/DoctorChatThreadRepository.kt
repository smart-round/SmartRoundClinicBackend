package ke.co.smartroundclinic.doctorchat.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctorchat.data.entity.DoctorChatThreadEntity

interface DoctorChatThreadRepository {
    /** Returns the thread for this doctor pair, creating it if it doesn't exist yet — order-independent (A,B) == (B,A). */
    suspend fun getOrCreate(doctorAId: String, doctorBId: String): Resource<DoctorChatThreadEntity>
    suspend fun getById(threadId: String): Resource<DoctorChatThreadEntity?>
    suspend fun getByVideoRoomId(videoRoomId: String): Resource<DoctorChatThreadEntity?>
    suspend fun getThreadsForDoctor(doctorId: String): Resource<List<DoctorChatThreadEntity>>
    suspend fun setVideoRoomId(threadId: String, videoRoomId: String): Resource<DoctorChatThreadEntity?>

    /** Stores [videoRoomId] only if the thread has no meeting ID yet; returns whichever ID ends up stored (this call's, or one that won a race). */
    suspend fun setVideoRoomIdIfAbsent(threadId: String, videoRoomId: String): Resource<String>
    suspend fun clearVideoRoomId(threadId: String, completedRoomId: String? = null): Resource<Unit>
}
