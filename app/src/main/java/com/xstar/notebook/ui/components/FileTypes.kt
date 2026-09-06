package com.xstar.notebook.ui.components

enum class FileKind { MD, IMAGE, DRAWIO, CODE, TEXT, OTHER }

object FileTypes {
    val imageExts = setOf("png", "jpg", "jpeg", "gif", "webp", "bmp", "svg")
    val textExts = setOf("txt", "text", "log", "csv", "tsv")
    val codeExts = setOf(
        "sh", "bat", "cmd", "ps1",
        "py", "rb", "go", "rs", "php",
        "js", "ts", "jsx", "tsx",
        "java", "kt", "kts",
        "c", "h", "cpp", "hpp", "cs",
        "sql", "json", "xml", "yaml", "yml", "toml", "ini", "cfg", "conf",
        "properties", "gradle", "html", "css", "scss",
    )
    val namedCodeFiles = setOf("dockerfile", "makefile", "gradlew", "gradle.properties", "gitignore")

    fun kind(fileName: String): FileKind {
        val name = fileName.lowercase()
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when {
            ext == "md" || ext == "markdown" -> FileKind.MD
            ext == "drawio" || ext == "dio" -> FileKind.DRAWIO
            ext in imageExts -> FileKind.IMAGE
            ext in codeExts || name in namedCodeFiles || name == "dockerfile" -> FileKind.CODE
            ext in textExts -> FileKind.TEXT
            else -> FileKind.OTHER
        }
    }

    fun emoji(fileName: String): String = when (kind(fileName)) {
        FileKind.MD -> "📄"
        FileKind.DRAWIO -> "🔀"
        FileKind.IMAGE -> "🖼"
        FileKind.CODE -> "⚙️"
        FileKind.TEXT -> "📝"
        FileKind.OTHER -> "📁"
    }
}
