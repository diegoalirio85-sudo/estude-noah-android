package com.estudenoah.app.material

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.graphics.pdf.PdfRendererPreV
import android.net.Uri
import android.os.Build
import android.os.ext.SdkExtensions
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.zip.ZipInputStream

internal data class ImportedMaterialResult(
    val fileName: String,
    val extension: String,
    val extractedText: String,
    val message: String,
    val usableForGeneration: Boolean
)

internal object MaterialFileExtractor {
    private val accepted = setOf("pdf", "ppt", "pptx", "docx", "odt", "doc", "mp4", "mp3", "avi")

    fun displayName(context: Context, uri: Uri): String {
        val resolver = context.contentResolver
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                return cursor.getString(index) ?: "material"
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "material"
    }

    fun extract(context: Context, uri: Uri): ImportedMaterialResult {
        val name = displayName(context, uri)
        val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        if (ext !in accepted) {
            return ImportedMaterialResult(
                fileName = name,
                extension = ext,
                extractedText = "",
                message = "Formato não aceito nesta versão.",
                usableForGeneration = false
            )
        }

        return runCatching {
            when (ext) {
                "docx" -> {
                    val text = extractZipXml(context, uri) { entry -> entry == "word/document.xml" }
                    resultFromText(name, ext, text, "Texto extraído do DOCX.")
                }
                "pptx" -> {
                    val text = extractZipXml(context, uri) { entry ->
                        entry.startsWith("ppt/slides/slide") && entry.endsWith(".xml")
                    }
                    resultFromText(name, ext, text, "Texto extraído dos slides do PPTX.")
                }
                "odt" -> {
                    val text = extractZipXml(context, uri) { entry -> entry == "content.xml" }
                    resultFromText(name, ext, text, "Texto extraído do ODT.")
                }
                "doc", "ppt" -> {
                    val bytes = readAllLimited(context, uri, 16 * 1024 * 1024)
                    val text = extractLegacyPrintableText(bytes)
                    resultFromText(
                        name,
                        ext,
                        text,
                        if (text.length >= 140) "Texto recuperado do formato antigo .$ext (extração experimental). Revise antes de gerar." else "Arquivo .$ext selecionado, mas não foi possível recuperar texto suficiente. Prefira salvar como ${if (ext == "doc") "DOCX" else "PPTX"}."
                    )
                }
                "pdf" -> extractPdf(context, uri, name, ext)
                "mp3", "mp4", "avi" -> ImportedMaterialResult(
                    fileName = name,
                    extension = ext,
                    extractedText = "",
                    message = "Arquivo de ${if (ext == "mp3") "áudio" else "vídeo"} selecionado. A transcrição automática de áudio/vídeo será a próxima etapa; esta versão ainda não transforma a fala do arquivo em texto.",
                    usableForGeneration = false
                )
                else -> ImportedMaterialResult(name, ext, "", "Formato não suportado.", false)
            }
        }.getOrElse { error ->
            ImportedMaterialResult(
                fileName = name,
                extension = ext,
                extractedText = "",
                message = "Não consegui ler este arquivo: ${error.message ?: "erro desconhecido"}.",
                usableForGeneration = false
            )
        }
    }

    private fun resultFromText(name: String, ext: String, raw: String, successMessage: String): ImportedMaterialResult {
        val clean = normalizeExtractedText(raw).take(25000)
        return if (clean.length >= 40) {
            ImportedMaterialResult(name, ext, clean, successMessage, clean.length >= 140)
        } else {
            ImportedMaterialResult(name, ext, clean, "O arquivo foi lido, mas encontrei pouco texto utilizável.", false)
        }
    }

    private fun extractZipXml(context: Context, uri: Uri, include: (String) -> Boolean): String {
        val pieces = mutableListOf<Pair<String, String>>()
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && include(entry.name)) {
                        val out = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        var total = 0
                        while (true) {
                            val read = zip.read(buffer)
                            if (read <= 0) break
                            total += read
                            if (total > 8 * 1024 * 1024) break
                            out.write(buffer, 0, read)
                        }
                        val xml = out.toString(Charsets.UTF_8.name())
                        pieces += entry.name to xmlToText(xml)
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: error("Não foi possível abrir o arquivo")
        return pieces.sortedBy { it.first }.joinToString("\n\n") { it.second }
    }

