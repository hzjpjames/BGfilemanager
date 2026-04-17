package com.hzlpjames.filemanager.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.hzlpjames.filemanager.data.repository.LocalFileRepository
import com.hzlpjames.filemanager.ui.screens.FileBrowserScreen
import com.hzlpjames.filemanager.ui.theme.BGfilemanagerTheme
import com.hzlpjames.filemanager.ui.viewmodel.FileBrowserViewModel

val LocalFileRepository = staticCompositionLocalOf<LocalFileRepository> {
    error("No LocalFileRepository provided")
}

class MainActivity : ComponentActivity() {
    
    private lateinit var fileRepository: LocalFileRepository
    private lateinit var viewModel: FileBrowserViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        fileRepository = LocalFileRepository(this)
        viewModel = FileBrowserViewModel(fileRepository)
        
        // 加载根目录
        viewModel.loadRoots()
        
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
                        onRefresh = { viewModel.refresh() },
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
}