data class ArticleCategoryEntity(
    val id: String = ObjectId().toString(),
    val name:String,
    val isActive: Boolean,
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String? = null,
)