package com.xstar.notebook.data.util

/** data 层的文件类型归类（与 ui 的 FileTypes 保持一致的最小集合）。 */
object FileTypeMap {
    private val mdExt = setOf("md", "markdown")
    private val imageExt = setOf("png", "jpg", "jpeg", "gif", "webp", "bmp", "svg")
    private val textExt = setOf("txt", "text", "log", "csv", "tsv")
    private val drawioExt = setOf("drawio", "dio")
    private val codeExt = setOf(
        "sh", "bat", "cmd", "ps1", "py", "rb", "go", "rs", "php",
        "js", "ts", "jsx", "tsx", "java", "kt", "kts", "c", "h", "cpp", "hpp",
        "cs", "sql", "json", "xml", "yaml", "yml", "toml", "ini", "cfg", "conf",
        "properties", "gradle", "html", "css", "scss",
    )
    private val namedCode = setOf("dockerfile", "makefile", "gradlew", "gradle.properties", "gitignore")

    /** 可被全文搜索读取的文件类型。 */
    fun isTextLike(fileName: String): Boolean {
        val name = fileName.lowercase()
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in mdExt || ext in textExt || ext in codeExt || name in namedCode || name == "dockerfile"
    }

    fun type(fileName: String): String {
        val name = fileName.lowercase()
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when {
            ext in mdExt -> "md"
            ext in drawioExt -> "drawio"
            ext in imageExt -> "image"
            ext in textExt -> "text"
            ext in codeExt || name in namedCode || name == "dockerfile" -> "code"
            else -> "other"
        }
    }
}
