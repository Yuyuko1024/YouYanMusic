package com.youyuan.music.compose.api.model

/**
 * Used by /captcha/sent and /captcha/verify.
 * Example: { "code": 200, "data": true }
 */
data class CaptchaResponse(
    val code: Int?,
    val data: Boolean?,
    val message: String? = null,
)
