package com.hzlpjames.filemanager.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.hzlpjames.filemanager.domain.model.FileItem
import com.hzlpjames.filemanager.domain.repository.FileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * 本地文件仓库实现
 */
class LocalFileRepository(
    private val context: Context
) : FileRepository {
    
    override suspend fun listFiles(path: String): Result<List<FileItem>> = withContext(Dispatchers.IO) {
        try {
            val dir = File(path)
            if (!dir.exists() || !dir.isDirectory) {
                return@withContext Result.failure(Exception("目录不存在"))
            }
            
            val files = dir.listFiles()?.map { file ->
                FileItem(
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = file.isDirectory,
                    size = if (file.isFile) file.length() else 0,
                    lastModified = file.lastModified(),
                    isHidden = file.name.startsWith(".")
                )
            }?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) 
                ?: emptyList()
            
            Result.success(files)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun createDirectory(path: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val dir = File(path)
            val result = if (dir.exists()) true else dir.mkdirs()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun delete(path: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            val result = if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun copy(source: String, destination: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val srcFile = File(source)
            val destFile = File(destination)
            
            if (srcFile.isDirectory) {
                srcFile.copyRecursively(destFile, true)
            } else {
                destFile.parentFile?.mkdirs()
                FileInputStream(srcFile).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun move(source: String, destination: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val srcFile = File(source)
            val destFile = File(destination)
            val result = srcFile.renameTo(destFile)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun rename(path: String, newName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            val newFile = File(file.parentFile, newName)
            val result = file.renameTo(newFile)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun exists(path: String): Boolean = withContext(Dispatchers.IO) {
        File(path).exists()
    }
    
    override suspend fun getFileInfo(path: String): Result<FileItem> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) {
                return@withContext Result.failure(Exception("文件不存在"))
            }
            Result.success(
                FileItem(
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = file.isDirectory,
                    size = if (file.isFile) file.length() else 0,
                    lastModified = file.lastModified(),
                    isHidden = file.name.startsWith(".")
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取存储根目录
     */
    fun getStorageRoots(): List<FileItem> {
        val roots = mutableListOf<FileItem>()

        // 内部存储 - 使用 context.filesDir 向上追溯
        val internalDir = context.filesDir
        var storagePath = internalDir.absolutePath
        if (storagePath.contains("/Android/data/")) {
            storagePath = storagePath.substringBefore("/Android/data/")
        } else if (storagePath.contains("/Android")) {
            storagePath = storagePath.substringBefore("/Android")
        }
        val internalStorage = File(storagePath)
        if (internalStorage.exists() && internalStorage.canRead()) {
            roots.add(FileItem(
                name = "内部存储",
                path = internalStorage.absolutePath,
                isDirectory = true
            ))
        }

        // 备选: /storage/emulated/0 (需要权限)
        val altPath = File("/storage/emulated/0")
        if (altPath.exists() && altPath.canRead() && roots.isEmpty()) {
            roots.add(FileItem(
                name = "内部存储",
                path = altPath.absolutePath,
                isDirectory = true
            ))
        }

        // 外部SD卡
        val externalDirs = context.getExternalFilesDirs(null)
        externalDirs.filterNotNull().forEach { dir ->
            if (!dir.absolutePath.contains("emulated")) {
                val sdPath = dir.absolutePath.substringBefore("Android")
                val sdCard = File(sdPath)
                if (sdCard.exists() && sdCard.canRead()) {
                    roots.add(FileItem(
                        name = "SD卡",
                        path = sdCard.absolutePath,
                        isDirectory = true
                    ))
                }
            }
        }

        return roots
    }
}