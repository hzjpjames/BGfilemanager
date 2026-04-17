package com.hzlpjames.filemanager.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hzlpjames.filemanager.domain.model.FileItem
import com.hzlpjames.filemanager.domain.model.FileType
import com.hzlpjames.filemanager.domain.model.getFileType
import com.hzlpjames.filemanager.ui.viewmodel.FileBrowserState
import com.hzlpjames.filemanager.ui.viewmodel.SortMode
import com.hzlpjames.filemanager.ui.viewmodel.ViewMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FileBrowserScreen(
    state: FileBrowserState,
    onNavigateUp: () -> Unit,
    onNavigateTo: (String) -> Unit,
    onRefresh: () -> Unit,
    onSortModeChange: (SortMode) -> Unit,
    onToggleSelectAll: () -> Unit,
    onToggleSelection: (FileItem) -> Unit,
    onDeleteSelected: () -> Unit,
    onCreateFolder: (String) -> Unit,
    onRename: (FileItem, String) -> Unit
) {
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var selectedFile by remember { mutableStateOf<FileItem?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    
    val isSelecting = state.selectedFiles.isNotEmpty()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.currentPath.isEmpty()) "大师文件管理器" 
                               else state.currentPath.substringAfterLast('/'),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    if (isSelecting) {
                        Text(
                            text = "已选 ${state.selectedFiles.size}",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        IconButton(onClick = onToggleSelectAll) {
                            Icon(Icons.Default.SelectAll, "全选")
                        }
                        IconButton(onClick = onDeleteSelected) {
                            Icon(Icons.Default.Delete, "删除")
                        }
                    } else {
                        IconButton(onClick = { showSortDialog = true }) {
                            Icon(Icons.Default.Sort, "排序")
                        }
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, "刷新")
                        }
                        IconButton(onClick = { showCreateFolderDialog = true }) {
                            Icon(Icons.Default.CreateNewFolder, "新建文件夹")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                state.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "错误",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("加载失败: ${state.error}")
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onRefresh) {
                            Text("重试")
                        }
                    }
                }
                
                state.files.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = "空文件夹",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("空文件夹")
                    }
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(state.files, key = { it.path }) { file ->
                            FileItemRow(
                                file = file,
                                isSelected = state.selectedFiles.contains(file),
                                isSelecting = isSelecting,
                                onClick = {
                                    if (isSelecting) {
                                        onToggleSelection(file)
                                    } else if (file.isDirectory) {
                                        onNavigateTo(file.path)
                                    }
                                },
                                onLongClick = {
                                    onToggleSelection(file)
                                },
                                onMoreClick = {
                                    selectedFile = file
                                    showRenameDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // 创建文件夹对话框
    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { name ->
                onCreateFolder(name)
                showCreateFolderDialog = false
            }
        )
    }
    
    // 排序对话框
    if (showSortDialog) {
        SortDialog(
            currentSortMode = state.sortMode,
            onDismiss = { showSortDialog = false },
            onSelect = { mode ->
                onSortModeChange(mode)
                showSortDialog = false
            }
        )
    }
    
    // 重命名对话框
    if (showRenameDialog && selectedFile != null) {
        RenameDialog(
            currentName = selectedFile!!.name,
            onDismiss = { 
                showRenameDialog = false
                selectedFile = null
            },
            onConfirm = { newName ->
                onRename(selectedFile!!, newName)
                showRenameDialog = false
                selectedFile = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItemRow(
    file: FileItem,
    isSelected: Boolean,
    isSelecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    ListItem(
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .fillMaxWidth(),
        headlineContent = {
            Text(
                text = file.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            if (!file.isDirectory) {
                Text(
                    text = file.displaySize,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        leadingContent = {
            Icon(
                imageVector = getFileIcon(file),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = getFileIconColor(file)
            )
        },
        trailingContent = {
            if (!isSelecting) {
                IconButton(onClick = onMoreClick) {
                    Icon(Icons.Default.MoreVert, "更多")
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = backgroundColor)
    )
}

@Composable
fun getFileIcon(file: FileItem): ImageVector {
    return when {
        file.isDirectory -> Icons.Default.Folder
        else -> when (file.getFileType()) {
            FileType.IMAGE -> Icons.Default.Image
            FileType.VIDEO -> Icons.Default.VideoFile
            FileType.AUDIO -> Icons.Default.AudioFile
            FileType.DOCUMENT -> Icons.Default.Description
            FileType.APK -> Icons.Default.Android
            FileType.ARCHIVE -> Icons.Default.Archive
            FileType.OTHER -> Icons.Default.InsertDriveFile
        }
    }
}

@Composable
fun getFileIconColor(file: FileItem) = when {
    file.isDirectory -> MaterialTheme.colorScheme.primary
    else -> when (file.getFileType()) {
        FileType.IMAGE -> MaterialTheme.colorScheme.tertiary
        FileType.VIDEO -> MaterialTheme.colorScheme.error
        FileType.AUDIO -> MaterialTheme.colorScheme.secondary
        FileType.DOCUMENT -> MaterialTheme.colorScheme.primary
        FileType.APK -> MaterialTheme.colorScheme.tertiary
        FileType.ARCHIVE -> MaterialTheme.colorScheme.outline
        FileType.OTHER -> MaterialTheme.colorScheme.outline
    }
}

@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建文件夹") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("文件夹名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun SortDialog(
    currentSortMode: SortMode,
    onDismiss: () -> Unit,
    onSelect: (SortMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("排序方式") },
        text = {
            Column {
                SortMode.values().forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = mode == currentSortMode,
                            onClick = { onSelect(mode) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when (mode) {
                                SortMode.NAME -> "按名称"
                                SortMode.SIZE -> "按大小"
                                SortMode.DATE -> "按时间"
                                SortMode.TYPE -> "按类型"
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
fun RenameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("新名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && name != currentName) onConfirm(name) },
                enabled = name.isNotBlank() && name != currentName
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}