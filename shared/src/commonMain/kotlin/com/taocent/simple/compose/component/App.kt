package com.taocent.simple.compose.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.window.core.layout.WindowSizeClass
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.CalendarRange
import com.composables.icons.lucide.LayoutList
import com.taocent.simple.compose.component.testscreens.TestBlockRichTextEditor
import com.taocent.simple.compose.component.testscreens.TestComponentScreen
import com.taocent.simple.compose.component.testscreens.TestRichTextEditor
import kotlinx.serialization.Serializable

data class ComponentItem(
    val name: String,
    val description: String,
    val status: ComponentStatus,
    val screen: @Composable (onBack: () -> Unit, showTopBar: Boolean) -> Unit
)

enum class ComponentStatus(val displayName: String) {
    Completed("已完成"),
    InProgress("开发中"),
    Planned("计划中")
}

val sampleComponents = listOf(
    ComponentItem(
        name = "富文本编辑器",
        description = "不依赖 Markdown 的跨平台富文本编辑组件，支持加粗、斜体、下划线、颜色和字号设置",
        status = ComponentStatus.Completed,
        screen = { onBack, showTopBar -> TestRichTextEditor(onBack = onBack, showTopBar = showTopBar) }
    ),
    ComponentItem(
        name = "块级富文本编辑器",
        description = "支持多文本块的块级富文本编辑组件，每块独立编辑，撤销重做跨块全局管理",
        status = ComponentStatus.InProgress,
        screen = { onBack, showTopBar -> TestBlockRichTextEditor(onBack = onBack, showTopBar = showTopBar) }
    ),
    ComponentItem(
        name = "示例组件",
        description = "这是一个占位示例组件，展示组件列表的基本结构",
        status = ComponentStatus.Completed,
        screen = { onBack, showTopBar -> TestComponentScreen(onBack = onBack, showTopBar = showTopBar) }
    )
)

@Serializable
data object HomeKey : NavKey

@Serializable
data class ComponentDetailKey(val componentName: String) : NavKey

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun App() {
    MaterialTheme {
        var selectedComponent by remember { mutableStateOf<ComponentItem?>(null) }

        val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
        val isExpanded = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

        if (isExpanded) {
            AdaptiveListDetailLayout(
                components = sampleComponents,
                selectedComponent = selectedComponent,
                onComponentClick = { selectedComponent = it }
            )
        } else {
            CompactNavigationLayout(
                components = sampleComponents,
                selectedComponent = selectedComponent,
                onComponentClick = { selectedComponent = it },
                onBack = { selectedComponent = null }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun AdaptiveListDetailLayout(
    components: List<ComponentItem>,
    selectedComponent: ComponentItem?,
    onComponentClick: (ComponentItem) -> Unit
) {
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val directive = calculatePaneScaffoldDirective(adaptiveInfo)
    val navigator = rememberListDetailPaneScaffoldNavigator<ComponentItem>(
        scaffoldDirective = directive
    )

    LaunchedEffect(selectedComponent) {
        if (selectedComponent != null) {
            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, selectedComponent)
        }
    }

    ListDetailPaneScaffold(
        directive = directive,
        scaffoldState = navigator.scaffoldState,
        listPane = {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Compose 组件") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ComponentListPane(
                    components = components,
                    selectedComponent = selectedComponent,
                    onComponentClick = onComponentClick
                )
            }
        },
        detailPane = {
            val detailComponent = navigator.currentDestination?.contentKey
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Text(detailComponent?.name ?: "Compose 组件")
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                if (detailComponent != null) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        detailComponent.screen({}, false)
                    }
                } else {
                    DetailPlaceholder()
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactNavigationLayout(
    components: List<ComponentItem>,
    selectedComponent: ComponentItem?,
    onComponentClick: (ComponentItem) -> Unit,
    onBack: () -> Unit
) {
    if (selectedComponent != null) {
        selectedComponent.screen(onBack, true)
    } else {
        HomeScreen(
            components = components,
            onComponentClick = onComponentClick
        )
    }
}

@Composable
private fun ComponentListPane(
    components: List<ComponentItem>,
    selectedComponent: ComponentItem?,
    onComponentClick: (ComponentItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(components, key = { _, item -> item.name }) { _, component ->
            ComponentCard(
                component = component,
                isSelected = selectedComponent?.name == component.name,
                onClick = { onComponentClick(component) }
            )
        }
    }
}

@Composable
private fun DetailPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                Lucide.LayoutList,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "选择一个组件查看详情",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    components: List<ComponentItem>,
    onComponentClick: (ComponentItem) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compose 组件") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        if (components.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "暂无组件",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "开始开发你的第一个组件吧",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(components, key = { _, item -> item.name }) { _, component ->
                    ComponentCard(
                        component = component,
                        isSelected = false,
                        onClick = { onComponentClick(component) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ComponentCard(
    component: ComponentItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 1.dp else 2.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .then(Modifier.padding(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = component.name.first().uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = component.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = component.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            StatusBadge(status = component.status)
        }
    }
}

@Composable
private fun StatusBadge(status: ComponentStatus) {
    val containerColor = when (status) {
        ComponentStatus.Completed -> MaterialTheme.colorScheme.primaryContainer
        ComponentStatus.InProgress -> MaterialTheme.colorScheme.tertiaryContainer
        ComponentStatus.Planned -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (status) {
        ComponentStatus.Completed -> MaterialTheme.colorScheme.onPrimaryContainer
        ComponentStatus.InProgress -> MaterialTheme.colorScheme.onTertiaryContainer
        ComponentStatus.Planned -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Text(
            text = status.displayName,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    title: String,
    onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Image(
                            Lucide.ArrowLeft,
                            contentDescription = "返回",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        content(padding)
    }
}
