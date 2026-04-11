package ke.co.smartroundclinic.common


import com.google.gson.Gson
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

@Serializable
data class DefaultResponse<T>(
    val httpStatusCode: Int = HttpStatusCode.OK.value,
    val status:Boolean = false,
    val message:String = "",
    val data:T? = null
){
    fun toJson(): String = Gson().toJson(this)
}

