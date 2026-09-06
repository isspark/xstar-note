package com.xstar.notebook.data.util

import java.nio.charset.Charset

object TextEncoding {

    private val UTF_8 = Charsets.UTF_8
    private val UTF_16LE = Charset.forName("UTF-16LE")
    private val UTF_16BE = Charset.forName("UTF-16BE")
    private val GBK = Charset.forName("GBK")

    private val BOM_UTF8 = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
    private val BOM_LE = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
    private val BOM_BE = byteArrayOf(0xFE.toByte(), 0xFF.toByte())

    private fun startsWith(data: ByteArray, prefix: ByteArray): Boolean {
        if (data.size < prefix.size) return false
        for (i in prefix.indices) if (data[i] != prefix[i]) return false
        return true
    }

    /** 读取时解码：BOM > 严格 UTF-8 > UTF-16 启发 > GBK 兜底。 */
    fun decode(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        return when {
            startsWith(bytes, BOM_UTF8) -> String(bytes, 3, bytes.size - 3, UTF_8)
            startsWith(bytes, BOM_LE) -> String(bytes, 2, bytes.size - 2, UTF_16LE)
            startsWith(bytes, BOM_BE) -> String(bytes, 2, bytes.size - 2, UTF_16BE)
            isValidUtf8(bytes) -> String(bytes, UTF_8)
            looksLikeUtf16(bytes) -> String(bytes, UTF_16LE)
            else -> try {
                String(bytes, GBK)
            } catch (e: Exception) {
                String(bytes, Charset.forName("ISO-8859-1"))
            }
        }
    }

    /** 写入时尽量沿用原文件编码，避免再次保存改变编码。 */
    fun charsetForWrite(bytes: ByteArray): Charset = when {
        startsWith(bytes, BOM_UTF8) -> UTF_8
        startsWith(bytes, BOM_LE) -> UTF_16LE
        startsWith(bytes, BOM_BE) -> UTF_16BE
        isValidUtf8(bytes) -> UTF_8
        looksLikeUtf16(bytes) -> UTF_16LE
        else -> GBK
    }

    private fun isValidUtf8(bytes: ByteArray): Boolean {
        var i = 0
        while (i < bytes.size) {
            val b = bytes[i].toInt() and 0xFF
            when {
                b <= 0x7F -> i++
                b in 0xC2..0xDF -> {
                    if (i + 1 >= bytes.size) return false
                    if ((bytes[i + 1].toInt() and 0xC0) != 0x80) return false
                    i += 2
                }
                b == 0xE0 -> {
                    if (i + 2 >= bytes.size) return false
                    if ((bytes[i + 1].toInt() and 0xE0) != 0xA0) return false
                    if ((bytes[i + 2].toInt() and 0xC0) != 0x80) return false
                    i += 3
                }
                b in 0xE1..0xEC -> {
                    if (i + 2 >= bytes.size) return false
                    if ((bytes[i + 1].toInt() and 0xC0) != 0x80) return false
                    if ((bytes[i + 2].toInt() and 0xC0) != 0x80) return false
                    i += 3
                }
                b == 0xED -> {
                    if (i + 2 >= bytes.size) return false
                    if ((bytes[i + 1].toInt() and 0xE0) != 0x80) return false
                    if ((bytes[i + 2].toInt() and 0xC0) != 0x80) return false
                    i += 3
                }
                b in 0xEE..0xEF -> {
                    if (i + 2 >= bytes.size) return false
                    if ((bytes[i + 1].toInt() and 0xC0) != 0x80) return false
                    if ((bytes[i + 2].toInt() and 0xC0) != 0x80) return false
                    i += 3
                }
                b in 0xF0..0xF4 -> {
                    if (i + 3 >= bytes.size) return false
                    val c1 = bytes[i + 1].toInt() and 0xFF
                    val lo = if (b == 0xF0) 0x90 else if (b == 0xF4) 0x80 else 0x80
                    val hi = if (b == 0xF0) 0x8F else if (b == 0xF4) 0x8F else 0xBF
                    if (c1 < lo || c1 > hi) return false
                    if ((bytes[i + 2].toInt() and 0xC0) != 0x80) return false
                    if ((bytes[i + 3].toInt() and 0xC0) != 0x80) return false
                    i += 4
                }
                else -> return false
            }
        }
        return true
    }

    private fun looksLikeUtf16(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        var zeros = 0
        for (b in bytes) if (b == 0.toByte()) zeros++
        return zeros >= bytes.size / 3
    }
}
