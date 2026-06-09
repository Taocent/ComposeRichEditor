package com.taocent.simple.compose.component.richtext.core.internal.emoji

import androidx.compose.runtime.Immutable

/**
 * 自定义表情数据模型 — 不依赖任何具体图标库,
 * 在 UI 模块中可以通过 [ImageVector] 映射为具体图标。
 */
@Immutable
data class CustomEmoji(
    val id: String,
    val name: String
)

/** 默认自定义表情列表 */
val DefaultCustomEmojis = listOf(
    CustomEmoji("heart", "爱心"),
    CustomEmoji("star", "星星"),
    CustomEmoji("thumbsup", "点赞"),
    CustomEmoji("fire", "火焰"),
    CustomEmoji("check", "对勾"),
    CustomEmoji("bolt", "闪电"),
    CustomEmoji("rocket", "火箭"),
    CustomEmoji("trophy", "奖杯"),
    CustomEmoji("crown", "皇冠"),
    CustomEmoji("lightbulb", "灯泡"),
    CustomEmoji("bell", "铃铛"),
    CustomEmoji("camera", "相机"),
    CustomEmoji("music", "音乐"),
    CustomEmoji("moon", "月亮"),
    CustomEmoji("sun", "太阳"),
    CustomEmoji("cloud", "云朵"),
    CustomEmoji("home", "首页"),
    CustomEmoji("mail", "邮件"),
    CustomEmoji("lock", "锁定"),
    CustomEmoji("eye", "眼睛"),
    CustomEmoji("code", "代码"),
    CustomEmoji("coffee", "咖啡"),
    CustomEmoji("flag", "旗帜"),
    CustomEmoji("file", "文件"),
    CustomEmoji("clock", "时钟"),
    CustomEmoji("flower", "花朵"),
    CustomEmoji("tree", "树木"),
    CustomEmoji("umbrella", "雨伞"),
    CustomEmoji("flashlight", "手电"),
    CustomEmoji("rocket2", "火箭2"),
)

const val CUSTOM_EMOJI_TAG = "CUSTOM_EMOJI"
const val CUSTOM_EMOJI_PLACEHOLDER = "\u2588"