    private fun xmlToText(xml: String): String {
        return xml
            .replace(Regex("</(?:w:p|a:p|text:p|text:h)>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<(?:w:tab|text:tab)[^>]*/>", RegexOption.IGNORE_CASE), "\t")
            .replace(Regex("<[^>]+>"), " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
    }

    private fun normalizeExtractedText(raw: String): String {
        return raw
            .replace('\u0000', ' ')
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n[ \\t]+"), "\n")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    private fun readAllLimited(context: Context, uri: Uri, maxBytes: Int): ByteArray {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                total += read
                if (total > maxBytes) error("arquivo muito grande para extração local")
                out.write(buffer, 0, read)
            }
            return out.toByteArray()
        }
        error("Não foi possível abrir o arquivo")
    }

    private fun extractLegacyPrintableText(bytes: ByteArray): String {
        val pieces = linkedSetOf<String>()

        val ascii = StringBuilder()
        fun flushAscii() {
            val value = ascii.toString().trim()
            if (value.length >= 8 && value.count { it.isLetterOrDigit() } >= value.length / 2) pieces += value
            ascii.clear()
        }
        for (b in bytes) {
            val c = (b.toInt() and 0xFF).toChar()
            if (c.code in 32..126 || c in "áàâãéêíóôõúçÁÀÂÃÉÊÍÓÔÕÚÇ") ascii.append(c) else flushAscii()
        }
        flushAscii()

        val utf16 = StringBuilder()
        var i = 0
        fun flushUtf16() {
            val value = utf16.toString().trim()
            if (value.length >= 6 && value.count { it.isLetterOrDigit() } >= value.length / 2) pieces += value
            utf16.clear()
        }
        while (i + 1 < bytes.size) {
            val lo = bytes[i].toInt() and 0xFF
            val hi = bytes[i + 1].toInt() and 0xFF
            val code = lo or (hi shl 8)
            val c = code.toChar()
            if (hi == 0 && (c.code in 32..126)) utf16.append(c) else flushUtf16()
            i += 2
        }
        flushUtf16()

        return normalizeExtractedText(pieces.joinToString("\n"))
    }

    private fun extractPdf(context: Context, uri: Uri, name: String, ext: String): ImportedMaterialResult {
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= 35) {
            val text = resolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    buildString {
                        val pages = minOf(renderer.pageCount, 80)
                        for (i in 0 until pages) {
                            renderer.openPage(i).use { page ->
                                val pageText = page.textContents.joinToString(" ") { it.text }
                                if (pageText.isNotBlank()) append(pageText).append("\n\n")
                                if (length >= 25000) return@buildString
                            }
                        }
                    }
                }
            }.orEmpty()
            return resultFromText(name, ext, text, "Texto extraído do PDF.")
        }

        if (Build.VERSION.SDK_INT >= 31 && SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 13) {
            val text = resolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRendererPreV(pfd).use { renderer ->
                    buildString {
                        val pages = minOf(renderer.pageCount, 80)
                        for (i in 0 until pages) {
                            renderer.openPage(i).use { page ->
                                val pageText = page.textContents.joinToString(" ") { it.text }
                                if (pageText.isNotBlank()) append(pageText).append("\n\n")
                                if (length >= 25000) return@buildString
                            }
                        }
                    }
                }
            }.orEmpty()
            return resultFromText(name, ext, text, "Texto extraído do PDF.")
        }

        return ImportedMaterialResult(
            fileName = name,
            extension = ext,
            extractedText = "",
            message = "PDF selecionado. Para extrair o texto localmente, o tablet precisa do mecanismo PDF mais recente do Android. Se não estiver disponível, você ainda pode colar ou ditar o texto.",
            usableForGeneration = false
        )
    }
}
