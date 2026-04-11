package ke.co.smartroundclinic.auth.domain.repository

interface CredentialsHasher {
    fun hash(text: String): String
    fun verify(text: String, hashed: String): Boolean
}