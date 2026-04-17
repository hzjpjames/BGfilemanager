package com.hzlpjames.filemanager.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hzlpjames.filemanager.data.repository.LocalFileRepository
import com.hzlpjames.filemanager.domain.model.FileItem
import kotlinx.coroutines.launch

/**
 * 文件浏览状态
 */
data class FileBrowserState(
    val currentPath: String = "",
    val files: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedFiles: Set<FileItem> = emptySet(),
    val viewMode: ViewMode = ViewMode.LIST,
    val sortMode: SortMode = SortMode.NAME
)

enum class ViewMode {
    LIST, GRID
}

enum class SortMode {
    NAME, SIZE, DATE, TYPE
}

/**
 * 文件管理ViewModel
 */
class FileBrowserViewModel(
    private val localFileRepository: LocalFileRepository
) : ViewModel() {
    
    var state by mutableStateOf(FileBrowserState())
        private set
    
    // 导航栈
    private val navigationStack = mutableListOf<String>()
    
    /**
     * 加载存储根目录
     */
    fun loadRoots() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            
            val roots = localFileRepository.getStorageRoots()
            state = state.copy(
                files = roots,
                currentPath = "",
                isLoading = false
            )
        }
    }
    
    /**
     * 进入目录
     */
    fun navigateTo(path: String) {
        if (state.currentPath.isNotEmpty()) {
            navigationStack.add(state.currentPath)
        }
        loadDirectory(path)
    }
    
    /**
     * 返回上一级
     */
    fun navigateUp(): Boolean {
        val path = state.currentPath
        if (path.isEmpty()) return false
        
        val parentPath = path.substringBeforeLast('/')
        if (parentPath.isNotEmpty()) {
            loadDirectory(parentPath)
            return true
        } else {
            loadRoots()
            return true
        }
    }
    
    /**
     * 加载目录内容
     */
    private fun loadDirectory(path: String) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            
            val result = localFileRepository.listFiles(path)
            result.fold(
                onSuccess = { files ->
                    val sortedFiles = sortFiles(files, state.sortMode)
                    state = state.copy(
                        files = sortedFiles,
                        currentPath = path,
                        isLoading = false
                    )
                },
                onFailure = { error ->
                    state = state.copy(
                        error = error.message,
                        isLoading = false
                    )
                }
            )
        }
    }
    
    /**
     * 排序文件
     */
    private fun sortFiles(files: List<FileItem>, sortMode: SortMode): List<FileItem> {
        return when (sortMode) {
            SortMode.NAME -> files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            SortMode.SIZE -> files.sortedWith(compareBy({ !it.isDirectory }, { it.size }))
            SortMode.DATE -> files.sortedWith(compareBy({ !it.isDirectory }, { it.lastModified }))
            SortMode.TYPE -> files.sortedWith(compareBy({ !it.isDirectory }, { it.extension }))
        }
    }
    
    /**
     * 切换排序模式
     */
    fun toggleSortMode(mode: SortMode) {
        val sortedFiles = sortFiles(state.files, mode)
        state = state.copy(files = sortedFiles, sortMode = mode)
    }
    
    /**
     * 刷新当前目录
     */
    fun refresh() {
        if (state.currentPath.isEmpty()) {
            loadRoots()
        } else {
            loadDirectory(state.currentPath)
        }
    }
    
    /**
     * 删除选中的文件
     */
    fun deleteSelected() {
        viewModelScope.launch {
            state = state.copy(isLoading = true)
            
            var allSuccess = true
            state.selectedFiles.forEach { file ->
                val result = localFileRepository.delete(file.path)
                if (result.isFailure) allSuccess = false
            }
            
            state = state.copy(
                selectedFiles = emptySet(),
                isLoading = false
            )
            refresh()
        }
    }
    
    /**
     * 重命名文件
     */
    fun rename(file: FileItem, newName: String) {
        viewModelScope.launch {
            localFileRepository.rename(file.path, newName)
            refresh()
        }
    }
    
    /**
     * 创建新文件夹
     */
    fun createFolder(name: String) {
        viewModelScope.launch {
            val newPath = "${state.currentPath}/$name"
            localFileRepository.createDirectory(newPath)
            refresh()
        }
    }
    
    /**
     * 切换选择状态
     */
    fun toggleSelection(file: FileItem) {
        val newSelected = state.selectedFiles.toMutableSet()
        if (newSelected.contains(file)) {
            newSelected.remove(file)
        } else {
            newSelected.add(file)
        }
        state = state.copy(selectedFiles = newSelected)
    }
    
    /**
     * 全选/取消全选
     */
    fun toggleSelectAll() {
        state = if (state.selectedFiles.size == state.files.size) {
            state.copy(selectedFiles = emptySet())
        } else {
            state.copy(selectedFiles = state.files.toSet())
        }
    }
    
    /**
     * 清除选择
     */
    fun clearSelection() {
        state = state.copy(selectedFiles = emptySet())
    }
}