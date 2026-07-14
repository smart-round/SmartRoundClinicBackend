package ke.co.smartroundclinic.common

interface RefundProcessor {
    suspend fun processRefund(refundId: String)
}
