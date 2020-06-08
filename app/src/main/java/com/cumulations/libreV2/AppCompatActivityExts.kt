package com.cumulations.libreV2

import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.appcompat.app.AppCompatActivity

fun AppCompatActivity.isPermissionGranted(permission: String) : Boolean =
        ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

fun AppCompatActivity.shouldShowPermissionRationale(permission: String) : Boolean =
        ActivityCompat.shouldShowRequestPermissionRationale(this, permission)

fun AppCompatActivity.requestPermission(permission: String, requestId: Int) =
        ActivityCompat.requestPermissions(this, arrayOf(permission), requestId)

fun AppCompatActivity.batchRequestPermissions(permissions: Array<String>, requestId: Int) =
        ActivityCompat.requestPermissions(this, permissions, requestId)