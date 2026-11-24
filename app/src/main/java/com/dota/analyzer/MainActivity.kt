package com.dota.analyzer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dota.analyzer.data.Match
import com.dota.analyzer.data.MatchAnalytics
import com.dota.analyzer.data.Player
import com.dota.analyzer.data.PlayerImpact
import com.dota.analyzer.ui.MatchState
import com.dota.analyzer.ui.MatchViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private val Bg = Color(0xFF0D1118)
private val Panel = Color(0xFF171E29)
private val Accent = Color(0xFFB58B45)
private val Muted = Color(0xFF8995A7)
private val Radiant = Color(0xFF79C99A)
private val Dire = Color(0xFFE58A8A)
private val GoldLine = Color(0xFFF0C36A)
private val XpLine = Color(0xFF78A9E8)

private enum class MatchTab(val title: String) {
    OVERVIEW("Обзор"), PLAYERS("Игроки"), ANALYSIS("Анализ"), ECONOMY("Экономика"), ITEMS("Предметы")
}

class MainActivity : ComponentActivity() {
    private val viewModel: MatchViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DotaAnalyzerApp(viewModel) }
    }
}

@Composable
fun DotaAnalyzerApp(vm: MatchViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    var matchId by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(MatchTab.OVERVIEW) }
    var showSearchPanel by remember { mutableStateOf(true) }

    LaunchedEffect(state) {
        if (state is MatchState.Success) showSearchPanel = false
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Accent, background = Bg, surface = Panel,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            Column(Modifier.fillMaxSize()) {
                when (val current = state) {
                    is MatchState.Success -> {
                        if (showSearchPanel) {
                            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                                Text("DOTA ANALYZER", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                                Text("Найти другой матч", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = matchId,
                                        onValueChange = { matchId = it.filter(Char::isDigit) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        label = { Text("ID матча") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Accent, unfocusedBorderColor = Color(0xFF354052),
                                            focusedLabelColor = Accent, cursorColor = Accent
                                        )
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Button(onClick = { vm.loadMatch(matchId) }, shape = RoundedCornerShape(14.dp)) {
                                        Icon(Icons.Default.Search, contentDescription = "Найти")
                                    }
                                }
                                TextButton(onClick = { showSearchPanel = false }) {
                                    Text("Вернуться к матчу #\${current.match.matchId}", color = Accent)
                                }
                            }
                        } else {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("DOTA ANALYZER", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                                    Text("Матч #\${current.match.matchId}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                                TextButton(onClick = { showSearchPanel = true }) {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = Accent)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Другой матч", color = Accent, fontSize = 12.sp)
                                }
                            }
                        }
                        if (!showSearchPanel) {
                            MatchWorkspace(
                                match = current.match,
                                heroNames = current.heroNames,
                                heroImageNames = current.heroImageNames,
                                selectedTab = selectedTab,
                                onTabSelected = { selectedTab = it },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    else -> {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                            Spacer(Modifier.height(30.dp))
                            Text("DOTA ANALYZER", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                            Text("Разбор матча", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            Text("Статистика и аналитика Dota 2", color = Muted, fontSize = 12.sp)
                            Spacer(Modifier.height(18.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = matchId,
                                    onValueChange = { matchId = it.filter(Char::isDigit) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    label = { Text("ID матча") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Accent, unfocusedBorderColor = Color(0xFF354052),
                                        focusedLabelColor = Accent, cursorColor = Accent
                                    )
                                )
                                Spacer(Modifier.width(10.dp))
                                Button(onClick = { vm.loadMatch(matchId) }, shape = RoundedCornerShape(14.dp)) {
                                    Icon(Icons.Default.Search, contentDescription = "Найти матч")
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                        when (current) {
                            MatchState.Idle -> Box(Modifier.weight(1f).fillMaxWidth().padding(20.dp), contentAlignment = Alignment.TopCenter) { EmptyState() }
                            MatchState.Loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Accent) }
                            is MatchState.Error -> Box(Modifier.weight(1f).fillMaxWidth().padding(20.dp), contentAlignment = Alignment.TopCenter) {
                                CardBlock {
                                    Text("Не удалось загрузить", color = Color(0xFFFF8585), fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(6.dp))
                                    Text(current.message, color = Muted)
                                }
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    CardBlock {
        Text("Начните с ID матча", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        Spacer(Modifier.height(6.dp))
        Text("Введите номер матча Dota 2. После загрузки статистика будет распределена по отдельным разделам.", color = Muted, lineHeight = 20.sp)
    }
}

@Composable
private fun MatchWorkspace(
    match: Match,
    heroNames: Map<Int, String>,
    heroImageNames: Map<Int, String>,
    selectedTab: MatchTab,
    onTabSelected: (MatchTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val impacts = remember(match) { MatchAnalytics.analyze(match.players) }
    Column(modifier.fillMaxWidth()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                MatchTab.OVERVIEW -> OverviewScreen(match, heroNames, heroImageNames, impacts)
                MatchTab.PLAYERS -> PlayersScreen(match, heroNames, heroImageNames, impacts)
                MatchTab.ANALYSIS -> AnalysisScreen(match, heroNames, heroImageNames, impacts)
                MatchTab.ECONOMY -> EconomyScreen(match, heroNames, heroImageNames, impacts)
                MatchTab.ITEMS -> ItemsScreen(match, heroNames, heroImageNames)
            }
        }
        NavigationBar(containerColor = Bg, tonalElevation = 0.dp) {
            MatchTab.values().forEach { tab ->
                val icon = when (tab) {
                    MatchTab.OVERVIEW -> Icons.Default.Home
                    MatchTab.PLAYERS -> Icons.Default.Groups
                    MatchTab.ANALYSIS -> Icons.Default.Analytics
                    MatchTab.ECONOMY -> Icons.Default.MonetizationOn
                    MatchTab.ITEMS -> Icons.Default.Inventory2
                }
                NavigationBarItem(
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    icon = { Icon(icon, contentDescription = tab.title) },
                    label = { Text(tab.title, fontSize = 9.sp, maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Accent,
                        selectedTextColor = Accent,
                        indicatorColor = Panel,
                        unselectedIconColor = Muted,
                        unselectedTextColor = Muted
                    )
                )
            }
        }
    }
}

@Composable
private fun OverviewScreen(match: Match, heroNames: Map<Int, String>, heroImageNames: Map<Int, String>, impacts: List<PlayerImpact>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)
    ) {
        item {
            CardBlock {
                Text("МАТЧ #${match.matchId}", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(8.dp))
                val winner = when (match.radiantWin) {
                    true -> "ПОБЕДА СВЕТА"
                    false -> "ПОБЕДА ТЬМЫ"
                    null -> "РЕЗУЛЬТАТ НЕИЗВЕСТЕН"
                }
                Text(winner, color = Accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("${match.radiantScore} : ${match.direScore}", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                Text("Длительность: ${formatDuration(match.duration)}  •  Режим: ${gameModeName(match.gameMode)}", color = Muted, fontSize = 12.sp)
                if (match.startTime > 0) {
                    Spacer(Modifier.height(5.dp))
                    Text("Начало: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(match.startTime * 1000))}", color = Muted, fontSize = 11.sp)
                }
            }
        }
        item { TeamComparison(match, impacts) }
        item {
            val top = impacts.maxByOrNull { it.score }
            val name = top?.let { heroNames[it.player.heroId] ?: "Герой #${it.player.heroId}" } ?: "Нет данных"
            CardBlock {
                Text("ЛУЧШИЙ ПО ИНДЕКСУ ВКЛАДА", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Оценка ${top?.score ?: "—"} / 100 · сравнительный показатель внутри матча", color = Muted, fontSize = 11.sp)
            }
        }
        item {
            val radiant = match.players.filter { it.isRadiant }
            val dire = match.players.filterNot { it.isRadiant }
            CardBlock {
                Text("СОСТАВЫ КОМАНД", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                TeamRosterLine("СВЕТ", radiant, heroNames, heroImageNames, Radiant)
                Spacer(Modifier.height(10.dp))
                TeamRosterLine("ТЬМА", dire, heroNames, heroImageNames, Dire)
            }
        }
    }
}

@Composable
private fun TeamRosterLine(title: String, players: List<Player>, heroNames: Map<Int, String>, heroImageNames: Map<Int, String>, color: Color) {
    Text(title, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    players.sortedBy { it.playerSlot }.forEach { player ->
        Row(Modifier.fillMaxWidth().padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            HeroPortrait(heroImageNames[player.heroId], Modifier.size(width = 52.dp, height = 30.dp))
            Spacer(Modifier.width(9.dp))
            Text(heroNames[player.heroId] ?: "Герой #\${player.heroId}", color = Color.White, fontSize = 12.sp)
        }
    }
}

@Composable
private fun PlayersScreen(match: Match, heroNames: Map<Int, String>, heroImageNames: Map<Int, String>, impacts: List<PlayerImpact>) {
    val impactByPlayer = impacts.associateBy { it.player }
    val radiant = match.players.filter { it.isRadiant }.sortedBy { it.playerSlot }
    val dire = match.players.filterNot { it.isRadiant }.sortedBy { it.playerSlot }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)
    ) {
        item { TeamHeader("СВЕТ", radiant.size, Radiant) }
        items(radiant) { player -> PlayerCard(player, heroNames[player.heroId] ?: "Герой #${player.heroId}", heroImageNames[player.heroId], impactByPlayer[player]) }
        item { TeamHeader("ТЬМА", dire.size, Dire) }
        items(dire) { player -> PlayerCard(player, heroNames[player.heroId] ?: "Герой #${player.heroId}", impactByPlayer[player]) }
    }
}

@Composable
private fun AnalysisScreen(match: Match, heroNames: Map<Int, String>, heroImageNames: Map<Int, String>, impacts: List<PlayerImpact>) {
    val ranked = impacts.sortedByDescending { it.score }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)
    ) {
        item {
            CardBlock {
                Text("АНАЛИЗ ВКЛАДА", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Оценка отражает относительный вклад игрока среди участников этого матча. Это аналитическая эвристика, а не официальный рейтинг.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
        item {
            CardBlock {
                Text("РЕЙТИНГ ИГРОКОВ", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                ranked.forEachIndexed { index, impact ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${index + 1}.", color = Muted, fontSize = 12.sp, modifier = Modifier.width(24.dp))
                        Column(Modifier.weight(1f)) {
                            Text(heroNames[impact.player.heroId] ?: "Герой #${impact.player.heroId}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("${if (impact.player.isRadiant) "Свет" else "Тьма"} · ${impact.roleName}", color = if (impact.player.isRadiant) Radiant else Dire, fontSize = 10.sp)
                        }
                        Text("${impact.score}", color = scoreColor(impact.score), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    if (index != ranked.lastIndex) HorizontalDivider(color = Color(0xFF2A3443))
                }
            }
        }
        item { MatchFlowInsights(match) }
        items(ranked) { impact ->
            PlayerAnalysisCard(impact, heroNames[impact.player.heroId] ?: "Герой #${impact.player.heroId}", heroImageNames[impact.player.heroId], match.players)
        }
    }
}

@Composable
private fun PlayerAnalysisCard(impact: PlayerImpact, heroName: String) {
    CardBlock {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HeroPortrait(heroImageName, Modifier.size(width = 64.dp, height = 44.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(heroName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${if (impact.player.isRadiant) "СВЕТ" else "ТЬМА"} · ${impact.roleName}", color = if (impact.player.isRadiant) Radiant else Dire, fontSize = 10.sp)
            }
            Text("${impact.score}/100", color = scoreColor(impact.score), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(12.dp))
        ImpactMetric("Бой", impact.combat)
        ImpactMetric("Экономика", impact.economy)
        ImpactMetric("Опыт", impact.experience)
        ImpactMetric("Урон героям", impact.heroDamage)
        ImpactMetric("Объекты", impact.objectives)
        ImpactMetric("Добивания", impact.lastHits)
        ImpactMetric("Выживаемость", impact.survivability)
        if (impact.strengths.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text("Сильные стороны", color = Radiant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(impact.strengths.joinToString(" · "), color = Color.White, fontSize = 11.sp)
        }
        if (impact.weaknesses.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Что можно улучшить", color = Dire, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(impact.weaknesses.joinToString(" · "), color = Color.White, fontSize = 11.sp)
        }
    }
}

@Composable
private fun EconomyScreen(match: Match, impacts: List<PlayerImpact>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)
    ) {
        if (match.radiantGoldAdv.isNotEmpty()) {
            item { AdvantageChart("ПРЕИМУЩЕСТВО ПО ЗОЛОТУ", match.radiantGoldAdv, GoldLine, "Свет впереди", "Тьма впереди", "Разница накопленного золота команд") }
        }
        if (match.radiantXpAdv.isNotEmpty()) {
            item { AdvantageChart("ПРЕИМУЩЕСТВО ПО ОПЫТУ", match.radiantXpAdv, XpLine, "Свет впереди", "Тьма впереди", "Разница полученного опыта команд") }
        }
        item { TeamComparison(match, impacts) }
        item {
            CardBlock {
                Text("ЭКОНОМИКА ИГРОКОВ", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                match.players.sortedByDescending { it.netWorth }.forEach { player ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (player.isRadiant) "С" else "Т", color = if (player.isRadiant) Radiant else Dire, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(18.dp))
                        Text("GPM ${player.gpm} · XPM ${player.xpm}", color = Muted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        Text(compact(player.netWorth), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemsScreen(match: Match, heroNames: Map<Int, String>, heroImageNames: Map<Int, String>) {
    val playersWithPurchases = match.players.filter { !it.purchaseLog.isNullOrEmpty() }.sortedBy { it.playerSlot }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)
    ) {
        item {
            CardBlock {
                Text("ПОКУПКИ И ПОРЯДОК СБОРКИ", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Выберите игрока, чтобы раскрыть покупки по времени. Значки и названия предметов помогают быстро просмотреть сборку.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
        if (playersWithPurchases.isEmpty()) {
            item {
                CardBlock {
                    Text("Данные о покупках недоступны", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(5.dp))
                    Text("Для этого матча OpenDota не вернул purchase_log.", color = Muted, fontSize = 11.sp)
                }
            }
        } else {
            items(playersWithPurchases) { player ->
                var expanded by remember(player.playerSlot, player.heroId) { mutableStateOf(false) }
                val purchases = player.purchaseLog.orEmpty().filter { it.key.isNotBlank() }.sortedBy { it.time }
                CardBlock {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HeroPortrait(heroImageNames[player.heroId], Modifier.size(width = 62.dp, height = 42.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(heroNames[player.heroId] ?: "Герой #\${player.heroId}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("\${if (player.isRadiant) "СВЕТ" else "ТЬМА"} · \${purchases.size} покупок", color = if (player.isRadiant) Radiant else Dire, fontSize = 10.sp)
                        }
                        TextButton(onClick = { expanded = !expanded }) {
                            Text(if (expanded) "Свернуть" else "Показать", color = Accent, fontSize = 11.sp)
                        }
                    }
                    if (expanded) {
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFF2A3443))
                        Spacer(Modifier.height(8.dp))
                        purchases.forEach { purchase ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                ItemArtwork(purchase.key, Modifier.size(42.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(humanizeItem(purchase.key), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Покупка", color = Muted, fontSize = 9.sp)
                                }
                                Text(formatDuration(purchase.time), color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamHeader(name: String, count: Int, color: Color) {
    Row(Modifier.fillMaxWidth().padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, color = color, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
        Spacer(Modifier.width(8.dp))
        Text("$count игроков", color = Muted, fontSize = 11.sp)
        Spacer(Modifier.weight(1f))
        Text("ВКЛАД 0–100", color = Muted, fontSize = 9.sp)
    }
}

@Composable
private fun PlayerCard(p: Player, heroName: String, heroImageName: String?, impact: PlayerImpact?) {
    var expanded by remember(p.playerSlot, p.heroId) { mutableStateOf(false) }
    CardBlock {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(heroName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${if (p.isRadiant) "СВЕТ" else "ТЬМА"} • ${impact?.roleName ?: MatchAnalytics.laneName(p.laneRole)}", color = if (p.isRadiant) Radiant else Dire, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(impact?.score?.toString() ?: "—", color = scoreColor(impact?.score ?: 0), fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                Text("ВКЛАД", color = Muted, fontSize = 9.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("K/D/A", "${p.kills}/${p.deaths}/${p.assists}")
            Stat("KDA", String.format(Locale.US, "%.2f", p.kda))
            Stat("GPM", p.gpm.toString())
            Stat("XPM", p.xpm.toString())
            Stat("NET", compact(p.netWorth))
        }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = Color(0xFF2A3443))
        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SmallStat("Урон героям", compact(p.heroDamage))
            SmallStat("Урон зданиям", compact(p.towerDamage))
            SmallStat("Лечение", compact(p.heroHealing))
            SmallStat("Добивания", p.lastHits.toString())
        }
        TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
            Text(if (expanded) "Скрыть подробности" else "Подробная статистика", color = Accent)
        }
        if (expanded) {
            HorizontalDivider(color = Color(0xFF2A3443))
            Spacer(Modifier.height(9.dp))
            Text("ДОПОЛНИТЕЛЬНЫЕ ПОКАЗАТЕЛИ", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            DetailLine("Участие в командных боях", p.teamfightParticipation?.let { String.format(Locale.US, "%.1f%%", it * 100.0) } ?: "Нет данных")
            DetailLine("Оглушения", p.stuns?.let { String.format(Locale.US, "%.1f сек.", it) } ?: "Нет данных")
            DetailLine("Эффективность линии", p.laneEfficiencyPct?.let { String.format(Locale.US, "%.1f%%", it * 100.0) } ?: p.laneEfficiency?.let { String.format(Locale.US, "%.2f", it) } ?: "Нет данных")
            DetailLine("Observer / Sentry", "${p.observerWardsPlaced?.toString() ?: "—"} / ${p.sentryWardsPlaced?.toString() ?: "—"}")
            DetailLine("Уничтожено вардов", "${p.observerWardsKilled?.toString() ?: "—"} / ${p.sentryWardsKilled?.toString() ?: "—"}")
            DetailLine("Стаки / руны", "${p.campsStacked?.toString() ?: "—"} / ${p.runePickups?.toString() ?: "—"}")
            DetailLine("Башни / Рошан", "${p.towerKills?.toString() ?: "—"} / ${p.roshanKills?.toString() ?: "—"}")
            DetailLine("Убийства курьеров", p.courierKills?.toString() ?: "Нет данных")
            DetailLine("Выкупы", p.buybackCount?.toString() ?: "Нет данных")
            DetailLine("Действий в минуту", p.actionsPerMinute?.toString() ?: "Нет данных")
            DetailLine("Нейтральные убийства", p.neutralKills?.toString() ?: "Нет данных")
            impact?.let {
                if (it.strengths.isNotEmpty()) Text("Сильные стороны: ${it.strengths.joinToString()}", color = Radiant, fontSize = 10.sp)
                if (it.weaknesses.isNotEmpty()) Text("Зоны роста: ${it.weaknesses.joinToString()}", color = Dire, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun TeamComparison(match: Match, impacts: List<PlayerImpact>) {
    val teams = remember(match, impacts) { MatchAnalytics.teams(match.players, impacts) }
    val radiant = teams.firstOrNull { it.isRadiant } ?: return
    val dire = teams.firstOrNull { !it.isRadiant } ?: return
    CardBlock {
        Text("СРАВНЕНИЕ КОМАНД", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        Spacer(Modifier.height(12.dp))
        TeamCompareRow("Средний вклад", radiant.averageImpact.toString(), dire.averageImpact.toString())
        TeamCompareRow("Средний GPM", radiant.averageGpm.toString(), dire.averageGpm.toString())
        TeamCompareRow("Средний XPM", radiant.averageXpm.toString(), dire.averageXpm.toString())
        TeamCompareRow("Урон героям", compactLong(radiant.totalHeroDamage), compactLong(dire.totalHeroDamage))
        TeamCompareRow("Урон строениям", compactLong(radiant.totalTowerDamage), compactLong(dire.totalTowerDamage))
        TeamCompareRow("Общий net worth", compactLong(radiant.totalNetWorth), compactLong(dire.totalNetWorth))
        TeamCompareRow("K / D / A", "${radiant.totalKills}/${radiant.totalDeaths}/${radiant.totalAssists}", "${dire.totalKills}/${dire.totalDeaths}/${dire.totalAssists}")
    }
}

@Composable
private fun TeamCompareRow(label: String, radiant: String, dire: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(radiant, color = Radiant, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(label, color = Muted, fontSize = 9.sp, modifier = Modifier.weight(1.4f), textAlign = TextAlign.Center)
        Text(dire, color = Dire, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Muted, fontSize = 10.sp, modifier = Modifier.weight(1f))
        Text(value, color = Color.White, fontSize = 10.sp)
    }
}

@Composable
private fun ImpactMetric(label: String, value: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, fontSize = 10.sp, modifier = Modifier.width(88.dp))
        LinearProgressIndicator(
            progress = { value / 100f },
            modifier = Modifier.weight(1f).height(5.dp),
            color = Accent,
            trackColor = Color(0xFF303B4B)
        )
        Text(value.toString(), color = Color.White, fontSize = 10.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
    }
}

@Composable
private fun AdvantageChart(
    title: String,
    values: List<Int>,
    lineColor: Color,
    positiveLabel: String,
    negativeLabel: String,
    description: String
) {
    CardBlock {
        Text(title, color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        Spacer(Modifier.height(8.dp))
        if (values.size < 2) {
            Text("Недостаточно точек для построения графика.", color = Muted, fontSize = 11.sp)
        } else {
            val maxAbs = values.maxOf { abs(it.toLong()) }.toFloat().coerceAtLeast(1f)
            Canvas(Modifier.fillMaxWidth().height(130.dp)) {
                val midY = size.height / 2f
                drawLine(Color(0xFF596477), Offset(0f, midY), Offset(size.width, midY), strokeWidth = 1.dp.toPx())
                val path = Path()
                values.forEachIndexed { index, value ->
                    val x = size.width * index / (values.size - 1).toFloat()
                    val y = midY - (value / maxAbs) * (size.height * 0.43f)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, lineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx()))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(positiveLabel, color = Radiant, fontSize = 9.sp)
                Text(negativeLabel, color = Dire, fontSize = 9.sp)
            }
            Text(description, color = Muted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(label, color = Muted, fontSize = 9.sp)
    }
}

@Composable
private fun SmallStat(label: String, value: String) {
    Column {
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(label, color = Muted, fontSize = 9.sp)
    }
}

@Composable
private fun CardBlock(content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), content = content)
    }
}

private fun scoreColor(score: Int): Color = when {
    score >= 75 -> Color(0xFF79C99A)
    score >= 45 -> Accent
    else -> Color(0xFFE58A8A)
}

private fun formatDuration(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
private fun compact(value: Int): String = if (value >= 1000) String.format(Locale.US, "%.1fk", value / 1000.0) else value.toString()
private fun compactLong(value: Long): String = if (value >= 1000) String.format(Locale.US, "%.1fk", value / 1000.0) else value.toString()

private fun gameModeName(mode: Int): String = when (mode) {
    1 -> "All Pick"
    2 -> "Captains Mode"
    3 -> "Random Draft"
    4 -> "Single Draft"
    5 -> "All Random"
    22 -> "Ranked All Pick"
    23 -> "Turbo"
    else -> "ID $mode"
}
