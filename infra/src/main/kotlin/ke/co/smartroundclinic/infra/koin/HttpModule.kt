package ke.co.smartroundclinic.infra.koin

import ke.co.smartroundclinic.infra.provideHttpClient
import org.koin.dsl.module

val httpModule = module {
    single { provideHttpClient() }
}