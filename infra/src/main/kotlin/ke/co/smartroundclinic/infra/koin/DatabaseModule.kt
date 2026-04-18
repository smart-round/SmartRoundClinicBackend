package ke.co.smartroundclinic.infra.koin

import com.mongodb.kotlin.client.coroutine.MongoClient
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.infra.AppConfig
import org.koin.core.qualifier.named
import org.koin.dsl.module

val databaseModule = module {
    single { 
        val config = AppConfig.mongo // Directly access the object
        println("[DEBUG_LOG] MongoDB Connection String: ${config.connectionString.replace(config.password, "****")}") // Print for debugging
        MongoClient.create(config.connectionString)
    }

    single(named("authDb")) {
        get<MongoClient>().getDatabase(MongoDBConstants.AUTH_DB)
    }
    single(named("adminDb")) {
        get<MongoClient>().getDatabase(MongoDBConstants.ADMIN_DB)
    }
    single(named("doctorDb")) {
        get<MongoClient>().getDatabase(MongoDBConstants.DOCTOR_DB)
    }
    single(named("patientDb")) {
        get<MongoClient>().getDatabase(MongoDBConstants.PATIENT_DB)
    }
    single(named("schedulingDb")) {
        get<MongoClient>().getDatabase(MongoDBConstants.SCHEDULING_DB)
    }
}
