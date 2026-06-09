package com.taocent.simple.compose.component.blockrichtext.internal.emoji

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.Camera
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.Cloud
import com.composables.icons.lucide.Code
import com.composables.icons.lucide.Coffee
import com.composables.icons.lucide.Crown
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.File
import com.composables.icons.lucide.Flag
import com.composables.icons.lucide.Flame
import com.composables.icons.lucide.Flashlight
import com.composables.icons.lucide.Flower2
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Lightbulb
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Mail
import com.composables.icons.lucide.Moon
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Rocket
import com.composables.icons.lucide.Star
import com.composables.icons.lucide.Sun
import com.composables.icons.lucide.ThumbsUp
import com.composables.icons.lucide.TreePine
import com.composables.icons.lucide.Trophy
import com.composables.icons.lucide.Umbrella
import com.composables.icons.lucide.Zap

@Immutable
data class CustomEmoji(
    val id: String,
    val name: String
)

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

val DefaultCustomEmojiIcons: Map<String, ImageVector> = mapOf(
    "heart" to Lucide.Heart,
    "star" to Lucide.Star,
    "thumbsup" to Lucide.ThumbsUp,
    "fire" to Lucide.Flame,
    "check" to Lucide.Check,
    "bolt" to Lucide.Zap,
    "rocket" to Lucide.Rocket,
    "trophy" to Lucide.Trophy,
    "crown" to Lucide.Crown,
    "lightbulb" to Lucide.Lightbulb,
    "bell" to Lucide.Bell,
    "camera" to Lucide.Camera,
    "music" to Lucide.Music,
    "moon" to Lucide.Moon,
    "sun" to Lucide.Sun,
    "cloud" to Lucide.Cloud,
    "home" to Lucide.House,
    "mail" to Lucide.Mail,
    "lock" to Lucide.Lock,
    "eye" to Lucide.Eye,
    "code" to Lucide.Code,
    "coffee" to Lucide.Coffee,
    "flag" to Lucide.Flag,
    "file" to Lucide.File,
    "clock" to Lucide.Clock,
    "flower" to Lucide.Flower2,
    "tree" to Lucide.TreePine,
    "umbrella" to Lucide.Umbrella,
    "flashlight" to Lucide.Flashlight,
    "rocket2" to Lucide.Rocket,
)

const val CUSTOM_EMOJI_TAG = "CUSTOM_EMOJI"
const val CUSTOM_EMOJI_PLACEHOLDER = "\u2588"
