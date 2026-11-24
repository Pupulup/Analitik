package com.dota.analyzer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dota.analyzer.data.*
import kotlinx.coroutines.launch
import java.util.Locale

private val InsightAccent = Color(0xFFB58B45)
private val InsightMuted = Color(0xFF8995A7)
private val InsightPanel = Color(0xFF171E29)

@Composable
fun PlayerProfileScreen() {
    val repository = remember { MatchRepository() }
    val scope = rememberCoroutineScope()
    var accountId by remember { mutableStateOf("") }
    var profile by remember { mutableStateOf<PlayerProfileResponse?>(null) }
    var matches by remember { mutableStateOf<List<PlayerMatchSummary>>(emptyList()) }
    var heroNames by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("ПРОФИЛЬ ИГРОКА", color = InsightAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Text("Анализ нескольких матчей", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = accountId,
                    onValueChange = { accountId = it.filter(Char::isDigit).take(20) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Steam ID (32 или 64)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.width(8.dp))
                Button(enabled = !loading && (accountId.toLongOrNull() ?: 0L) > 0L, onClick = {
                    val id = accountId.toLongOrNull() ?: return@Button
                    scope.launch {
                        loading = true
                        error = null
                        try {
                            profile = repository.getPlayerProfile(id)
                            matches = repository.getPlayerMatches(id, 30)
                            heroNames = repository.getHeroNames()
                            if (matches.isEmpty()) error = "История матчей для этого аккаунта не найдена."
                        } catch (e: Exception) {
                            error = "Ошибка загрузки: " + (e.message ?: e.javaClass.simpleName)
                        } finally { loading = false }
                    }
                }) { Text("Анализ") }
            }
        }
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = InsightAccent) }
            error != null -> ProfileList { item { InsightCard { Text(error.orEmpty(), color = Color(0xFFFF9292), fontSize = 12.sp) } } }
            matches.isNotEmpty() -> PlayerProfileResults(profile, matches, heroNames)
            else -> Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.TopCenter) {
                InsightCard {
                    Text("Введите Steam Account ID", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("Профиль анализирует последние 30 матчей: винрейт, KDA, экономику, героев и динамику последних двух периодов.", color = InsightMuted, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun PlayerProfileResults(profile: PlayerProfileResponse?, matches: List<PlayerMatchSummary>, heroNames: Map<Int, String>) {
    val valid = matches.filter { it.didWin != null }
    val wins = valid.count { it.didWin == true }
    val winRate = if (valid.isEmpty()) 0.0 else wins * 100.0 / valid.size
    val avgKda = matches.map { (it.kills + it.assists).toDouble() / maxOf(1, it.deaths) }.average()
    val avgGpm = matches.map { it.gpm }.average()
    val avgXpm = matches.map { it.xpm }.average()
    val grouped = matches.groupBy { it.heroId }.map { entry ->
        val rows = entry.value
        val results = rows.filter { it.didWin != null }
        val heroWins = results.count { it.didWin == true }
        Triple(entry.key, rows.size, if (results.isEmpty()) 0.0 else heroWins * 100.0 / results.size)
    }.sortedByDescending { it.second }

    ProfileList {
        item {
            InsightCard {
                Text(profile?.profile?.personaname?.takeIf { it.isNotBlank() } ?: "Профиль игрока", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("Выборка: " + matches.size + " матчей · " + if (matches.size < 10) "низкая надёжность" else if (matches.size < 25) "средняя надёжность" else "достаточная выборка", color = InsightMuted, fontSize = 10.sp)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ProfileMetric("Винрейт", String.format(Locale.US, "%.1f%%", winRate))
                    ProfileMetric("KDA", String.format(Locale.US, "%.2f", avgKda))
                    ProfileMetric("GPM", avgGpm.toInt().toString())
                    ProfileMetric("XPM", avgXpm.toInt().toString())
                }
            }
        }
        item {
            InsightCard {
                Text("СРАВНЕНИЕ ПЕРИОДОВ", color = InsightAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                val recent = matches.take(10)
                val previous = matches.drop(10).take(10)
                if (recent.size == 10 && previous.isNotEmpty()) {
                    val recentGpm = recent.map { it.gpm }.average()
                    val previousGpm = previous.map { it.gpm }.average()
                    val delta = recentGpm - previousGpm
                    Text("Средний GPM: " + String.format(Locale.US, "%.1f", recentGpm) + " (" + (if (delta >= 0) "+" else "") + String.format(Locale.US, "%.1f", delta) + " к предыдущему периоду)", color = Color.White, fontSize = 11.sp)
                    val recentKda = recent.map { (it.kills + it.assists).toDouble() / maxOf(1, it.deaths) }.average()
                    val previousKda = previous.map { (it.kills + it.assists).toDouble() / maxOf(1, it.deaths) }.average()
                    Text("Средний KDA: " + String.format(Locale.US, "%.2f", recentKda) + " (" + (if (recentKda >= previousKda) "+" else "") + String.format(Locale.US, "%.2f", recentKda - previousKda) + ")", color = Color.White, fontSize = 11.sp)
                } else {
                    Text("Для сравнения периодов нужно не менее 11 матчей.", color = InsightMuted, fontSize = 11.sp)
                }
                Spacer(Modifier.height(5.dp))
                Text("Разница показывает динамику, но сама по себе не доказывает причину изменения результатов.", color = InsightMuted, fontSize = 10.sp)
            }
        }
        item {
            InsightCard {
                Text("ГЕРОИ", color = InsightAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                grouped.take(8).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text((heroNames[row.first] ?: "Герой #" + row.first) + " · " + row.second + " игр", color = Color.White, fontSize = 11.sp)
                        Text(String.format(Locale.US, "%.0f%%", row.third), color = if (row.third >= 50) Color(0xFF79C99A) else Color(0xFFE58A8A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            InsightCard {
                Text("ПОСЛЕДНИЕ МАТЧИ", color = InsightAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                matches.take(20).forEach { match ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (match.didWin == true) "W" else if (match.didWin == false) "L" else "—", color = if (match.didWin == true) Color(0xFF79C99A) else Color(0xFFE58A8A), fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Матч " + match.matchId + " · " + (heroNames[match.heroId] ?: "Герой #" + match.heroId), color = Color.White, fontSize = 10.sp)
                            Text(match.kills.toString() + "/" + match.deaths + "/" + match.assists + " · GPM " + match.gpm + " · XPM " + match.xpm, color = InsightMuted, fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeroGuidesScreen() {
    val repository = remember { MatchRepository() }
    val scope = rememberCoroutineScope()
    var heroes by remember { mutableStateOf<List<HeroStat>>(emptyList()) }
    var items by remember { mutableStateOf<Map<String, ItemConstant>>(emptyMap()) }
    var selectedHero by remember { mutableStateOf<HeroStat?>(null) }
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var guide by remember { mutableStateOf<HeroGuideData?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            heroes = repository.getGuideHeroes()
            items = repository.getItemConstants()
        } catch (e: Exception) {
            error = "Не удалось загрузить список героев: " + (e.message ?: "")
        }
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("БИБЛИОТЕКА ГАЙДОВ", color = InsightAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Text("Сборки и матчапы", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Box {
                OutlinedTextField(value = query, onValueChange = { query = it; expanded = true }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Найти героя") })
                DropdownMenu(expanded = expanded && query.isNotBlank(), onDismissRequest = { expanded = false }) {
                    heroes.filter { it.localizedName.contains(query, true) }.take(8).forEach { hero ->
                        DropdownMenuItem(text = { Text(hero.localizedName) }, onClick = { selectedHero = hero; query = hero.localizedName; expanded = false })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(enabled = !loading && selectedHero != null, onClick = {
                val hero = selectedHero ?: return@Button
                scope.launch {
                    loading = true
                    error = null
                    try { guide = repository.getHeroGuide(hero.id, heroes, items) }
                    catch (e: Exception) { error = "Ошибка загрузки гайда: " + (e.message ?: e.javaClass.simpleName) }
                    finally { loading = false }
                }
            }) { Text("Открыть гайд") }
        }
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = InsightAccent) }
            error != null -> ProfileList { item { InsightCard { Text(error.orEmpty(), color = Color(0xFFFF9292), fontSize = 12.sp) } } }
            guide != null -> HeroGuideResults(guide!!)
            else -> Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.TopCenter) {
                InsightCard { Text("Выберите героя, чтобы получить популярные сборки предметов и статистику матчапов.", color = InsightMuted, fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun HeroGuideResults(guide: HeroGuideData) {
    ProfileList {
        item {
            InsightCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = guide.hero.img, contentDescription = guide.hero.localizedName, modifier = Modifier.size(68.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(guide.hero.localizedName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Статистика OpenDota", color = InsightMuted, fontSize = 10.sp)
                    }
                }
            }
        }
        item {
            InsightCard {
                Text("СБОРКА ПРЕДМЕТОВ", color = InsightAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                GuideItems("Стартовые", guide.startItems)
                GuideItems("Ранняя игра", guide.earlyItems)
                GuideItems("Средняя игра", guide.midItems)
                GuideItems("Поздняя игра", guide.lateItems)
            }
        }
        item {
            InsightCard {
                Text("СЛОЖНЫЕ МАТЧАПЫ", color = InsightAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                if (guide.hardMatchups.isEmpty()) Text("Недостаточно данных.", color = InsightMuted, fontSize = 11.sp)
                guide.hardMatchups.take(8).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(row.heroName, color = Color.White, fontSize = 11.sp)
                        Text(String.format(Locale.US, "%.1f%% · %d игр", row.winRate, row.games), color = Color(0xFFE58A8A), fontSize = 10.sp)
                    }
                }
            }
        }
        item {
            InsightCard {
                Text("ПРОКАЧКА И ТАЛАНТЫ", color = InsightAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Эти данные в старой версии получались через STRATZ GraphQL с серверным API-ключом. Встраивать такой ключ в APK небезопасно; для восстановления прокачки и талантов потребуется защищённый backend endpoint.", color = InsightMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun GuideItems(title: String, rows: List<GuideItem>) {
    if (rows.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        rows.take(6).forEachIndexed { index, item ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text((index + 1).toString() + ". " + item.name, color = Color.White, fontSize = 10.sp)
                Text(item.matches.toString(), color = InsightMuted, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun ProfileList(content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
}

@Composable
private fun InsightCard(content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = InsightPanel), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), content = content)
    }
}

@Composable
private fun ProfileMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(label, color = InsightMuted, fontSize = 9.sp)
    }
}
