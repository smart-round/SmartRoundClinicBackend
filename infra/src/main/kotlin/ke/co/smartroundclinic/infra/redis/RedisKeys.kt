package ke.co.smartroundclinic.infra.redis

object RedisKeys {
    /** Active/inactive presence flag for a connected user. TTL = PRESENCE_TTL_SECONDS. */
    fun presence(userId: String) = "presence:$userId"

    const val PRESENCE_TTL_SECONDS = 35L
}