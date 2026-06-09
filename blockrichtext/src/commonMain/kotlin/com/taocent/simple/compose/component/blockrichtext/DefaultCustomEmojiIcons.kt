package com.taocent.simple.compose.component.blockrichtext

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

/**
 * :blockrichtext UI 模块提供的默认 CustomEmoji 图标映射,
 * 通过 Lucide 图标库绑定表情 id → ImageVector。
 *
 * 与 [com.taocent.simple.compose.component.richtext.core.DefaultCustomEmojis] 配合使用。
 *
 * 注意:这是 :blockrichtext 独立维护的副本(不依赖 :richtext 模块),与
 * [com.taocent.simple.compose.component.richtext.DefaultCustomEmojiIcons] 内容一致。
 * 后续如需统一,可下沉到 :richtext-core(但需 :richtext-core 引入 lucide 依赖)。
 */
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
    "coffee" to Lucide.Coffee,
    "code" to Lucide.Code,
    "flag" to Lucide.Flag,
    "mail" to Lucide.Mail,
    "file" to Lucide.File,
    "clock" to Lucide.Clock,
    "rocket" to Lucide.Rocket,
    "house" to Lucide.House,
    "lock" to Lucide.Lock,
    "eye" to Lucide.Eye,
    "flashlight" to Lucide.Flashlight,
    "flower" to Lucide.Flower2,
    "tree" to Lucide.TreePine,
    "umbrella" to Lucide.Umbrella,
)
