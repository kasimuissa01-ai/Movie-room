package com.example.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CineBlack
import com.example.ui.theme.CineCardBorder
import com.example.ui.theme.CineRedPrimary
import com.example.ui.theme.CineSurface
import com.example.ui.theme.CineTextMuted
import com.example.ui.theme.CineTextPrimary

enum class CineNavTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "home"),
    SEARCH("Search", Icons.Filled.Search, Icons.Outlined.Search, "search"),
    MY_LIST("My List", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder, "my_list"),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person, "profile")
}

@Composable
fun CineBottomBar(
    currentTab: CineNavTab,
    onTabSelected: (CineNavTab) -> Unit,
    modifier: Modifier = Modifier,
    watchListCount: Int = 0
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cine_bottom_navigation"),
        color = CineSurface.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, CineCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CineNavTab.entries.forEach { tab ->
                val isSelected = tab == currentTab
                val iconTint by animateColorAsState(
                    if (isSelected) CineRedPrimary else CineTextMuted,
                    label = "tab_color"
                )
                val scale by animateFloatAsState(
                    if (isSelected) 1.08f else 1.0f,
                    label = "tab_scale"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(tab) }
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("nav_tab_${tab.route}")
                ) {
                    BadgedBox(
                        badge = {
                            if (tab == CineNavTab.MY_LIST && watchListCount > 0) {
                                Badge(
                                    containerColor = CineRedPrimary,
                                    contentColor = Color.White
                                ) {
                                    Text(text = "$watchListCount", fontSize = 10.sp)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.title,
                            tint = iconTint,
                            modifier = Modifier
                                .size(24.dp)
                                .scale(scale)
                        )
                    }

                    Text(
                        text = tab.title,
                        color = if (isSelected) CineTextPrimary else CineTextMuted,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    // Active Tab Subtle Pill Indicator
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(width = 16.dp, height = 2.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) CineRedPrimary else Color.Transparent)
                    )
                }
            }
        }
    }
}
