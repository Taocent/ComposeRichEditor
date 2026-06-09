package com.taocent.simple.compose.component.richtext

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
 * :richtext UI 模块提供的默认 CustomEmoji 图标映射,
 * 通过 Lucide 图标库绑定表情 id → ImageVector。
 *
 * 该 val 与 [com.taocent.simple.compose.component.richtext.core.DefaultCustomEmojis] 配合使用。
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
