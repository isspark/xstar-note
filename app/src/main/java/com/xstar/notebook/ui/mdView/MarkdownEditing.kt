package com.xstar.notebook.ui.mdView

import android.content.Context
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import java.io.File
import java.security.MessageDigest

enum class MdTool {
    H1, H2, H3, BOLD, ITALIC, STRIKE, INLINE_CODE,
    CODE_BLOCK, QUOTE, BULLET, NUMBER, TASK, LINK, HR,
}

/**
 * 对编辑器选区应用 Markdown 格式化动作，返回更新后的 TextFieldValue。
 * 尽力保留选区，无法精确保留时把光标放到改动末尾。
 */
fun applyMarkdownTool(original: TextFieldValue, tool: MdTool): TextFieldValue {
    val start = original.selection.min
    val end = original.selection.max
    val text = original.text
    return when (tool) {
        MdTool.H1 -> heading(text, start, end, 1)
        MdTool.H2 -> heading(text, start, end, 2)
        MdTool.H3 -> heading(text, start, end, 3)
        MdTool.BOLD -> wrap(text, start, end, "**", "**")
        MdTool.ITALIC -> wrap(text, start, end, "*", "*")
        MdTool.STRIKE -> wrap(text, start, end, "~~", "~~")
        MdTool.INLINE_CODE -> wrap(text, start, end, "`", "`")
        MdTool.QUOTE -> linePrefix(text, start, end, "> ")
        MdTool.BULLET -> linePrefix(text, start, end, "- ")
        MdTool.NUMBER -> linePrefix(text, start, end, "1. ")
        MdTool.TASK -> linePrefix(text, start, end, "- [ ] ")
        MdTool.CODE_BLOCK -> wrap(text, start, end, "\n```\n", "\n```\n")
        MdTool.LINK -> makeLink(text, start, end)
        MdTool.HR -> hr(text, end)
    }
}

private fun field(text: String, selection: Int) = TextFieldValue(text, TextRange(selection, selection))

private fun heading(text: String, start: Int, end: Int, level: Int): TextFieldValue {
    val (ls, le) = lineBounds(text, start)
    val line = text.substring(ls, le)
    val prefix = "#".repeat(level)
    val trimmed = line.trimStart()
    val leading = line.length - trimmed.length
    val m = Regex("^(#{1,6})\\s*").find(trimmed)
    val newLine = if (m != null) {
        val existing = m.groupValues[1]
        if (existing.length == level) {
            trimmed.removePrefix(existing).trimStart()
        } else {
            prefix + " " + trimmed.removePrefix(existing).trimStart()
        }
    } else {
        prefix + " " + trimmed
    }
    val newText = text.substring(0, ls) + (" ".repeat(leading)) + newLine + text.substring(le)
    val cursor = ls + leading + newLine.length
    return field(newText, cursor)
}

/** 对每个选中行加前缀；若所有非空行都已有该前缀则移除。 */
private fun linePrefix(text: String, start: Int, end: Int, prefix: String): TextFieldValue {
    val (ls, le) = lineBounds(text, start)
    val (ls2, le2) = lineBounds(text, end.coerceAtMost(text.length))
    val from = ls
    val to = le2
    val region = text.substring(from, to)
    val lines = region.split("\n")
    val allPrefixed = lines.isNotEmpty() && lines.all { it.isBlank() || it.startsWith(prefix) }
    val out = lines.joinToString("\n") { ln ->
        if (ln.isBlank()) ln
        else if (allPrefixed) ln.removePrefix(prefix)
        else prefix + ln
    }
    val newText = text.substring(0, from) + out + text.substring(to)
    return field(newText, from + out.length)
}

private fun wrap(text: String, start: Int, end: Int, pre: String, post: String): TextFieldValue {
    if (end <= start) {
        val newText = text.substring(0, start) + pre + post + text.substring(start)
        val sel = start + pre.length
        return TextFieldValue(newText, TextRange(sel, sel))
    }
    val selected = text.substring(start, end)
    val newText = text.substring(0, start) + pre + selected + post + text.substring(end)
    return TextFieldValue(newText, TextRange(start + pre.length, end + pre.length))
}

private fun makeLink(text: String, start: Int, end: Int): TextFieldValue {
    val label = if (end > start) text.substring(start, end) else ""
    val insert = "[$label](url)"
    val newText = text.substring(0, start) + insert + text.substring(end)
    // 选中 "url" 便于直接输入
    val urlStart = start + label.length + 3
    val urlEnd = urlStart + 3
    return TextFieldValue(newText, TextRange(urlStart, urlEnd))
}

private fun hr(text: String, cursor: Int): TextFieldValue {
    val needBlank = cursor in 1..text.length && text[cursor - 1] != '\n'
    val insert = if (needBlank) "\n\n---\n\n" else "---\n\n"
    val newText = text.substring(0, cursor) + insert + text.substring(cursor)
    return field(newText, cursor + insert.length)
}

private fun lineBounds(text: String, offset: Int): Pair<Int, Int> {
    val o = offset.coerceIn(0, text.length)
    val ls = text.lastIndexOf('\n', o - 1) + 1
    val le = text.indexOf('\n', o).let { if (it < 0) text.length else it }
    return ls to le
}

// ------------------------------------------------------------------
// 草稿存储：编辑过程中本地兜底保存，防误退丢内容。
// ------------------------------------------------------------------

object DraftStore {
    private fun draftFile(context: Context, repoId: Long, relPath: String): File {
        val dir = File(context.filesDir, "drafts").apply { mkdirs() }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(relPath.toByteArray(Charsets.UTF_8))
            .take(8)
            .joinToString("") { "%02x".format(it) }
        return File(dir, "$repoId-$digest.md")
    }

    fun exists(context: Context, repoId: Long, relPath: String): Boolean =
        draftFile(context, repoId, relPath).isFile

    fun load(context: Context, repoId: Long, relPath: String): String? =
        runCatching { draftFile(context, repoId, relPath).readText(Charsets.UTF_8) }.getOrNull()

    fun save(context: Context, repoId: Long, relPath: String, content: String) {
        runCatching { draftFile(context, repoId, relPath).writeText(content, Charsets.UTF_8) }
    }

    fun clear(context: Context, repoId: Long, relPath: String) {
        runCatching { draftFile(context, repoId, relPath).delete() }
    }
}
