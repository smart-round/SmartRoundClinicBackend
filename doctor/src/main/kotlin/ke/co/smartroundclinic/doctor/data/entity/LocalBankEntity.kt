package ke.co.smartroundclinic.doctor.data.entity

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class LocalBankBranchEntity(
    val branchCode: String,
    val branchName: String,
)

data class LocalBankEntity(
    @BsonId val id: String = ObjectId().toString(),
    val bankName: String,
    val bankCode: String,
    val branches: List<LocalBankBranchEntity>,
)