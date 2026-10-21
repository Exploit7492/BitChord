package com.music.bitchord.ui.tv.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.bitchord.campaign.CampaignCard
import com.music.bitchord.campaign.Campaigns
import com.music.bitchord.ui.tv.theme.TvDimensions
import com.music.bitchord.ui.tv.theme.TvGlass
import com.music.bitchord.ui.tv.theme.TvType
import kotlinx.coroutines.delay

/** How often a visible banner re-reads its gathered stream total. */
private const val TV_CAMPAIGN_TOTAL_REFRESH_MS = 15_000L

/** The gold the phone's own "Promoted" label uses. */
private val PROMOTED_GOLD = Color(0xFFD4AF37)

/** The LIVE pill's red. */
private val LIVE_RED = Color(0xFFE53935)

/**
 * The 10-foot promoted-campaign banner.
 *
 * The phone's [com.music.bitchord.ui.components.CampaignBanner] is a compact
 * portrait card; at ten feet that reads as a stray tile rather than a promotion,
 * so this is the same campaign drawn as a full-width billboard under the tab
 * bar: the campaign's own artwork, blurred, behind its title, artist and
 * gathered total, with the tvOS focus lift the rest of this surface uses.
 *
 * Draws nothing when there is no live campaign — the caller passes null — so a
 * TV with no campaign shows its ordinary shelves rather than an empty band.
 *
 * The total is re-read on a slow timer while the banner is on screen, because
 * the value handed in was a snapshot taken when the feed loaded. The read is
 * keyed on the campaign id so it restarts for the next campaign and stops for
 * one that has left the screen.
 */
@Composable
fun TvCampaignBanner(
    campaign: CampaignCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var liveTotal by remember(campaign.id) { mutableLongStateOf(campaign.totalStreams) }
    LaunchedEffect(campaign.id) {
        while (true) {
            Campaigns.campaignTotalStreams(campaign.id)?.let { if (it > 0L) liveTotal = it }
            delay(TV_CAMPAIGN_TOTAL_REFRESH_MS)
        }
    }

    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TvDimensions.SafeMarginHorizontal)
            .tvLift(interaction, shape, focusedScale = 1.03f, elevation = 18.dp)
            .tvClick(interaction, onClick = onClick)
            .clip(shape),
    ) {
        // Ambient art: the campaign's own thumbnail, blurred to fill the band.
        if (campaign.thumbnailUrl.isNotBlank()) {
            AsyncImage(
                model = campaign.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize().blur(30.dp),
            )
        }
        // A left-to-right scrim, so the type stays legible over any artwork and
        // the band still reads as one object at the edge of the panel.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.88f),
                            Color.Black.copy(alpha = 0.62f),
                            Color.Black.copy(alpha = 0.30f),
                        ),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp)
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TvArtwork(
                url = campaign.thumbnailUrl.takeIf { it.isNotBlank() },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(72.dp),
            )
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Promoted",
                    style = TvType.Caption,
                    color = PROMOTED_GOLD,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = campaign.title,
                    style = TvType.Headline,
                    color = TvGlass.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = campaign.artist,
                    style = TvType.Body,
                    color = TvGlass.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // Gated on the total, so a campaign that has gathered nothing shows
            // no figure rather than a "0" that reads as broken.
            if (liveTotal > 0L) {
                Spacer(Modifier.width(20.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCampaignTotal(liveTotal),
                        style = TvType.Headline,
                        color = TvGlass.TextPrimary,
                        maxLines = 1,
                    )
                    Text(
                        text = "streams",
                        style = TvType.Caption,
                        color = TvGlass.TextTertiary,
                        maxLines = 1,
                    )
                }
            }
            if (campaign.isLive) {
                Spacer(Modifier.width(18.dp))
                TvLivePill()
            }
        }
    }
}

/** A small pulsing "LIVE" pill — a UI animation, not a data claim. */
@Composable
private fun TvLivePill() {
    val transition = rememberInfiniteTransition(label = "tvLiveDotPulse")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "tvLiveDotAlpha",
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(LIVE_RED.copy(alpha = 0.5f))
            .border(1.dp, LIVE_RED.copy(alpha = 0.7f), RoundedCornerShape(7.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = alpha)),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = "LIVE",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** "341K" / "1.2M" — a compact count for a band with room for four characters. */
private fun formatCampaignTotal(total: Long): String = when {
    total >= 1_000_000_000L -> "%.1fB".format(total / 1_000_000_000.0)
    total >= 1_000_000L -> "%.1fM".format(total / 1_000_000.0)
    total >= 1_000L -> "%.0fK".format(total / 1_000.0)
    else -> total.toString()
}
