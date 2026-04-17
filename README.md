# BGfilemanager

大师文件管理器 - Android 文件管理器应用

## 功能特点

- 📁 本地文件管理：复制、移动、删除、重命名、新建文件夹
- 🔍 高级搜索：按文件名、类型、大小、日期搜索
- ⭐ 收藏夹：收藏常用目录
- 📋 最近文件：快速访问最近打开的文件
- 🗑️ 回收站：安全删除，防止误删
- 🌐 局域网共享：访问 SMB 共享文件夹
- 🔄 双向操作：上传、下载、删除、重命名远程文件
- 📱 自动发现：自动扫描局域网设备

## 技术栈

- Kotlin
- Jetpack Compose
- MVVM + Clean Architecture
- SMB 协议 (jCIFS-ng)
- 最低 SDK: Android 7.0 (API 24)

## 构建

项目已配置 GitHub Actions，每次 push 到 main 分支会自动构建 APK。