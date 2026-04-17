package com.hzlpjames.filemanager

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

class BGFileManagerApp : Application() {

    override fun onCreate() {
        super.onCreate()
    }

    /**
     * 检查是否有存储权限
     */
    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 使用 MANAGE_EXTERNAL_STORAGE
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.MANAGE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 10 及以下使用传统权限
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
