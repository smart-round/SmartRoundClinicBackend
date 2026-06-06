package ke.co.smartroundclinic.payments.data.remote.instasend

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

fun computeStkPushSignature(secretKey: String, phoneNumber: String, amount: String, apiRef: String): String {
    val message = "$phoneNumber$amount$apiRef"
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(secretKey.toByteArray(Charsets.UTF_8), "HmacSHA256"))
    return mac.doFinal(message.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
