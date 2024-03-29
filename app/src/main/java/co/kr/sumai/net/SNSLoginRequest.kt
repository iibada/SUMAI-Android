package co.kr.sumai.net

data class SNSLoginRequest(
    private val SNSType: String,
    private val accessToken: String,
    private val email: String?,
    private val name: String?,
    private val id: String?,
    private val gender: String?,
    private val birth: String?,
    private val ageRange: String?,
    private val imageURL: String?
)