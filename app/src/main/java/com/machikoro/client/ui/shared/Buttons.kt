package com.machikoro.client.ui.shared

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.machikoro.client.ui.theme.ButtonBeigeDark
import com.machikoro.client.ui.theme.ButtonBeigeLight
import com.machikoro.client.ui.theme.ButtonColor
import com.machikoro.client.ui.theme.ButtonTextColor
import com.machikoro.client.ui.theme.ClientTheme
import com.machikoro.client.ui.theme.OrangeButtonShadowColor
import com.machikoro.client.ui.theme.TextBlueDark

private val BUTTONS_ICON_SPACER = 12.dp
@Composable
fun ActionButton(
    label: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes leftIcon: Int? = null,
    @DrawableRes rightIcon: Int? = null
) {
    Box(
        modifier = modifier
            .wrapContentSize()
    ) {

    // Bottom shadow
    Box(
        modifier = Modifier
            .matchParentSize()
            .offset(y = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(color = OrangeButtonShadowColor)
    )

        // Main button
        Button(
            onClick = { onClick?.invoke() },
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ButtonColor
            ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left image
                leftIcon?.let {
                    Image(
                        painter = painterResource(id = it),
                        modifier = Modifier.size(30.dp),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(BUTTONS_ICON_SPACER))
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ButtonTextColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                // Right image
                rightIcon?.let {
                    Spacer(modifier = Modifier.width(BUTTONS_ICON_SPACER))
                    Image(
                        painter = painterResource(id = it),
                        modifier = Modifier.size(30.dp),
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
fun SecondaryActionButton(
    label: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes leftIcon: Int? = null,
    @DrawableRes rightIcon: Int? = null
) {
    Box(
        modifier = modifier.wrapContentSize()
    ) {
        // Bottom shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ButtonBeigeDark)
        )

        // Main button
        Button(
            onClick = { onClick?.invoke() },
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ButtonBeigeLight,
                contentColor = TextBlueDark,
                disabledContainerColor = ButtonBeigeLight.copy(alpha = 0.7f),
                disabledContentColor = TextBlueDark.copy(alpha = 0.7f)
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left image
                leftIcon?.let {
                    Image(
                        painter = painterResource(id = it),
                        modifier = Modifier.size(30.dp),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(BUTTONS_ICON_SPACER))
                }

                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextBlueDark,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                // Right image
                rightIcon?.let {
                    Spacer(modifier = Modifier.width(BUTTONS_ICON_SPACER))
                    Image(
                        painter = painterResource(id = it),
                        modifier = Modifier.size(30.dp),
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ButtonsPreview() {
    ClientTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Other staff on the top:
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ActionButton("Some text", null)
                ActionButton("Some text but long", null)
                SecondaryActionButton("text", null)
            }
        }
    }
}