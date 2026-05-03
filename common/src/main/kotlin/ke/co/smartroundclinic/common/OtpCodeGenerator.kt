package ke.co.smartroundclinic.common

import java.security.SecureRandom

class OtpCodeGenerator {

    // SecureRandom is cryptographically strong unlike Random or Math.random()
    private val secureRandom = SecureRandom.getInstanceStrong()

    fun generateOtpCode(length: Int = 4): String {
        return (1..length)
            .map { secureRandom.nextInt(10) } // 0-9 only
            .joinToString("")
    }
}