package com.hzlpjames.filemanager.domain.repository

import com.hzlpjames.filemanager.domain.model.FileItem
import kotlinx.coroutines.flow.Flow

/**
 * 文件操作仓库接口
 */
interface FileRepository {
    
    /**
     * 列出目录内容
     */
    suspend fun listFiles(path: String): Result<List<FileItem>>
    
    /**
     * 创建文件夹
     */
    suspend fun createDirectory(path: String): Result<Boolean>
    
    /**
     * 删除文件或文件夹
     */
    suspend fun delete(path: String): Result<Boolean>
    
    /**
     * 复制文件
     */
    suspend fun copy(source: String, destination: String): Result<Boolean>
    
    /**
     * 移动文件
     */
    suspend fun move(source: String, destination: String): Result<Boolean>
    
    /**
     * 重命名
     */
    suspend fun rename(path: String, newName: String): Result<Boolean>
    
    /**
     * 检查文件是否存在
     */
    suspend fun exists(path: String): Boolean
    
    /**
     * 获取文件详情
     */
    suspend fun getFileInfo(path: String): Result<FileItem>
}