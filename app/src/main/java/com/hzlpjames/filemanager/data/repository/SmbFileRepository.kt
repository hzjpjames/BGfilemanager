package com.hzlpjames.filemanager.data.repository

import android.util.Log
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msdtyp.FileTime
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.msfscc.fileinformation.FileAllInformation
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.msfscc.fileinformation.FileRenameInformation
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.protocol.commons.EnumWithValue
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskEntry
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.File
import com.hzlpjames.filemanager.domain.model.FileItem
import com.hzlpjames.filemanager.domain.repository.FileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

class SmbFileRepository : FileRepository {

    companion object {
        private const val TAG = "SmbFileRepository"
    }

    private var client: SMBClient? = null
    private var connection: Connection? = null
    private var session: Session? = null
    private var share: DiskShare? = null
    private var currentShareName: String? = null

    suspend fun connect(
        host: String,
        username: String = "",
        password: String = "",
        shareName: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            disconnect()
            client = SMBClient()
            connection = client?.connect(host)
            val authContext = if (username.isEmpty()) {
                AuthenticationContext.anonymous()
            } else {
                AuthenticationContext(username, password.toCharArray(), null)
            }
            session = connection?.authenticate(authContext)
            share = session?.connectShare(shareName) as? DiskShare
            currentShareName = shareName
            Result.success(share != null)
        } catch (e: Exception) {
            Log.e(TAG, "连接失败", e)
            Result.failure(e)
        }
    }

    fun disconnect() {
        try {
            share?.close()
            session?.close()
            connection?.close()
            client?.close()
        } catch (e: Exception) {
            Log.e(TAG, "断开连接失败", e)
        } finally {
            share = null
            session = null
            connection = null
            client = null
            currentShareName = null
        }
    }

    override suspend fun listFiles(path: String): Result<List<FileItem>> = withContext(Dispatchers.IO) {
        try {
            val smbShare = share ?: return@withContext Result.failure(Exception("未连接"))
            val smbPath = path.trimStart('/')
            val files = smbShare.list(smbPath).map { info ->
                val isDir = EnumWithValue.EnumUtils.isSet(
                    info.fileAttributes,
                    FileAttributes.FILE_ATTRIBUTE_DIRECTORY
                )
                val fileSize = if (isDir) 0L else info.endOfFile
                val lastMod = info.lastWriteTime?.toEpochMillis() ?: 0L
                val isHidden = EnumWithValue.EnumUtils.isSet(
                    info.fileAttributes,
                    FileAttributes.FILE_ATTRIBUTE_HIDDEN
                )
                FileItem(
                    name = info.fileName,
                    path = "$path/${info.fileName}",
                    isDirectory = isDir,
                    size = fileSize,
                    lastModified = lastMod,
                    isHidden = isHidden
                )
            }.filter { it.name != "." && it.name != ".." }
                .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            Result.success(files)
        } catch (e: java.io.FileNotFoundException) {
            Result.success(emptyList())
        } catch (e: Exception) {
            Log.e(TAG, "列出文件失败", e)
            Result.success(emptyList())
        }
    }

    override suspend fun createDirectory(path: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val smbShare = share ?: return@withContext Result.failure(Exception("未连接"))
            val smbPath = path.trimStart('/')
            smbShare.mkdir(smbPath)
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "创建目录失败", e)
            Result.failure(e)
        }
    }

    override suspend fun delete(path: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val smbShare = share ?: return@withContext Result.failure(Exception("未连接"))
            val smbPath = path.trimStart('/')
            val isDir = try {
                val info = smbShare.getFileInformation(smbPath)
                EnumWithValue.EnumUtils.isSet(
                    info.basicInformation.fileAttributes,
                    FileAttributes.FILE_ATTRIBUTE_DIRECTORY
                )
            } catch (e: Exception) { false }
            if (isDir) {
                smbShare.rmdir(smbPath, true)
            } else {
                smbShare.rm(smbPath)
            }
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "删除失败", e)
            Result.failure(e)
        }
    }

    override suspend fun copy(source: String, destination: String): Result<Boolean> {
        return Result.failure(Exception("SMB不支持直接复制，请使用下载/上传"))
    }

    override suspend fun move(source: String, destination: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val smbShare = share ?: return@withContext Result.failure(Exception("未连接"))
            val srcPath = source.trimStart('/')
            val destPath = destination.trimStart('/')
            val destName = destPath.substringAfterLast('/')
            val destDir = destPath.substringBeforeLast('/', "")
            
            // Open source file and rename it
            val entry = smbShare.open(
                srcPath,
                setOf(AccessMask.DELETE, AccessMask.GENERIC_WRITE),
                null,
                setOf(SMB2ShareAccess.FILE_SHARE_DELETE, SMB2ShareAccess.FILE_SHARE_WRITE, SMB2ShareAccess.FILE_SHARE_READ),
                SMB2CreateDisposition.FILE_OPEN,
                null
            )
            entry.rename(destName)
            entry.close()
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "移动失败", e)
            Result.failure(e)
        }
    }

    override suspend fun rename(path: String, newName: String): Result<Boolean> {
        val parent = path.substringBeforeLast('/')
        val newPath = if (parent.isNotEmpty()) "$parent/$newName" else newName
        return move(path, newPath)
    }

    override suspend fun exists(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val smbShare = share ?: return@withContext false
            val smbPath = path.trimStart('/')
            smbShare.fileExists(smbPath) || smbShare.folderExists(smbPath)
        } catch (e: Exception) { false }
    }

    override suspend fun getFileInfo(path: String): Result<FileItem> = withContext(Dispatchers.IO) {
        try {
            val smbShare = share ?: return@withContext Result.failure(Exception("未连接"))
            val smbPath = path.trimStart('/')
            val info = smbShare.getFileInformation(smbPath)
            val isDir = EnumWithValue.EnumUtils.isSet(
                info.basicInformation.fileAttributes,
                FileAttributes.FILE_ATTRIBUTE_DIRECTORY
            )
            val fileSize = if (isDir) 0L else info.standardInformation.endOfFile
            Result.success(FileItem(
                name = path.substringAfterLast('/'),
                path = path,
                isDirectory = isDir,
                size = fileSize,
                lastModified = info.basicInformation.lastWriteTime?.toEpochMillis() ?: 0L,
                isHidden = EnumWithValue.EnumUtils.isSet(
                    info.basicInformation.fileAttributes,
                    FileAttributes.FILE_ATTRIBUTE_HIDDEN
                )
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getInputStream(path: String): InputStream? {
        return try {
            val smbShare = share ?: return null
            val smbPath = path.trimStart('/')
            val file = smbShare.openFile(
                smbPath, setOf(AccessMask.GENERIC_READ), null,
                setOf(SMB2ShareAccess.FILE_SHARE_READ), SMB2CreateDisposition.FILE_OPEN, null
            )
            file.inputStream
        } catch (e: Exception) {
            Log.e(TAG, "获取输入流失败", e)
            null
        }
    }

    fun getOutputStream(path: String): OutputStream? {
        return try {
            val smbShare = share ?: return null
            val smbPath = path.trimStart('/')
            val file = smbShare.openFile(
                smbPath, setOf(AccessMask.GENERIC_WRITE), null,
                setOf(SMB2ShareAccess.FILE_SHARE_WRITE), SMB2CreateDisposition.FILE_OVERWRITE_IF, null
            )
            file.outputStream
        } catch (e: Exception) {
            Log.e(TAG, "获取输出流失败", e)
            null
        }
    }

    val isConnected: Boolean
        get() = share != null && connection?.isConnected == true
}
