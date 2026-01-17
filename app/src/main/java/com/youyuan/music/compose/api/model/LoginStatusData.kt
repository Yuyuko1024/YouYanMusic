package com.youyuan.music.compose.api.model

/**
 * /login/status 接口返回的数据结构
 * 注意：实际返回可能是 { "data": { "code": 200, "account": {...}, "profile": {...} } }
 */
data class LoginStatusData(
    val code: Int?,
    val account: Account?,
    val profile: Profile?,
    // 有些版本的 API 返回嵌套在 data 里
    val data: LoginStatusInnerData?
) {
    /**
     * 获取实际的 profile，优先从 data 中获取
     */
    fun getActualProfile(): Profile? {
        return data?.profile ?: profile
    }
    
    /**
     * 获取实际的 code，优先从 data 中获取
     */
    fun getActualCode(): Int? {
        return data?.code ?: code
    }
    
    /**
     * 获取实际的 account，优先从 data 中获取
     */
    fun getActualAccount(): Account? {
        return data?.account ?: account
    }
}

/**
 * 嵌套的内部数据结构
 */
data class LoginStatusInnerData(
    val code: Int?,
    val account: Account?,
    val profile: Profile?
)

data class Account(
    val id: Long?,
    val userName: String?,
    val type: Int?,
    val status: Int?,
    val createTime: Long?,
    val tokenVersion: Int?,
    val ban: Int?,
    val baoyueVersion: Int?,
    val donateVersion: Int?,
    val vipType: Int?,
    val anonimousUser: Boolean,
    val paidFee: Boolean
)

data class Profile(
    val userId: Long?,
    val userType: Int?,
    val nickname: String?,
    val avatarImgId: Long?,
    val avatarUrl: String?,
    val backgroundImgId: Long?,
    val backgroundUrl: String?,
    val signature: String?,
    val createTime: Long?,
    val userName: String?,
    val birthday: Long?,
    val authority: Int?,
    val gender: Int?,
    val accountStatus: Int?,
    val province: Int?,
    val city: Int?,
    val authStatus: Int?,
    val description: String?,
    val detailDescription: String?,
    val defaultAvatar: Boolean,
    val expertTags: List<String>?,
    val experts: Map<String, String>?,
    val djStatus: Int?,
    val locationStatus: Int?,
    val vipType: Int?,
    val followed: Boolean,
    val mutual: Boolean,
    val authenticated: Boolean,
    val lastLoginTime: Long?,
    val lastLoginIP: String?,
    val remarkName: String? = null,
    val viptypeVersion: Long?,
    val authenticationTypes: Int?,
    val avatarDetail: AvatarDetail? = null,
    val anchor: Boolean
)