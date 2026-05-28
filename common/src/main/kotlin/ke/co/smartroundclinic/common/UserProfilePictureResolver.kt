package ke.co.smartroundclinic.common

interface UserProfilePictureResolver {
    suspend fun getProfilePictureUrls(userIds: List<String>): Map<String, String?>
}
