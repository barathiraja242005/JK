package com.barathiraja.jk.ui.gym

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CardMembership
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.SportsGymnastics
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.barathiraja.jk.gym.Award

/*
 * Award badges: every award is drawn as a medal (a gold, silver or rose disc with a black ring and a line icon,
 * hanging from two red ribbon tails) instead of an emoji, so they match the rest of the app and look the same on every phone. Awards
 * are stored with an emoji; it is only used here to pick the badge.
 */

/** How one award looks: its icon and the metal it sits on. */
internal data class AwardLook(val icon: ImageVector, val fill: Color)

/** Medal discs: fixed light colours so the black icon and ring read the same in light and dark mode. */
private val Gold = Color(0xFFF59E0B)
private val Silver = Color(0xFFE1E3E6)
private val Rose = Color(0xFFFFB4B0)
private val Cream = Color(0xFFFEF3C7)

internal fun awardLook(emoji: String, title: String = ""): AwardLook = when {
    emoji == Award.BEST_MEMBER.emoji || title == Award.BEST_MEMBER.label -> AwardLook(Icons.Outlined.Star, Gold)
    emoji == Award.BEST_TRAINER.emoji || title == Award.BEST_TRAINER.label -> AwardLook(Icons.Outlined.WorkspacePremium, Gold)
    emoji == Award.MOST_CONSISTENT.emoji || title == Award.MOST_CONSISTENT.label -> AwardLook(Icons.Outlined.LocalFireDepartment, Rose)
    emoji == Award.MOST_IMPROVED.emoji || title == Award.MOST_IMPROVED.label -> AwardLook(Icons.AutoMirrored.Outlined.TrendingUp, Silver)
    emoji == Award.IRON_LIFTER.emoji || title == Award.IRON_LIFTER.label -> AwardLook(Icons.Outlined.FitnessCenter, Cream)
    emoji == "⭐" -> AwardLook(Icons.Outlined.AutoAwesome, Gold)
    emoji == "🔄" -> AwardLook(Icons.Outlined.Autorenew, Silver)
    emoji == "📅" -> AwardLook(Icons.Outlined.EventAvailable, Silver)
    else -> AwardLook(Icons.Outlined.MilitaryTech, Rose)
}

internal fun Award.look() = awardLook(emoji, label)

/** Icon for a reward option, keyed by the emoji it is stored with. */
internal fun rewardIcon(emoji: String): ImageVector = when (emoji) {
    "🗓️" -> Icons.Outlined.CalendarMonth
    "🥤" -> Icons.Outlined.LocalDrink
    "🏋️" -> Icons.Outlined.SportsGymnastics
    "👕" -> Icons.Outlined.Checkroom
    "💸" -> Icons.Outlined.Sell
    "📜" -> Icons.Outlined.CardMembership
    "📣" -> Icons.Outlined.Campaign
    "✏️" -> Icons.Outlined.Edit
    "💰" -> Icons.Outlined.Payments
    "🌴" -> Icons.Outlined.BeachAccess
    "🎓" -> Icons.Outlined.School
    "🎁" -> Icons.Outlined.CardGiftcard
    "🍽️" -> Icons.Outlined.Restaurant
    else -> Icons.Outlined.ThumbUp
}

/**
 * A medal [size] wide (and a little taller, for the ribbon). [ribbon] is the tails' colour: brand red.
 */
@Composable
internal fun AwardBadge(look: AwardLook, size: Dp, ribbon: Color = Owner.Red, modifier: Modifier = Modifier) {
    Box(modifier.size(size, size * 1.12f)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width
            val disc = w * 0.84f
            val cx = w / 2
            val cy = disc / 2 + w * 0.02f
            // Two ribbon tails, each ending in a V notch, hanging from behind the disc.
            val tail = w * 0.24f
            fun tailPath(dir: Float) = Path().apply {
                val top = cy + disc * 0.18f
                val x0 = cx + dir * w * 0.04f
                val bottom = this@Canvas.size.height
                val x1 = cx + dir * w * 0.26f
                moveTo(x0, top)
                lineTo(x0 + dir * tail, top)
                lineTo(x1 + dir * tail, bottom)
                lineTo(x1 + dir * tail / 2, bottom - tail * 0.45f)
                lineTo(x1, bottom)
                close()
            }
            drawPath(tailPath(-1f), ribbon)
            drawPath(tailPath(1f), ribbon)
            drawCircle(look.fill, disc / 2, Offset(cx, cy))
            drawCircle(Color.Black, disc / 2 - 1.dp.toPx(), Offset(cx, cy), style = Stroke(2.dp.toPx()))
            drawCircle(Color.Black.copy(alpha = 0.28f), disc / 2 - 6.dp.toPx().coerceAtMost(disc * 0.12f), Offset(cx, cy), style = Stroke(1.dp.toPx()))
        }
        Box(Modifier.size(size * 0.84f).padding(top = size * 0.02f).align(Alignment.TopCenter), contentAlignment = Alignment.Center) {
            Icon(look.icon, null, Modifier.size(size * 0.38f), tint = Color.Black)
        }
    }
}
