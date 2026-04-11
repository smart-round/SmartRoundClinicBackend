package ke.co.smartroundclinic.infra.koin

import ke.co.smartroundclinic.infra.AppConfig
import org.koin.dsl.module

val appConfigModule = module{
    single { AppConfig }
}