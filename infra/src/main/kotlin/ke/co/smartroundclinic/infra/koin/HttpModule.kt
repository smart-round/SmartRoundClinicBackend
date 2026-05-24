package ke.co.smartroundclinic.infra.koin

import ke.co.smartroundclinic.infra.provideHttpClient
import ke.co.smartroundclinic.infra.realtime.RealtimeKitClient
import org.koin.dsl.module

val httpModule = module {
    single { provideHttpClient() }
    single { RealtimeKitClient(get()) }
}