package com.hzlpjames.filemanager.domain.model

/**
 * 文件/文件夹项
 */
data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0,
    val lastModified: Long = 0,
    val mimeType: String? = null,
    val isHidden: Boolean = false
) {
    val extension: String
        get() = if (name.contains(".")) name.substringAfterLast(".") else ""
    
    val displaySize: String
        get() = formatSize(size)
    
    companion object {
        fun formatSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
                else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
            }
        }
    }
}

/**
 * 文件类型
 */
enum class FileType {
    DOCUMENT,
    IMAGE,
    VIDEO,
    AUDIO,
    ARCHIVE,
    APK,
    OTHER
}

fun FileItem.getFileType(): FileType {
    return when (extension.lowercase()) {
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md" -> FileType.DOCUMENT
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg" -> FileType.IMAGE
        "mp4", "mkv", "avi", "mov", "3gp", "webm" -> FileType.VIDEO
        "mp3", "wav", "flac", "aac", "ogg", "m4a" -> FileType.AUDIO
        "zip", "rar", "7z", "tar", "gz" -> FileType.ARCHIVE
        "apk" -> FileType.APK
        else -> FileType.OTHER
    }
}