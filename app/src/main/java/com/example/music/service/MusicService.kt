package com.example.music.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.ContentUris
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.music.MainActivity
import com.example.music.R
import com.example.music.app.MyApp
import com.example.music.models.Song
import com.example.music.receiver.NotifyReceiver
import java.io.IOException
import kotlin.random.Random


class MusicService : Service() {
    private var mediaPlayer = MediaPlayer()

    // var isPlaying = false
    private var playlist = mutableListOf<Song>()
    private val listSongPos = mutableListOf<Int>()
    lateinit var cursong: Song
    private var songPos = 0
    var shuffle = false
    var repeat = false


    inner class MyBinder() : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onCreate() {
        super.onCreate()
        mediaPlayer.setOnCompletionListener {
            nextSong()
        }
    }

    override fun onBind(p0: Intent?): IBinder {
        return MyBinder()
    }

    fun setPlayList(list: MutableList<Song>) {
        playlist = list
        for (i in 0..playlist.size) {
            listSongPos.add(i)
        }
    }

    fun setNewSong(newPos: Int) {
        songPos = newPos
        mediaPlayer.reset()
        cursong = playlist[songPos]
        val contentUri: Uri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            cursong.id
        )
        mediaPlayer = MediaPlayer.create(applicationContext, contentUri)
    }

    fun playSong() {
        mediaPlayer.start()
    }

    fun togglePlayPause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        } else {
            mediaPlayer.start()
        }
        pushNotification(cursong)
    }

    fun setRepeat() {
        repeat = !repeat
        if (repeat) {
            // repeat == true
            mediaPlayer.isLooping = true
        } else if (!repeat) {
            //repeat = false
            mediaPlayer.isLooping = false
        }

    }

    fun setShuffle() {
        shuffle = !shuffle
    }

    fun nextSong() {
        if (!repeat && !shuffle) { // repeat disable _>next
            if (songPos < playlist.size - 1) {
                setNewSong(songPos + 1)
                playSong()
            }
        } else if (!repeat && shuffle) { // next random
            val random = Random.nextInt(0, playlist.size)
            setNewSong(random)
            playSong()
        } else { // repeat enable -> seek to start
            mediaPlayer.seekTo(0)
            mediaPlayer.isLooping = true
        }
        pushNotification(cursong)
    }

    fun prevSong() {
        if (!repeat) {
            if (mediaPlayer.currentPosition > 5000) {
                mediaPlayer.seekTo(0)
                return
            }
            if (songPos > 0) {
                setNewSong(songPos - 1)
                playSong()
            }
        } else {
            mediaPlayer.seekTo(0)
            mediaPlayer.isLooping = true
        }
        pushNotification(cursong)

    }

    fun isPlaying() = mediaPlayer.isPlaying
    fun getCurSong() = cursong
    fun getMediaPlayer() = mediaPlayer

    fun getMediaCurrentPos() = mediaPlayer.currentPosition
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        pushNotification(cursong)
        val actionFromNotify = intent?.getIntExtra("fromNotify", -1)
        actionFromNotify?.let { handlerActionFromNotify(actionFromNotify) }

        return START_NOT_STICKY
    }


    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.stop()
        mediaPlayer.release()
    }


    private fun pushNotification(song: Song) {
        val remoteView = RemoteViews(packageName, R.layout.notify_layout)
        initRemoteView(remoteView, song)
        val intent = Intent(this, MainActivity::class.java)

        initControlRemoteView(remoteView, song)
        val notification =
            NotificationCompat.Builder(this, MyApp.CHANNEL_ID)
                .setCustomContentView(remoteView)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setSound(null)
                .build()
        startForeground(1, notification)
    }

    private fun initControlRemoteView(remoteView: RemoteViews, song: Song) {
        remoteView.apply {
            setOnClickPendingIntent(R.id.btn_next, getPendingIntent(ACTION_NEXT))

            setOnClickPendingIntent(R.id.btn_prev, getPendingIntent(ACTION_PREV))

            setOnClickPendingIntent(R.id.btn_pause, getPendingIntent(ACTION_PAUSE))

            setOnClickPendingIntent(R.id.btn_cancel,getPendingIntent(ACTION_CANCEL))

        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun getPendingIntent(action: Int): PendingIntent {
        val intent = Intent(this,NotifyReceiver::class.java)
        intent.putExtra("fromNotify", action)
        intent.action = FROM_NOTIFY
        return PendingIntent.getBroadcast(
            applicationContext,
            action,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun initRemoteView(remoteView: RemoteViews, song: Song) {
        remoteView.setTextViewText(R.id.tv_title, song.title)
        remoteView.setTextViewText(R.id.tv_singer, song.singer)

        remoteView.setImageViewResource(R.id.iv_notify, R.drawable.outline_not_started_black_24)
        remoteView.setImageViewResource(
            R.id.btn_pause,
            if (mediaPlayer.isPlaying) R.drawable.outline_pause_circle_black_24 else R.drawable.outline_not_started_black_24
        )
    }

    private fun handlerActionFromNotify(actionFromNotify: Int) {
        when (actionFromNotify) {
            ACTION_PAUSE -> {
                togglePlayPause()
                pushNotification(cursong)
                sendToActivity(ACTION_PAUSE)
            }
            ACTION_PREV -> {
                prevSong()
                sendToActivity(ACTION_CHANGE_SONG)
            }
            ACTION_NEXT -> {
                nextSong()
                sendToActivity(ACTION_CHANGE_SONG)

            }
            ACTION_CANCEL -> {
                stopForeground(true)
                mediaPlayer.reset()
                sendToActivity(ACTION_CANCEL)
            }
            else -> Unit
        }
    }

    private fun sendToActivity(action: Int) {
        val intent = Intent()
        intent.action = "fromNotifyToActivity"
        intent.putExtra("fromNotifyToActivity",action)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

    }


    companion object {
        const val ACTION_PREV = 0
        const val ACTION_PAUSE = 1
        const val ACTION_RESUME = 2
        const val ACTION_CANCEL = 4
        const val ACTION_NEXT = 3
        const val ACTION_CHANGE_SONG = 5
        const val FROM_NOTIFY = "fromNotify"
    }
}