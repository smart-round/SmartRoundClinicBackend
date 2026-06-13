package ke.co.smartroundclinic.infra.koin

import io.lettuce.core.RedisClient
import ke.co.smartroundclinic.common.RedisRepository
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.redis.RedisRepositoryImpl
import org.koin.dsl.module

val redisModule = module {
    single<RedisClient> {
        RedisClient.create(AppConfig.redis.url)
    }
    single<RedisRepository> {
        RedisRepositoryImpl(get())
    }
}
