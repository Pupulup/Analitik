package com.dota.analyzer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dota.analyzer.data.Match
import com.dota.analyzer.data.Player
import com.dota.analyzer.ui.MatchState
import com.dota.analyzer.ui.MatchViewModel
import java.util.Locale

private val Bg = Color(0xFF0D1118)
private val Panel = Color(0xFF171E29)
private val Accent = Color(0xFFB58B45)
private val Muted = Color(0xFF8995A7)

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
                Text("Подробная статистика на основе OpenDota", color = Muted, fontSize = 13.sp)
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
        Text("Введите номер матча Dota 2. Приложение загрузит результат, составы команд и индивидуальную статистику игроков.", color = Muted, lineHeight = 20.sp)
    }
}

@Composable
private fun MatchDetails(match: Match, heroNames: Map<Int, String>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 28.dp)) {
        item {
            CardBlock {
                Text("МАТЧ #${match.matchId}", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(8.dp))
                val winner = when (match.radiantWin) { true -> "ПОБЕДА СВЕТА"; false -> "ПОБЕДА ТЬМЫ"; null -> "РЕЗУЛЬТАТ НЕИЗВЕСТЕН" }
                Text(winner, color = Accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("${match.radiantScore}  :  ${match.direScore}", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                Text("Длительность: ${formatDuration(match.duration)}  •  Режим: ${match.gameMode}", color = Muted, fontSize = 12.sp)
            }
        }
        item { Text("ИГРОКИ", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) }
        items(match.players) { player -> PlayerCard(player, heroNames[player.heroId] ?: "Герой #${player.heroId}") }
        item {
            Text("Оценка вклада в этой версии основана на доступных игровых показателях. Это аналитическая сводка, а не официальный рейтинг Valve.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun PlayerCard(p: Player, heroName: String) {
    CardBlock {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(heroName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(if (p.isRadiant) "СВЕТ" else "ТЬМА", color = if (p.isRadiant) Color(0xFF79C99A) else Color(0xFFE58A8A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Text("${p.kills}/${p.deaths}/${p.assists}", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("KDA", String.format(Locale.US, "%.2f", p.kda))
            Stat("GPM", p.gpm.toString())
            Stat("XPM", p.xpm.toString())
            Stat("NET", compact(p.netWorth))
        }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = Color(0xFF2A3443))
        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SmallStat("Урон героя", compact(p.heroDamage))
            SmallStat("Добивания", p.lastHits.toString())
            SmallStat("Уровень", p.level.toString())
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, color = Muted, fontSize = 10.sp)
    }
}
@Composable
private fun SmallStat(label: String, value: String) {
    Column { Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold); Text(label, color = Muted, fontSize = 10.sp) }
}
@Composable
private fun CardBlock(content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), content = content)
    }
}
private fun formatDuration(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
private fun compact(value: Int): String = if (value >= 1000) String.format(Locale.US, "%.1fk", value / 1000.0) else value.toString()
