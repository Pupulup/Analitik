package com.dota.analyzer.data

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportExporter {
    fun share(context: Context, match: Match, heroNames: Map<Int, String>, impacts: List<PlayerImpact>) {
        val file = create(context, match, heroNames, impacts)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Экспорт отчёта матча"))
    }

    private fun create(
        context: Context,
        match: Match,
        heroNames: Map<Int, String>,
        impacts: List<PlayerImpact>
    ): File {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 42f
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f; color = 0xFF596273.toInt() }
        val players = impacts.sortedByDescending { it.score }
        val pageCount = maxOf(1, (players.size + 5) / 6)
        for (pageIndex in 0 until pageCount) {
            val page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create())
            val canvas = page.canvas
            var y = 54f
            if (pageIndex == 0) {
                canvas.drawText("DOTA ANALYZER — MATCH REPORT", margin, y, titlePaint)
                y += 28f
                canvas.drawText("Матч #${match.matchId}", margin, y, headingPaint)
                y += 18f
                val result = when (match.radiantWin) {
                    true -> "Победа Света"
                    false -> "Победа Тьмы"
                    null -> "Результат неизвестен"
                }
                canvas.drawText(
                    "$result  |  ${match.radiantScore} : ${match.direScore}  |  ${formatDuration(match.duration)}",
                    margin, y, bodyPaint
                )
                y += 16f
                if (match.startTime > 0) {
                    val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                        .format(Date(match.startTime * 1000))
                    canvas.drawText("Дата: $date", margin, y, mutedPaint)
                    y += 24f
                } else y += 8f
                canvas.drawText("ИТОГОВАЯ ОЦЕНКА ИГРОКОВ", margin, y, headingPaint)
                y += 18f
            } else {
                canvas.drawText("MATCH #${match.matchId} — ИГРОКИ (ПРОДОЛЖЕНИЕ)", margin, y, headingPaint)
                y += 26f
            }

            val pagePlayers = players.drop(pageIndex * 6).take(6)
            pagePlayers.forEachIndexed { index, impact ->
                val p = impact.player
                val hero = heroNames[p.heroId] ?: "Герой #${p.heroId}"
                val side = if (p.isRadiant) "СВЕТ" else "ТЬМА"
                canvas.drawText("${pageIndex * 6 + index + 1}. $hero — $side — ${impact.score}/100", margin, y, headingPaint)
                y += 15f
                canvas.drawText(
                    "K/D/A ${p.kills}/${p.deaths}/${p.assists} | KDA ${"%.2f".format(Locale.US, p.kda)} | GPM ${p.gpm} | XPM ${p.xpm} | Net Worth ${p.netWorth}",
                    margin + 10f, y, bodyPaint
                )
                y += 14f
                canvas.drawText(
                    "Урон героям ${p.heroDamage} | строениям ${p.towerDamage} | лечение ${p.heroHealing} | LH ${p.lastHits} | denies ${p.denies}",
                    margin + 10f, y, bodyPaint
                )
                y += 14f
                canvas.drawText(
                    "Бой ${impact.combat}  Экономика ${impact.economy}  Опыт ${impact.experience}  Объекты ${impact.objectives}  Выживаемость ${impact.survivability}",
                    margin + 10f, y, mutedPaint
                )
                y += 14f
                val positive = if (impact.strengths.isEmpty()) "выраженных сильных сторон по модели не выделено" else "Сильные стороны: ${impact.strengths.joinToString()}"
                val negative = if (impact.weaknesses.isEmpty()) "слабые стороны не выделены" else "Зоны роста: ${impact.weaknesses.joinToString()}"
                canvas.drawText(positive, margin + 10f, y, mutedPaint)
                y += 12f
                canvas.drawText(negative, margin + 10f, y, mutedPaint)
                y += 24f
            }
            canvas.drawText(
                "Индекс — сравнительная эвристическая оценка внутри этого матча, не рейтинг Valve.",
                margin, pageHeight - 28f, mutedPaint
            )
            document.finishPage(page)
        }

        val file = File(context.cacheDir, "match-${match.matchId}-report.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun formatDuration(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
}
