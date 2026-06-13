package ke.co.smartroundclinic.auth.domain.usecase

import ke.co.smartroundclinic.common.RedisRepository
import ke.co.smartroundclinic.infra.redis.RedisKeys

class TrackUserPresenceUseCase(private val redis: RedisRepository) {

    suspend operator fun invoke(userId: String, isActive: Boolean) {
        redis.set(
            key = RedisKeys.presence(userId),
            value = isActive.toString(),
            ttlSeconds = RedisKeys.PRESENCE_TTL_SECONDS,
        )
    }

    suspend fun clear(userId: String) {
        redis.delete(RedisKeys.presence(userId))
    }
}
