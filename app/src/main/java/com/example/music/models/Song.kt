package com.example.music.models

import android.net.Uri
import java.io.Serializable


data class Song(
    val id: Long,
    val title: String,
    val singer: String?,
    val duration: Long,
    val byteArray: ByteArray
) :Serializable