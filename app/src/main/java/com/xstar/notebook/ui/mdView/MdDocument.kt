package com.xstar.notebook.ui.mdView

import java.io.File

/** 供渲染使用的“处理结果”。 */
data class PreparedDoc(
    val markdown: String,
    val mermaidBlocks: List<String>,
)

/**
 * Markdown 渲染前处理：
 * 1. 把 ```mermaid 代码块折叠成一个可点击的“预览图表”占位链接；
 * 2. 支持 Obsidian 式双链 [[目标]]（解析为仓库内文档链接）；
 * 3. 把相对图片、相对文档链接转换为指向仓库内绝对 file:// 路径，
 *    便于图片渲染与点击跳转。
 * 处理是围栏(fence)感知的：``` 代码块内部的内容不会被改写。
 */
object MdDocumentPreparer {

    fun prepare(
        raw: String,
        docDir: String,
        repoDir: File,
        mdIndex: Map<String, List<String>>,
    ): PreparedDoc {
        val mermaidBlocks = ArrayList<String>()
        val lines = raw.split("\n")
        val out = ArrayList<String>(lines.size)
        var i = 0
        var inFence = false
        var fenceChar = ""
        var fenceLen = 0
        val mermaidBuffer = StringBuilder()
        var mermaidStart = -1

        fun flushMermaid() {
            if (mermaidStart < 0) return
            val idx = mermaidBlocks.size
            mermaidBlocks.add(mermaidBuffer.toString().trim('\n'))
            out[mermaidStart] = "> **Mermaid 图 #$idx**（点击下方链接预览图表）"
            out.add("")
            out.add("[预览图表](#mermaid-$idx)")
            out.add("")
            mermaidStart = -1
            mermaidBuffer.setLength(0)
        }

        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trimStart()
            val fenceMatch = Regex("^(`{3,}|~{3,})").find(trimmed)
            if (!inFence) {
                if (fenceMatch != null) {
                    inFence = true
                    fenceChar = fenceMatch.groupValues[1].first().toString()
                    fenceLen = fenceMatch.groupValues[1].length
                    val lang = trimmed.substring(fenceLen).trim()
                    if (lang == "mermaid") {
                        mermaidStart = out.size
                        out.add("") // placeholder，稍后替换
                        i++
                        continue
                    }
                    out.add(line)
                    i++
                    continue
                }
                out.add(convertLine(line, docDir, repoDir, mdIndex))
                i++
            } else {
                // 围栏内
                if (fenceMatch != null && fenceMatch.groupValues[1].first().toString() == fenceChar &&
                    fenceMatch.groupValues[1].length >= fenceLen
                ) {
                    inFence = false
                    if (mermaidStart >= 0) {
                        flushMermaid()
                    } else {
                        out.add(line)
                    }
                    i++
                } else {
                    if (mermaidStart >= 0) {
                        mermaidBuffer.append(line).append('\n')
                    } else {
                        out.add(line)
                    }
                    i++
                }
            }
        }
        if (mermaidStart >= 0) flushMermaid()

