package com.example.weather.feature.weather.presentation

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weather.feature.location.data.store.LocationStore
import com.example.weather.feature.location.domain.model.AdministrativeRegion
import com.example.weather.feature.location.domain.model.SelectedLocation
import com.example.weather.feature.location.domain.repository.LocationRepository
import com.example.weather.feature.weather.domain.model.CurrentWeather
import com.example.weather.feature.weather.domain.model.DailyForecast
import com.example.weather.feature.weather.domain.model.WeatherDataSource
import com.example.weather.feature.weather.domain.repository.WeatherRepository
import kotlinx.coroutines.launch

/** 连接 ViewModel 与无状态界面的路由层，并保存可恢复的 UI 输入与层级状态。 */
@Composable
fun WeatherRoute(
    viewModelKey: String,
    weatherRepository: WeatherRepository,
    locationRepository: LocationRepository,
    locationStore: LocationStore,
    onConfigureApiKey: () -> Unit,
) {
    val weatherViewModel = viewModel(key = viewModelKey) {
        WeatherViewModel(weatherRepository, locationRepository, locationStore)
    }
    val weatherState by weatherViewModel.weatherUiState.collectAsStateWithLifecycle()
    val isRefreshing by weatherViewModel.isRefreshing.collectAsStateWithLifecycle()
    val location by weatherViewModel.location.collectAsStateWithLifecycle()
    val pickerState by weatherViewModel.pickerState.collectAsStateWithLifecycle()

    var savedSearchQuery by rememberSaveable { mutableStateOf("") }
    var savedPathCodes by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var savedPickerVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (savedPickerVisible) {
            weatherViewModel.openPicker(savedPathCodes, savedSearchQuery)
        }
    }
    LaunchedEffect(pickerState.visible, pickerState.path) {
        if (pickerState.visible) {
            savedPathCodes = pickerState.path.map(AdministrativeRegion::adcode)
        }
    }

    WeatherScreen(
        weatherState = weatherState,
        isRefreshing = isRefreshing,
        location = location,
        pickerState = pickerState,
        onRetry = weatherViewModel::retry,
        onNormalRefresh = { weatherViewModel.refreshWeather(forceRefresh = false) },
        onForceRefresh = { weatherViewModel.refreshWeather(forceRefresh = true) },
        onOpenPicker = {
            savedPickerVisible = true
            weatherViewModel.openPicker(savedPathCodes, savedSearchQuery)
        },
        onClosePicker = {
            savedPickerVisible = false
            weatherViewModel.closePicker()
        },
        onRegionClick = { region ->
            if (region.children.isEmpty()) savedPickerVisible = false
            weatherViewModel.onRegionClick(region)
        },
        onLocationClick = { selected ->
            savedPickerVisible = false
            weatherViewModel.selectLocation(selected)
        },
        onToggleFavorite = weatherViewModel::toggleFavorite,
        onBack = {
            if (pickerState.path.isEmpty()) savedPickerVisible = false
            weatherViewModel.onBack()
        },
        onRefreshDistricts = weatherViewModel::refreshDistricts,
        onSearchQueryChange = { query ->
            savedSearchQuery = query
            weatherViewModel.onSearchQueryChange(query)
        },
        onConfigureApiKey = onConfigureApiKey,
    )
}

