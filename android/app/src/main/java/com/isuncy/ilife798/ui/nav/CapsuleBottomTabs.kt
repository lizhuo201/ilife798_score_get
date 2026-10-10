package com.isuncy.ilife798.ui.nav

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow

/** 底部导航 tab：路由 + 文案 + 图标 */
data class NavTab(val route: String, val label: String, val icon: ImageVector)

/**
 * 液态玻璃胶囊底部导航。
 *
 * 分层结构（参照 Kyant0/AndroidLiquidGlass 1.0.0 的 LiquidBottomTabs 示例）：
 * 1. 玻璃胶囊容器：对 backdrop（屏幕内容层）做 vibrancy + blur + lens，并铺半透明白色表面；
 * 2. 隐形复制条：把 tab 图标/文案以主色 tint 后录制进 tabsBackdrop（alpha=0，仅用于供玻璃折射）；
 * 3. 选中指示块：采样 combined(屏幕层, tab层)，blur + lens（带色散），弹簧动画滑动到当前 tab 下方。
 *
 * 说明：API 31+ 才有模糊、API 33+ 才有 lens/高光；低版本自动退化为半透明胶囊。
 */
@Composable
fun CapsuleBottomTabs(
    tabs: List<NavTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    if (tabs.isEmpty()) return
    val capsule = RoundedCornerShape(percent = 50)
    val accent = MaterialTheme.colorScheme.primary
    val containerColor = Color.White.copy(alpha = 0.45f)
    val tabsBackdrop = rememberLayerBackdrop()
    val combined = rememberCombinedBackdrop(backdrop, tabsBackdrop)
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.height(64.dp), contentAlignment = Alignment.CenterStart) {
        val slotWidth = (maxWidth - 8.dp) / tabs.size
        val animatedIndex by animateFloatAsState(
            targetValue = selectedIndex.toFloat(),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "capsuleTabIndex"
        )
        val indicatorOffset = with(density) { slotWidth.toPx() * animatedIndex }

        // 1. 玻璃胶囊容器 + 可见的 tab 内容
        Row(
            modifier = Modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { capsule },
                    effects = {
                        vibrancy()
                        blur(8.dp.toPx())
                        lens(24.dp.toPx(), 24.dp.toPx())
                    },
                    onDrawSurface = { drawRect(containerColor) }
                )
                .height(64.dp)
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            TabStrip(
                tabs = tabs,
                selectedIndex = selectedIndex,
                onSelect = onSelect,
                showSelection = true,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. 隐形复制条：仅录制进 tabsBackdrop，供选中指示块折射图标
        TabStrip(
            tabs = tabs,
            selectedIndex = selectedIndex,
            onSelect = {},
            showSelection = true,
            modifier = Modifier
                .alpha(0f)
                .layerBackdrop(tabsBackdrop)
                .height(56.dp)
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .graphicsLayer { colorFilter = ColorFilter.tint(accent) }
        )

        // 3. 选中指示块（玻璃滑块）
        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .graphicsLayer { translationX = indicatorOffset }
                .drawBackdrop(
                    backdrop = combined,
                    shape = { capsule },
                    effects = {
                        blur(8.dp.toPx())
                        lens(10.dp.toPx(), 14.dp.toPx(), chromaticAberration = true)
                    },
                    highlight = { Highlight.Default },
                    innerShadow = { InnerShadow.Default },
                    onDrawBehind = { drawRect(Color.Red, blendMode = BlendMode.Clear) },
                    onDrawSurface = { drawRect(Color.White.copy(alpha = 0.16f)) }
                )
                .height(56.dp)
                .fillMaxWidth(1f / tabs.size)
        )
    }
}

@Composable
private fun TabStrip(
    tabs: List<NavTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    showSelection: Boolean,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        tabs.forEachIndexed { index, tab ->
            val selected = index == selectedIndex
            val tint = if (selected && showSelection) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab,
                        onClick = { onSelect(index) }
                    ),
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    modifier = Modifier.size(20.dp),
                    tint = tint
                )
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = tint
                )
            }
        }
    }
}
