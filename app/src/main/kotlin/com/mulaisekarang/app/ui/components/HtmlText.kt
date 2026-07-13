package com.mulaisekarang.app.ui.components

import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.widget.TextView
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat

@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
) {
    val resolvedColor = (
        if (color.isUnspecified) {
            style.color.takeIf { it.isSpecified } ?: MaterialTheme.colorScheme.onSurface
        } else {
            color
        }
    ).toArgb()
    val textSizeSp = style.fontSize.value

    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                setTextColor(resolvedColor)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
                setLineSpacing(0f, 1.6f)
                movementMethod = LinkMovementMethod.getInstance()
            }
        },
        update = { textView ->
            textView.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
            textView.setTextColor(resolvedColor)
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
            textView.setLineSpacing(0f, 1.6f)
        },
    )
}

private val BOLD_OPEN = Regex("<(strong|b)>", RegexOption.IGNORE_CASE)
private val BOLD_CLOSE = Regex("</(strong|b)>", RegexOption.IGNORE_CASE)
private val HEADING_OPEN = Regex("<(h[1-4])>", RegexOption.IGNORE_CASE)
private val HEADING_CLOSE = Regex("</(h[1-4])>", RegexOption.IGNORE_CASE)

fun accentHtml(html: String, accentColor: Color): String {
    val hex = String.format("#%06X", 0xFFFFFF and accentColor.toArgb())
    return html
        .let { BOLD_OPEN.replace(it) { m -> "<${m.groupValues[1]}><font color=\"$hex\">" } }
        .let { BOLD_CLOSE.replace(it) { m -> "</font></${m.groupValues[1]}>" } }
        .let { HEADING_OPEN.replace(it) { m -> "<${m.groupValues[1]}><font color=\"$hex\">" } }
        .let { HEADING_CLOSE.replace(it) { m -> "</font></${m.groupValues[1]}>" } }
}
