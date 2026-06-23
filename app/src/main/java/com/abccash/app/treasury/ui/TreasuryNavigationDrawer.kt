package com.abccash.app.treasury.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.R
import com.abccash.app.ui.theme.AppColors

data class DrawerMenuEntry(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun DrawerMenuIconButton(
    onClick: () -> Unit,
    tint: Color = AppColors.BrandBlue,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            Icons.Default.Menu,
            contentDescription = stringResource(R.string.nav_menu),
            tint = tint
        )
    }
}

@Composable
fun TreasuryNavigationDrawerContent(
    companyName: String,
    userName: String,
    items: List<DrawerMenuEntry>,
    onClose: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = Color.White,
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = companyName.ifBlank { stringResource(R.string.app_name) },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                if (userName.isNotBlank()) {
                    Text(
                        text = userName,
                        fontSize = 13.sp,
                        color = AppColors.TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            HorizontalDivider(color = AppColors.Border)

            items.forEach { item ->
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            item.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    label = {
                        Column {
                            Text(
                                text = stringResource(item.titleRes),
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp
                            )
                            Text(
                                text = stringResource(item.subtitleRes),
                                fontSize = 12.sp,
                                color = AppColors.TextSecondary,
                                lineHeight = 14.sp
                            )
                        }
                    },
                    selected = false,
                    onClick = {
                        onClose()
                        item.onClick()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.White
                    )
                )
            }
        }
    }
}
