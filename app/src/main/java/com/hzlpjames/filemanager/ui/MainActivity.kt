package com.hzlpjames.filemanager.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.hzlpjames.filemanager.data.repository.LocalFileRepository
import com.hzlpjames.filemanager.ui.screens.FileBrowserScreen
import com.hzlpjames.filemanager.ui.theme.BGfilemanagerTheme
import com.hzlpjames.filemanager.ui.viewmodel.FileBrowserViewModel

class MainActivity : ComponentActivity() {
    
    private lateinit var fileRepository: LocalFileRepository
    private lateinit var viewModel: FileBrowserViewModel
    private var hasPermission by mutableStateOf(false)
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { 
        // 用户从设置返回后检查权限
        checkAndLoadRoots()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        fileRepository = LocalFileRepository(this)
        viewModel = FileBrowserViewModel(fileRepository)
        
        // 检查并请求存储权限
        checkAndLoadRoots()
        
        setContent {
            BGfilemanagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FileBrowserScreen(
                        state = viewModel.state,
                        onNavigateUp = { viewModel.navigateUp() },
                        onNavigateTo = { viewModel.navigateTo(it) },
                        onRefresh = { checkAndLoadRoots() },
                        onSortModeChange = { viewModel.toggleSortMode(it) },
                        onToggleSelectAll = { viewModel.toggleSelectAll() },
                        onToggleSelection = { viewModel.toggleSelection(it) },
                        onDeleteSelected = { viewModel.deleteSelected() },
                        onCreateFolder = { viewModel.createFolder(it) },
                        onRename = { file, name -> viewModel.rename(file, name) }
                    )
                }
            }
        }
    }
    
    private fun checkAndLoadRoots() {
        hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true // Android 10 及以下不需要特殊权限
        }
        
        if (hasPermission) {
            viewModel.loadRoots()
        } else {
            // 请求 MANAGE_EXTERNAL_STORAGE 权限
            requestPermissionLauncher.launch(
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
            // 尝试加载，可能仍能访问部分目录
            viewModel.loadRoots()
        }
    }
}