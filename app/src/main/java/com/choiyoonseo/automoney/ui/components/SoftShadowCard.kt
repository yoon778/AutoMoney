package com.choiyoonseo.automoney.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.choiyoonseo.automoney.ui.theme.MoneyTheme

@Composable
fun SoftShadowCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MoneyTheme.colors
    val surface = if (colors.isDark) {
        Modifier.border(1.dp, colors.divider, shape)
    } else {
        Modifier.shadow(elevation = 6.dp, shape = shape, clip = false)
    }
    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(surface)
            .clip(shape)
            .background(colors.surface)
            .then(clickMod)
            .padding(18.dp),
        content = content
    )
}
