package com.xstar.notebook.data.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * 解析 .drawio 文件内容：
 * - 直接可读的 XML（mxGraphModel 直接作为元素存在）原样返回；
 * - 支持 drawio 默认的“整页/单 diagram 内容 base64 + deflate”压缩形式；
 * - 支持少数文本节点被 XML 实体转义（&lt;…）的形式。
 * 若都失败则抛出带说明的异常，便于 UI 做降级提示。
 */
object DrawioDecoder {

    private const val MAX_INFLATED = 8L * 1024 * 1024

    fun decode(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) error("文件为空")
        // 场景1：整个文件就是一段 base64（无任何 XML 标签）
        if (!trimmed.contains('<')) {
            val xml = decodeCompressed(trimmed)
                ?: error("无法解码压缩内容（可能被多次编码或格式特殊）")
            return ensureGraph(xml)
        }
        // 场景2：xml 包裹，逐个 diagram 内容尝试解码
        val diagram = Regex("""<diagram[^>]*>([\s\S]*?)</diagram>""")
        for (m in diagram.findAll(trimmed)) {
            val body = m.groupValues[1].trim()
            if (body.isEmpty()) continue
            // 内容本身就是被转义的 XML 文本
            val unescaped = unescapeXml(body)
            if (unescaped.contains("<mxGraphModel")) return ensureGraph(unescaped)
            // 内容为 base64(deflate(xml))
            decodeCompressed(body)?.let { xml ->
                if (xml.contains("<mxGraphModel")) return ensureGraph(xml)
            }
        }
        // 场景3：普通可读 XML
        if (trimmed.contains("<mxGraphModel") || trimmed.contains("<mxfile")) return trimmed
        error("无法解析该 drawio 文件：可能包含多页或使用了非标准编码，可尝试用文本方式查看原始内容")
    }

    private fun ensureGraph(xml: String): String {
        val idx = xml.indexOf("<mxGraphModel")
        return if (idx >= 0) {
            // 截取到 graphModel 结束，避免其它干扰
            val close = xml.indexOf("</mxGraphModel>", idx)
            if (close > idx) xml.substring(idx, close + "</mxGraphModel>".length) else xml
        } else {
            xml
        }
    }

    private fun inflateBase64(s: String): String? {
        val bytes = decodeBase64(s) ?: return null
        val inflated = inflate(bytes) ?: return null
        if (inflated.size > MAX_INFLATED) return null
        return String(inflated, Charsets.UTF_8)
    }

    /** Drawio 常见格式是 raw-deflate -> base64 -> encodeURIComponent。 */
    private fun decodeCompressed(value: String): String? {
        val candidates = LinkedHashSet<String>()
        val raw = unescapeXml(value).trim()
        candidates.add(raw)
        if (raw.contains('%')) {
            runCatching { URLDecoder.decode(raw, StandardCharsets.UTF_8.name()) }
                .onSuccess { candidates.add(it) }
        }
        for (candidate in candidates) {
            if (!isLikelyBase64(candidate)) continue
            inflateBase64(candidate)?.let { xml ->
                if (xml.contains("<mxGraphModel")) return xml
            }
        }
        return null
    }

    private fun decodeBase64(s: String): ByteArray? {
        val compact = s.replace(Regex("\\s"), "")
        val standard = compact.replace('-', '+').replace('_', '/')
            .let { it + "=".repeat((4 - it.length % 4) % 4) }
        return runCatching { android.util.Base64.decode(standard, android.util.Base64.DEFAULT) }.getOrNull()
    }

    private fun inflate(bytes: ByteArray): ByteArray? {
        // 尝试 zlib（带头）
        try {
            val out = ByteArrayOutputStream()
            InflaterInputStream(ByteArrayInputStream(bytes)).copyTo(out)
            if (out.size() > 0) return out.toByteArray()
        } catch (_: Exception) {
        }
        // 尝试 raw deflate
        return try {
            val out = ByteArrayOutputStream()
            val inflater = Inflater(true)
            val input = ByteArrayInputStream(bytes)
            val buf = ByteArray(4096)
            while (!inflater.finished()) {
                val n = input.read(buf)
                if (n < 0) break
                inflater.setInput(buf, 0, n)
                val outBuf = ByteArray(4096)
                while (!inflater.needsInput() && !inflater.finished()) {
                    val produced = inflater.inflate(outBuf)
                    if (produced > 0) out.write(outBuf, 0, produced)
                    else break
                }
            }
            inflater.end()
            out.toByteArray().takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    private fun isLikelyBase64(s: String): Boolean {
        val compact = s.replace(Regex("\\s"), "")
        if (compact.length < 16) return false
        return compact.all { c ->
            c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9' ||
                c == '+' || c == '/' || c == '=' || c == '-' || c == '_'
        }
    }

    private fun unescapeXml(s: String): String =
        s.replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
}
