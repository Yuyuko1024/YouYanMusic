package com.youyuan.music.compose.api.model

/**
 * Subset of /login/cellphone response we care about.
 * We primarily persist `cookie` and then call /login/status to refresh profile.
 */
data class CellphoneLoginResponse(
    val code: Int?,
    val cookie: String?,
    val profile: Profile? = null,
    val message: String? = null,
)
