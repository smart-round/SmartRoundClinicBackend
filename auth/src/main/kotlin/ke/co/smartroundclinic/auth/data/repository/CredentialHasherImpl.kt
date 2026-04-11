package ke.co.smartroundclinic.auth.data.repository

import ke.co.smartroundclinic.auth.domain.repository.CredentialsHasher
import org.mindrot.jbcrypt.BCrypt

class CredentialHasherImpl: CredentialsHasher {
    override fun hash(text: String): String {
        return BCrypt.hashpw(text, BCrypt.gensalt())
    }

    override fun verify(text: String, hashed: String): Boolean {
        return BCrypt.checkpw(text, hashed)
    }
}