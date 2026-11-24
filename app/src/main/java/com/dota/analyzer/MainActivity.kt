package com.dota.analyzer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dota.analyzer.data.Match
import com.dota.analyzer.data.MatchAnalytics
import com.dota.analyzer.data.Player
import com.dota.analyzer.data.PlayerImpact
import com.dota.analyzer.data.TimelineEvent
import com.dota.analyzer.data.PdfReportExporter
import androidx.compose.ui.platform.LocalContext
import com.dota.analyzer.ui.MatchState
import com.dota.analyzer.ui.MatchViewModel
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
    MaterialTheme(colorScheme = darkColorScheme(
        primary = Accent, background = Bg, surface = Panel,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(34.dp))
                Text("DOTA ANALYZER", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Text("Разбор матча", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                Text("Статистика, вклад игроков и динамика преимущества", color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(22.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = matchId, onValueChange = { matchId = it.filter(Char::isDigit) },
                        modifier = Modifier.weight(1f), singleLine = true,
                        label = { Text("ID матча") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Accent, unfocusedBorderColor = Color(0xFF354052),
                            focusedLabelColor = Accent, cursorColor = Accent
                        )
                    )
                    Spacer(Modifier.width(10.dp))
                    Button(onClick = { vm.loadMatch(matchId) }, shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp)) {
                        Icon(Icons.Default.Search, contentDescription = "Найти матч")
                    }
                }
                Spacer(Modifier.height(18.dp))
                when (val s = state) {
                    MatchState.Idle -> EmptyState()
                    MatchState.Loading -> Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Accent)
                    }
                    is MatchState.Error -> CardBlock {
                        Text("Не удалось загрузить", color = Color(0xFFFF8585), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp)); Text(s.message, color = Muted)
                    }
                    is MatchState.Success -> MatchDetails(s.match, s.heroNames)
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
        Text("Введите номер матча Dota 2. Приложение загрузит результат, составы команд, индивидуальную статистику и оценку вклада игроков.", color = Muted, lineHeight = 20.sp)
    }
}

