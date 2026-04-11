package ke.co.smartroundclinic.infra.koin

import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.storage.R2StorageRepositoryImpl
import ke.co.smartroundclinic.infra.storage.StorageRepository
import org.koin.dsl.module

val storageModule = module {
    single<StorageRepository> { R2StorageRepositoryImpl(AppConfig.r2) }
}
