package com.youyuan.music.compose.api.model


// 生成二维码登录密钥数据类
data class QrCodeLoginKeyData(
    val code: Int,
    val data: QrCodeLoginKey
)

data class QrCodeLoginKey(
    val unikey: String,
    val code: Int
)

// 二维码图片数据类
data class QrCodeLoginImgData(
    val code: Int,
    val data: QrCodeLoginImg
)

data class QrCodeLoginImg(
    val qrimg: String,
    val qrurl: String,
    val code: Int
)

// 检查二维码登录状态数据类
data class QrCodeLoginCheckData(
    val code: Int,
    val message: String?,
    val cookie: String?,
)