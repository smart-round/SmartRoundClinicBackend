package ke.co.smartroundclinic.common

interface RedisRepository {
    suspend fun set(key: String, value: String, ttlSeconds: Long? = null)
    suspend fun get(key: String): String?
    suspend fun delete(key: String)
    suspend fun exists(key: String): Boolean
    suspend fun expire(key: String, ttlSeconds: Long)
    suspend fun setIfAbsent(key: String, value: String, ttlSeconds: Long? = null): Boolean
    suspend fun increment(key: String): Long
    suspend fun getAndDelete(key: String): String?
}