        return PreparedDoc(markdown = out.joinToString("\n"), mermaidBlocks = mermaidBlocks)
    }

    private fun convertLine(line: String, docDir: String, repoDir: File, mdIndex: Map<String, List<String>>): String {
        var text = line
        text = convertWiki(text, docDir, repoDir, mdIndex)
        text = convertImages(text, docDir, repoDir)
        text = convertLinks(text, docDir, repoDir)
        return text
    }

    /** 把 [[Target|label]] / [[Target]] 解析为仓库内文档链接。 */
    private fun convertWiki(text: String, docDir: String, repoDir: File, mdIndex: Map<String, List<String>>): String {
        val re = Regex("""\[\[([^\]|]+?)(?:\|([^\]]*))?\]\]""")
        return re.replace(text) { m ->
            val target = m.groupValues[1].trim()
            val label = m.groupValues[2].ifBlank {
                target.substringAfterLast('/').substringBeforeLast('.').ifBlank { target }
            }
            val rel = resolveTarget(docDir, repoDir, mdIndex, target)
            if (rel == null) {
                m.value
            } else {
                "[${label.replace("[", "").replace("]", "")}](${File(repoDir, rel).toURI()})"
            }
        }
    }

    private fun convertImages(text: String, docDir: String, repoDir: File): String {
        val regex = Regex("""!\[([^\]]*)\]\(([^)]+)\)""")
        return regex.replace(text) { m ->
            val alt = m.groupValues[1]
            val dest = m.groupValues[2].trim()
            val lower = dest.lowercase()
            val skip = lower.startsWith("http://") || lower.startsWith("https://") ||
                lower.startsWith("data:") || lower.startsWith("file:") || lower.startsWith("/")
            if (skip || dest.startsWith("#")) {
                m.value
            } else {
                val rel = resolvePath(docDir, dest.substringBefore('#').trim()) ?: return@replace m.value
                val file = File(repoDir, rel)
                if (file.isFile) "![$alt](${file.toURI()})" else m.value
            }
        }
    }

    private fun convertLinks(text: String, docDir: String, repoDir: File): String {
        val re = Regex("""(?<!!)\[([^\]]*)]\(([^)]+)\)""")
        return re.replace(text) { m ->
            val label = m.groupValues[1]
            var dest = m.groupValues[2].trim()
            if (dest.isBlank()) return@replace m.value
            val lower = dest.lowercase()
            if (lower.startsWith("http://") || lower.startsWith("https://") ||
                lower.startsWith("mailto:") || lower.startsWith("data:") ||
                lower.startsWith("file:") || lower.startsWith("#")
            ) {
                return@replace m.value
            }
            val target = dest.substringBefore('#').trim()
            val rel = resolvePath(docDir, target) ?: return@replace m.value
            val file = File(repoDir, rel)
            if (file.isFile) {
                val anchor = if (dest.contains("#")) "#${dest.substringAfter('#')}" else ""
                "[$label](${file.toURI()}$anchor)"
            } else {
                m.value
            }
        }
    }

    /** 优先按“相对当前文档目录”解析，找不到时回退按 mdIndex 标题查找。 */
    private fun resolveTarget(docDir: String, repoDir: File, mdIndex: Map<String, List<String>>, target: String): String? {
        if (target.isBlank()) return null
        // 1) 相对路径
        val rel = resolvePath(docDir, target.trim())
        if (rel != null && File(repoDir, rel).isFile) return rel
        // 2) 标题/文件名匹配（任意目录）
        val name = target.trim().substringAfterLast('/')
        val base = name.substringBeforeLast('.').lowercase().trim()
        val hits = mdIndex[base]
        if (!hits.isNullOrEmpty()) return hits.first()
        return null
    }

    /** 把 docDir 下的相对路径规范化到仓库内相对路径；越界或非法时返回 null。 */
    private fun resolvePath(docDir: String, target: String): String? {
        val clean = target.replace("\\", "/")
        if (clean.startsWith("/")) return null
        val parts = ArrayList<String>()
        if (docDir.isNotEmpty()) docDir.split('/').forEach { if (it.isNotEmpty()) parts.add(it) }
        for (seg in clean.split('/')) {
            when (seg) {
                "", "." -> {}
                ".." -> if (parts.isNotEmpty()) parts.removeAt(parts.size - 1) else return null
                else -> parts.add(seg)
            }
        }
        if (parts.isEmpty()) return null
        return parts.joinToString("/")
    }
}

/** 构建仓库内 md 文件标题索引：小写文件名(不含扩展名) -> 相对路径列表。 */
fun buildMdIndex(repoDir: File): Map<String, List<String>> {
    if (!repoDir.isDirectory) return emptyMap()
    val map = HashMap<String, ArrayList<String>>()
    fun walk(dir: File, prefix: String) {
        val children = dir.listFiles() ?: return
        for (f in children) {
            val rel = if (prefix.isEmpty()) f.name else "$prefix/${f.name}"
            if (f.isDirectory) {
                if (f.name == ".git" || f.name == ".idea") continue
                walk(f, rel)
            } else if (f.isFile && f.extension.lowercase() in setOf("md", "markdown")) {
                map.getOrPut(f.nameWithoutExtension.lowercase()) { ArrayList() }.add(rel)
            }
        }
    }
    walk(repoDir, "")
    return map.mapValues { it.value.sorted() }
}
