package ke.co.smartroundclinic.infra.redis

import io.lettuce.core.RedisClient
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import ke.co.smartroundclinic.common.RedisRepository
import kotlinx.coroutines.future.await

class RedisRepositoryImpl(client: RedisClient) : RedisRepository {

    private val connection: StatefulRedisConnection<String, String> = client.connect()
    private val commands: RedisAsyncCommands<String, String> = connection.async()

    override suspend fun set(key: String, value: String, ttlSeconds: Long?) {
        if (ttlSeconds != null) {
            commands.set(key, value, SetArgs().ex(ttlSeconds)).await()
        } else {
            commands.set(key, value).await()
        }
    }

    override suspend fun get(key: String): String? = commands.get(key).await()

    override suspend fun delete(key: String) {
        commands.del(key).await()
    }

    override suspend fun exists(key: String): Boolean =
        (commands.exists(key).await() ?: 0L) > 0L

    override suspend fun expire(key: String, ttlSeconds: Long) {
        commands.expire(key, ttlSeconds).await()
    }

    override suspend fun setIfAbsent(key: String, value: String, ttlSeconds: Long?): Boolean {
        val args = if (ttlSeconds != null) SetArgs().nx().ex(ttlSeconds) else SetArgs().nx()
        return commands.set(key, value, args).await() == "OK"
    }

    override suspend fun increment(key: String): Long =
        commands.incr(key).await() ?: 0L

    override suspend fun getAndDelete(key: String): String? =
        commands.getdel(key).await()
}
