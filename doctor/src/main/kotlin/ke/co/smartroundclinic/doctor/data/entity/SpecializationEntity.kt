package ke.co.smartroundclinic.doctor.data.entity

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.conversions.Bson

data class SpecializationEntity(
    val id: String = BsonId().toString(),
    val doctorsId: String,
    val specializationId:String,
    val subSpecializationId:String? = null,
)