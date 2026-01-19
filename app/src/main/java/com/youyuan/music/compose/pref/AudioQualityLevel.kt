package com.youyuan.music.compose.pref

/**
 * /song/url/v1 的 level 参数枚举。
 *
 * 说明：不同账号权限/歌曲可能返回不同可用档位。
 * 我们用 "level" 作为唯一标识，并在 UI 中做友好展示。
 */
enum class AudioQualityLevel(
    val level: String,
    val displayName: String,
) {
    STANDARD("standard", "标准"),
    HIGHER("higher", "较高"),
    EXHIGH("exhigh", "极高"),
    LOSSLESS("lossless", "无损"),
    HIRES("hires", "Hi-Res"),
    JYEFFECT("jyeffect", "沉浸环绕"),
    SKY("sky", "沉浸全景声"),
    JYMASTER("jymaster", "超清母带"),
    ;

    companion object {
        fun fromLevel(raw: String?): AudioQualityLevel? {
            val v = raw?.trim()?.lowercase() ?: return null
            return entries.firstOrNull { it.level == v }
        }

        fun default(): AudioQualityLevel = STANDARD

        /** 用于“可用音质探测”的候选顺序（从高到低 + 标准兜底） */
        fun probeOrder(): List<AudioQualityLevel> = listOf(
            JYMASTER,
            SKY,
            JYEFFECT,
            HIRES,
            LOSSLESS,
            EXHIGH,
            HIGHER,
            STANDARD,
        )
    }
}