@Composable
private fun MatchDetails(match: Match, heroNames: Map<Int, String>) {
    val context = LocalContext.current
    val impacts = remember(match) { MatchAnalytics.analyze(match.players) }
    val impactByPlayer = impacts.associateBy { it.player }
    val timeline = remember(match) { MatchAnalytics.timeline(match.players) }
    val radiantPlayers = match.players.filter { it.isRadiant }.sortedBy { it.playerSlot }
    val direPlayers = match.players.filterNot { it.isRadiant }.sortedBy { it.playerSlot }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 28.dp)) {
        item {
            CardBlock {
                Text("МАТЧ #${match.matchId}", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(8.dp))
                val winner = when (match.radiantWin) { true -> "ПОБЕДА СВЕТА"; false -> "ПОБЕДА ТЬМЫ"; null -> "РЕЗУЛЬТАТ НЕИЗВЕСТЕН" }
                Text(winner, color = Accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("${match.radiantScore}  :  ${match.direScore}", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                Text("Длительность: ${formatDuration(match.duration)}  •  Режим: ${gameModeName(match.gameMode)}", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { runCatching { PdfReportExporter.share(context, match, heroNames, impacts) } }, modifier = Modifier.fillMaxWidth()) {
                    Text("Экспорт подробного PDF-отчёта", color = Accent)
                }
                if (match.startTime > 0) {
                    Text("Начало: ${java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(java.util.Date(match.startTime * 1000))}", color = Muted, fontSize = 11.sp)
                }
            }
        }
        if (match.radiantGoldAdv.isNotEmpty()) {
            item { AdvantageChart("ПРЕИМУЩЕСТВО ПО ЗОЛОТУ", match.radiantGoldAdv, GoldLine, "Свет впереди", "Тьма впереди") }
        }
        if (match.radiantXpAdv.isNotEmpty()) {
            item { AdvantageChart("ПРЕИМУЩЕСТВО ПО ОПЫТУ", match.radiantXpAdv, XpLine, "Свет впереди", "Тьма впереди") }
        }
        item { TeamComparison(match, impacts) }
        if (timeline.isNotEmpty()) {
            item { MatchTimeline(timeline, heroNames) }
        }
        item { TeamHeader("СВЕТ", radiantPlayers.size, Radiant) }
        items(radiantPlayers) { player ->
            PlayerCard(player, heroNames[player.heroId] ?: "Герой #${player.heroId}", impactByPlayer[player])
        }
        item { TeamHeader("ТЬМА", direPlayers.size, Dire) }
        items(direPlayers) { player ->
            PlayerCard(player, heroNames[player.heroId] ?: "Герой #${player.heroId}", impactByPlayer[player])
        }
        item {
            CardBlock {
                Text("КАК ЧИТАТЬ ОЦЕНКУ", color = Accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Text("Индекс вклада — сравнительная эвристическая оценка от 0 до 100 внутри конкретного матча. В текущей модели учитываются бой (20%), экономика (20%), опыт (15%), урон героям (20%), объекты (10%), добивания (5%) и выживаемость (10%). Дополнительные метрики показываются отдельно, если их вернул OpenDota. Отсутствующие поля не трактуются как нулевой результат. Это не официальный рейтинг Valve.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
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
private fun PlayerCard(p: Player, heroName: String, impact: PlayerImpact?) {
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
        if (impact != null) {
            Spacer(Modifier.height(12.dp))
            ImpactMetric("Бой", impact.combat)
            ImpactMetric("Экономика", impact.economy)
            ImpactMetric("Опыт", impact.experience)
            ImpactMetric("Урон героям", impact.heroDamage)
            ImpactMetric("Объекты", impact.objectives)
            ImpactMetric("Добивания", impact.lastHits)
            ImpactMetric("Выживаемость", impact.survivability)
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
            impact?.let { playerImpact ->
                if (playerImpact.strengths.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Сильные стороны: ${playerImpact.strengths.joinToString()}", color = Radiant, fontSize = 10.sp)
                }
                if (playerImpact.weaknesses.isNotEmpty()) {
                    Spacer(Modifier.height(5.dp))
                    Text("Зоны роста: ${playerImpact.weaknesses.joinToString()}", color = Dire, fontSize = 10.sp)
                }
            }
            val purchases = p.purchaseLog.orEmpty().takeLast(5).asReversed()
            if (purchases.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("ПОСЛЕДНИЕ ПОКУПКИ", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                purchases.forEach { purchase ->
                    DetailLine(formatDuration(purchase.time), purchase.key.replace('_', ' '))
                }
            }
        }
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
private fun TeamComparison(match: Match, impacts: List<PlayerImpact>) {
    val teams = remember(match, impacts) { MatchAnalytics.teams(match.players, impacts) }
    val radiant = teams.firstOrNull { it.isRadiant }
    val dire = teams.firstOrNull { !it.isRadiant }
    if (radiant == null || dire == null) return
    CardBlock {
        Text("СРАВНЕНИЕ КОМАНД", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        Spacer(Modifier.height(12.dp))
        TeamCompareRow("Средний вклад", radiant.averageImpact.toString(), dire.averageImpact.toString(), Radiant, Dire)
        TeamCompareRow("Средний GPM", radiant.averageGpm.toString(), dire.averageGpm.toString(), Radiant, Dire)
        TeamCompareRow("Средний XPM", radiant.averageXpm.toString(), dire.averageXpm.toString(), Radiant, Dire)
        TeamCompareRow("Урон героям", compactLong(radiant.totalHeroDamage), compactLong(dire.totalHeroDamage), Radiant, Dire)
        TeamCompareRow("Урон строениям", compactLong(radiant.totalTowerDamage), compactLong(dire.totalTowerDamage), Radiant, Dire)
        TeamCompareRow("Общий net worth", compactLong(radiant.totalNetWorth), compactLong(dire.totalNetWorth), Radiant, Dire)
        TeamCompareRow("K / D / A", "${radiant.totalKills}/${radiant.totalDeaths}/${radiant.totalAssists}", "${dire.totalKills}/${dire.totalDeaths}/${dire.totalAssists}", Radiant, Dire)
    }
}

@Composable
private fun TeamCompareRow(label: String, radiant: String, dire: String, radiantColor: Color, direColor: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(radiant, color = radiantColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(label, color = Muted, fontSize = 9.sp, modifier = Modifier.weight(1.4f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text(dire, color = direColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}

@Composable
private fun MatchTimeline(events: List<TimelineEvent>, heroNames: Map<Int, String>) {
    CardBlock {
        Text("ХРОНОЛОГИЯ УБИЙСТВ", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        Spacer(Modifier.height(8.dp))
        events.take(18).forEach { event ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(formatDuration(event.timeSeconds), color = Muted, fontSize = 10.sp, modifier = Modifier.width(48.dp))
                Text(heroNames[event.heroId] ?: "Герой #${event.heroId}", color = if (event.isRadiant) Radiant else Dire, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(event.description, color = Color.White, fontSize = 10.sp)
            }
        }
        if (events.size > 18) Text("Показаны первые 18 из ${events.size} событий.", color = Muted, fontSize = 9.sp)
    }
}

private fun compactLong(value: Long): String =
    if (value >= 1000) String.format(Locale.US, "%.1fk", value / 1000.0) else value.toString()

@Composable
private fun ImpactMetric(label: String, value: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, fontSize = 10.sp, modifier = Modifier.width(78.dp))
        LinearProgressIndicator(
            progress = { value / 100f },
            modifier = Modifier.weight(1f).height(5.dp),
            color = Accent,
            trackColor = Color(0xFF303B4B)
        )
        Text(value.toString(), color = Color.White, fontSize = 10.sp, modifier = Modifier.width(28.dp))
    }
}

@Composable
private fun AdvantageChart(title: String, values: List<Int>, lineColor: Color, positiveLabel: String, negativeLabel: String) {
    CardBlock {
        Text(title, color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        Spacer(Modifier.height(8.dp))
        if (values.size < 2) {
            Text("Недостаточно точек для построения графика.", color = Muted, fontSize = 11.sp)
        } else {
            val maxAbs = (values.maxOf { abs(it.toLong()) }.toFloat()).coerceAtLeast(1f)
            Canvas(Modifier.fillMaxWidth().height(112.dp)) {
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
            Text("Показатель приведён с точки зрения Света; выше нуля — преимущество Света.", color = Muted, fontSize = 9.sp)
        }
    }
}

private fun scoreColor(score: Int): Color = when {
    score >= 75 -> Color(0xFF79C99A)
    score >= 45 -> Accent
    else -> Color(0xFFE58A8A)
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
    Column { Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold); Text(label, color = Muted, fontSize = 9.sp) }
}
@Composable
private fun CardBlock(content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), content = content)
    }
}
private fun formatDuration(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
private fun compact(value: Int): String = if (value >= 1000) String.format(Locale.US, "%.1fk", value / 1000.0) else value.toString()
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