@Composable
fun WeatherScreen(
    weatherState: WeatherUiState,
    isRefreshing: Boolean,
    location: SelectedLocation,
    pickerState: LocationPickerState,
    onRetry: () -> Unit,
    onNormalRefresh: () -> Unit,
    onForceRefresh: () -> Unit,
    onOpenPicker: () -> Unit,
    onClosePicker: () -> Unit,
    onRegionClick: (AdministrativeRegion) -> Unit,
    onLocationClick: (SelectedLocation) -> Unit,
    onToggleFavorite: (SelectedLocation) -> Unit,
    onBack: () -> Unit,
    onRefreshDistricts: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onConfigureApiKey: () -> Unit,
) {
    val weatherListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val pickerListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val snackbarHostState = remember { SnackbarHostState() }
    val notificationScope = rememberCoroutineScope()
    val currentIsFavorite = pickerState.favoriteLocations.any { it.adcode == location.adcode }
    val forceRefreshWithNotice: () -> Unit = {
        onForceRefresh()
        notificationScope.launch {
            snackbarHostState.showSnackbar(
                message = "已刷新",
                duration = SnackbarDuration.Short,
            )
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (pickerState.visible) {
                    PickerTopBar(pickerState, onBack, onClosePicker, onRefreshDistricts)
                } else {
                    WeatherTopBar(
                        location = location,
                        isFavorite = currentIsFavorite,
                        onOpenPicker = onOpenPicker,
                        onToggleFavorite = { onToggleFavorite(location) },
                        onConfigureApiKey = onConfigureApiKey,
                    )
                }
            },
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (pickerState.visible) {
                    LocationPickerContent(
                        state = pickerState,
                        listState = pickerListState,
                        onRegionClick = onRegionClick,
                        onLocationClick = onLocationClick,
                        onToggleFavorite = onToggleFavorite,
                        onRefreshDistricts = onRefreshDistricts,
                        onSearchQueryChange = onSearchQueryChange,
                    )
                } else {
                    when (weatherState) {
                        WeatherUiState.Idle, WeatherUiState.Loading -> LoadingContent()
                        is WeatherUiState.Error -> ErrorContent(weatherState.message, onRetry)
                        is WeatherUiState.Content -> WeatherContent(
                            state = weatherState,
                            isRefreshing = isRefreshing,
                            listState = weatherListState,
                            onNormalRefresh = onNormalRefresh,
                            onForceRefresh = forceRefreshWithNotice,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeatherTopBar(
    location: SelectedLocation,
    isFavorite: Boolean,
    onOpenPicker: () -> Unit,
    onToggleFavorite: () -> Unit,
    onConfigureApiKey: () -> Unit,
) {
    TopAppBar(
        title = {
            Column {
                Text(location.name, style = MaterialTheme.typography.titleLarge)
                Text(location.fullName, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        actions = {
            TextButton(onClick = onToggleFavorite) { Text(if (isFavorite) "★" else "☆") }
            TextButton(onClick = onOpenPicker) { Text("切换地点") }
            TextButton(onClick = onConfigureApiKey) { Text("API Key") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerTopBar(
    state: LocationPickerState,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onRefresh: () -> Unit,
) {
    TopAppBar(
        title = { Text(state.path.lastOrNull()?.name ?: "选择地点") },
        navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
        actions = {
            TextButton(onClick = onRefresh) { Text("刷新区划") }
            TextButton(onClick = onClose) { Text("关闭") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeatherContent(
    state: WeatherUiState.Content,
    isRefreshing: Boolean,
    listState: LazyListState,
    onNormalRefresh: () -> Unit,
    onForceRefresh: () -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onNormalRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                CurrentWeatherCard(
                    current = state.weather.current,
                    source = state.source,
                    cacheDateKey = state.cacheDateKey,
                    onForceRefresh = onForceRefresh,
                )
            }
            if (state.weather.forecast.days.isNotEmpty()) {
                item { Text("未来 ${state.weather.forecast.days.size} 天", style = MaterialTheme.typography.titleMedium) }
            }
            items(state.weather.forecast.days, key = { it.date }) { DailyForecastRow(it) }
        }
    }
}

@Composable
private fun CurrentWeatherCard(
    current: CurrentWeather,
    source: WeatherDataSource,
    cacheDateKey: String?,
    onForceRefresh: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(conditionEmoji(current.condition), fontSize = 48.sp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(current.condition, style = MaterialTheme.typography.headlineSmall)
                    Text("${current.temperatureCelsius}℃", style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DetailChip("湿度", "${current.humidityPercent}%")
                DetailChip("风向", "${current.windDirection}风")
                DetailChip("风力", "${current.windPower}级")
            }
            Text("上次更新时间：${current.observedAt}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = cacheStatusText(source, cacheDateKey),
                style = MaterialTheme.typography.labelMedium,
                color = if (source == WeatherDataSource.STALE_CACHE) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.tertiary
                },
            )
            OutlinedButton(onClick = onForceRefresh) { Text("刷新") }
        }
    }
}

@Composable
private fun LocationPickerContent(
    state: LocationPickerState,
    listState: LazyListState,
    onRegionClick: (AdministrativeRegion) -> Unit,
    onLocationClick: (SelectedLocation) -> Unit,
    onToggleFavorite: (SelectedLocation) -> Unit,
    onRefreshDistricts: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            label = { Text("搜索地点") },
            placeholder = { Text("支持中文、拼音全拼或首字母") },
            singleLine = true,
        )

        when {
            state.loading -> LoadingContent()
            state.error != null -> ErrorContent(state.error, onRefreshDistricts)
            state.searchQuery.isNotBlank() -> LocationResultsList(
                title = "搜索结果",
                locations = state.searchResults,
                favorites = state.favoriteLocations,
                listState = listState,
                onLocationClick = onLocationClick,
                onToggleFavorite = onToggleFavorite,
            )
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                if (state.favoriteLocations.isNotEmpty()) {
                    item { QuickLocationSection("收藏地点", state.favoriteLocations, onLocationClick) }
                }
                if (state.recentLocations.isNotEmpty()) {
                    item { QuickLocationSection("最近使用", state.recentLocations, onLocationClick) }
                }
                item {
                    Text(
                        text = state.path.lastOrNull()?.let { "${it.name}下级地区" } ?: "按行政区选择",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                if (state.items.isEmpty()) {
                    item { Text("没有可用的地点", modifier = Modifier.padding(20.dp)) }
                } else {
                    items(state.items, key = { it.adcode }) { region ->
                        RegionRow(region) { onRegionClick(region) }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickLocationSection(
    title: String,
    locations: List<SelectedLocation>,
    onLocationClick: (SelectedLocation) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, modifier = Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.titleSmall)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(locations, key = { it.adcode }) { location ->
                OutlinedButton(onClick = { onLocationClick(location) }) { Text(location.name) }
            }
        }
    }
}

@Composable
private fun LocationResultsList(
    title: String,
    locations: List<SelectedLocation>,
    favorites: List<SelectedLocation>,
    listState: LazyListState,
    onLocationClick: (SelectedLocation) -> Unit,
    onToggleFavorite: (SelectedLocation) -> Unit,
) {
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        item { Text(title, modifier = Modifier.padding(20.dp), style = MaterialTheme.typography.titleMedium) }
        if (locations.isEmpty()) {
            item { Text("没有匹配的地点", modifier = Modifier.padding(20.dp)) }
        }
        items(locations, key = { it.adcode }) { location ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onLocationClick(location) }
                    .padding(start = 20.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(location.name, style = MaterialTheme.typography.bodyLarge)
                    Text(location.fullName, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { onToggleFavorite(location) }) {
                    Text(if (favorites.any { it.adcode == location.adcode }) "★" else "☆")
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("重试") }
    }
}

@Composable
private fun DetailChip(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DailyForecastRow(day: DailyForecast) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(day.date, style = MaterialTheme.typography.bodyMedium)
                Text("周${weekLabel(day.week)}", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(conditionEmoji(day.dayCondition), fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("白天 ${day.dayCondition}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("夜间 ${day.nightCondition}", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(12.dp))
            Text("${day.nightTemperatureCelsius}~${day.dayTemperatureCelsius}℃",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RegionRow(region: AdministrativeRegion, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(region.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (region.children.isNotEmpty()) {
            Text("›", style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun cacheStatusText(source: WeatherDataSource, cacheDateKey: String?): String = when (source) {
    WeatherDataSource.NETWORK -> "已从网络更新"
    WeatherDataSource.TODAY_CACHE -> "今日已缓存"
    WeatherDataSource.STALE_CACHE ->
        "当前离线 · 显示 ${cacheDateKey.toDisplayDate()} 的最近缓存"
}

private fun String?.toDisplayDate(): String {
    if (this == null || length != 8) return "未知日期"
    return "${substring(0, 4)}-${substring(4, 6)}-${substring(6, 8)}"
}

private fun conditionEmoji(condition: String): String = when {
    condition.contains("晴") -> "☀️"
    condition.contains("云") -> "⛅"
    condition.contains("雨") -> "🌧️"
    condition.contains("雪") -> "❄️"
    condition.contains("雾") || condition.contains("霾") -> "🌫️"
    condition.contains("风") -> "💨"
    else -> "🌡️"
}

private fun weekLabel(week: String): String = when (week.trim()) {
    "1" -> "一"; "2" -> "二"; "3" -> "三"; "4" -> "四"
    "5" -> "五"; "6" -> "六"; "7" -> "日"; else -> week
}
