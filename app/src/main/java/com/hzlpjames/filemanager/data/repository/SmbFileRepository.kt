package com.hzlpjames.filemanager.data.repository

import android.util.Log
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
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

    suspend fun listShares(host: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val tempClient = SMBClient()
            val tempConnection = tempClient.connect(host)
            val tempSession = tempConnection.authenticate(AuthenticationContext.anonymous())
            val shares = tempSession.listShares().map { it.name }
            tempSession.close()
            tempConnection.close()
            tempClient.close()
            Result.success(shares)
        } catch (e: Exception) {
            Log.e(TAG, "列出共享失败", e)
            Result.failure(e)
        }
    }

    override suspend fun listFiles(path: String): Result<List<FileItem>> = withContext(Dispatchers.IO) {
        try {
            val smbShare = share ?: return@withContext Result.failure(Exception("未连接"))
            val smbPath = path.trimStart('/')
            val files = smbShare.list(smbPath).map { smbFile ->
                val isDir = smbFile.fileInformation.isDirectory
                val fileSize = if (isDir) 0L else {
                    try { smbFile.fileInformation.size } catch (e: Exception) { 0L }
                }
                val lastMod = try {
                    smbFile.lastWriteTime?.time
                } catch (e: Exception) { 0L }
                val isHidden = try {
                    smbFile.isHidden
                } catch (e: Exception) { false }
                FileItem(
                    name = smbFile.fileName,
                    path = "$path/${smbFile.fileName}",
                    isDirectory = isDir,
                    size = fileSize,
                    lastModified = lastMod,
                    isHidden = isHidden
                )
            }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
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
            val isDir = try { smbShare.getFileInformation(smbPath).isDirectory } catch (e: Exception) { false }
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
            smbShare.rename(srcPath, destPath)
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
            smbShare.fileExists(smbPath)
        } catch (e: Exception) { false }
    }

    override suspend fun getFileInfo(path: String): Result<FileItem> = withContext(Dispatchers.IO) {
        try {
            val smbShare = share ?: return@withContext Result.failure(Exception("未连接"))
            val smbPath = path.trimStart('/')
            val smbFile = smbShare.openFile(
                smbPath, setOf(AccessMask.GENERIC_READ), null,
                setOf(SMB2ShareAccess.FILE_SHARE_READ), SMB2CreateDisposition.FILE_OPEN, null
            )
            val isDir = try { smbFile.fileInformation.isDirectory } catch (e: Exception) { false }
            val fileSize = if (isDir) 0L else {
                try { smbFile.fileInformation.size } catch (e: Exception) { 0L }
            }
            smbFile.close()
            Result.success(FileItem(
                name = path.substringAfterLast('/'),
                path = path,
                isDirectory = isDir,
                size = fileSize,
                lastModified = 0,
                isHidden = false
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
