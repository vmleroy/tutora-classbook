package app.tutora.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import app.tutora.designsystem.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.tokens.TutoraTokens.Radius
import app.tutora.designsystem.tokens.TutoraTokens.Size
import app.tutora.designsystem.tokens.TutoraTokens.Spacing

enum class ThumbnailSize { MD, LG }

private val ThumbnailSize.side get() = if (this == ThumbnailSize.LG) Size.thumbLg else Size.thumb

/**
 * A comprovante's thumbnail (spec 021): the picture, or the file icon with [kindLabel] ("PDF")
 * while it loads or for a file without one. [onRemove] adds the × in the corner, a full touch
 * target, which takes it off the payment being edited.
 */
@Composable
fun Thumbnail(
    label: String,
    image: Painter?,
    modifier: Modifier = Modifier,
    size: ThumbnailSize = ThumbnailSize.MD,
    kindLabel: String? = null,
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
) {
    val c = TutoraTheme.colors
    Box(modifier.testTag("thumbnail")) {
        Box(
            Modifier
                .size(size.side)
                .clip(RoundedCornerShape(Radius.xl))
                .background(if (image != null) c.surfaceSunken else c.secondarySoft)
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            if (image != null) {
                Image(image, null, Modifier.size(size.side), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.s0_5)) {
                    Icon(IconName.FILE, tint = c.secondaryInk)
                    if (kindLabel != null) Text(kindLabel, style = TutoraTheme.type.badge, color = c.secondaryInk)
                }
            }
        }
        if (onRemove != null) {
            // The × sits on the corner; its press area is a full touch target around it.
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = Size.touch / 2 - Size.iconLg / 2, y = -(Size.touch / 2 - Size.iconLg / 2))
                    .size(Size.touch)
                    .clickable(role = Role.Button, onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(Size.iconLg)
                        .clip(CircleShape)
                        .background(c.ink),
                    contentAlignment = Alignment.Center,
                ) { Icon(IconName.CLOSE, label = stringResource(R.string.ds_remove, label), tint = c.surface, size = Size.iconSm) }
            }
        }
    }
}

/** The dashed tile after the thumbnails that adds another (spec 021). */
@Composable
fun AddThumbnail(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: ThumbnailSize = ThumbnailSize.MD,
) {
    val c = TutoraTheme.colors
    Column(
        modifier
            .size(size.side)
            .clip(RoundedCornerShape(Radius.xl))
            .border(Size.borderStrong, c.lineStrong, RoundedCornerShape(Radius.xl))
            .clickable(role = Role.Button, onClick = onClick)
            .testTag("thumbnail-add"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(IconName.PLUS, tint = c.primary)
        Text(label, style = TutoraTheme.type.badge, color = c.primary)
    }
}

private const val ZOOM_MAX = 5f
private const val ZOOM_DOUBLE_TAP = 2.5f

/**
 * A picture that fills its box and zooms (spec 021's viewer): pinch to zoom up to 5×, drag to
 * move inside its edges, double tap to zoom in or back out.
 */
@Composable
fun ZoomImage(
    image: Painter,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var box by remember { mutableStateOf(IntSize.Zero) }

    fun bounded(o: Offset): Offset {
        val maxX = box.width * (scale - 1) / 2
        val maxY = box.height * (scale - 1) / 2
        return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
    }
    Box(
        modifier
            .onSizeChanged { box = it }
            .testTag("zoom-image")
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, ZOOM_MAX)
                    offset = bounded(offset + pan)
                }
            }.pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    scale = if (scale > 1f) 1f else ZOOM_DOUBLE_TAP
                    offset = bounded(Offset.Zero)
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            image,
            contentDescription,
            Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
            contentScale = ContentScale.Fit,
        )
    }
}
