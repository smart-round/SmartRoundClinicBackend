package ke.co.smartroundclinic.common

import io.ktor.http.HttpStatusCode

sealed class Resource<T>(
    val data:T? = null,
    val message:String? = null,
){
    class Success<T >(data: T?, message: String = "Success"): Resource<T>(data = data, message)
    class Error<T >(message: String, data: T? = null): Resource<T>(message = message,data = data)


    fun <R> toDefaultResponse(
        failedStatusCode: Int = HttpStatusCode.NotFound.value,
        successStatusCode: Int = HttpStatusCode.OK.value,
        successMessage:String? = null,
        errorMessage:String? = null,
        transform: (T?) -> R? = {null}
    ): DefaultResponse<R?> {
        return when (this) {
            is Success -> DefaultResponse(
                httpStatusCode = successStatusCode,
                status = true,
                message = successMessage ?: (message ?: "Success"),
                data = transform(this.data),
            )
            is Error -> DefaultResponse(
                httpStatusCode = failedStatusCode,
                status = false,
                message = errorMessage ?: (message ?: "Error"),
                data = transform(this.data),
            )
        }
    }


}