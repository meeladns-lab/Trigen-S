package com.example.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.example.ui.i18n.Bidi

/**
 * Text composable with guaranteed Bidi isolation for IP addresses, ports, URLs, and numbers.
 * Prevents scrambled display (e.g. 104.16.0.1:443 becoming 443:104.16.0.1) in Persian RTL layout.
 */
@Composable
fun BidiText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE
) {
    Text(
        text = Bidi.isolate(text),
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        style = style,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines
    )
}
