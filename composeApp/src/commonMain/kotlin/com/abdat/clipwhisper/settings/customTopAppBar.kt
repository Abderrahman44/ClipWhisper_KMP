package com.abdat.clipwhisper.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandTopAppBar(
    title: @Composable () -> Unit,
    themeColor: Long,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable () -> Unit = {}
) {
    val brush = remember(themeColor) { BrandThemePresets.brushForStoredLong(themeColor) }
    val onColor = remember(themeColor) { BrandThemePresets.onTopBarColor(themeColor) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(brush)
    ) {
        TopAppBar(
            title = title,
            navigationIcon = navigationIcon,
            actions = { actions() },
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = onColor,
                actionIconContentColor = onColor,
                navigationIconContentColor = onColor
            )
        )
    }
}
