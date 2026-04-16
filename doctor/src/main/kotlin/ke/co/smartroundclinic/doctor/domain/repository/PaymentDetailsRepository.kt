package ke.co.smartroundclinic.doctor.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.data.entity.PractitionerPaymentDetailsEntity


interface PaymentDetailsRepository {
    suspend fun addPaymentDetails(paymentDetails: PractitionerPaymentDetailsEntity): Resource<Nothing>
    suspend fun getPaymentDetails(doctorId: String): Resource<PractitionerPaymentDetailsEntity?>
    suspend fun updatePaymentDetails(accountName: String?, accountNumber: String?, bankCode: String?, branchCode: String?, bankName: String?, branchName: String?, doctorId: String): Resource<PractitionerPaymentDetailsEntity?>
    suspend fun deletePaymentDetails(doctorId: String): Resource<Nothing>
    suspend fun getAllPaymentDetails(page: Int, size: Int): Resource<Pair<List<PractitionerPaymentDetailsEntity>, Long>>
    suspend fun getPaymentDetailsById(id: String): Resource<PractitionerPaymentDetailsEntity?>

}