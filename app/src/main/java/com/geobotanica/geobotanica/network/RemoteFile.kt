package com.geobotanica.geobotanica.network

data class RemoteFile(
        val url: String,
        val fileName: String,
        val relativePath: String,
        val isExternalStorage: Boolean,
        val compressedSize: Long,
        val decompressedSize: Long
)