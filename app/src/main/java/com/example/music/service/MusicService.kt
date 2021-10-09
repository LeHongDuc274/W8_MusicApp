package com.example.music.service

import android.app.PendingIntent
import android.app.Service
import android.content.ContentUris
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.music.R
import com.example.music.app.MyApp
import com.example.music.models.Song
import com.example.music.receiver.NotifyReceiver


class MusicService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private lateinit var cursong: Song
    override fun onCreate() {
        super.onCreate()
    }
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val bundle = intent?.extras
        if(bundle!=null) {
            val song = bundle.get("song")
            if (song != null) {
                song as Song
                cursong = song
                startSong(cursong)
                pushNotification(cursong)
            }
        }
        val actionFromNotify = intent?.getIntExtra("fromNotify", -1)
        actionFromNotify?.let { handlerActionFromNotify(actionFromNotify) }

        return START_NOT_STICKY
    }


    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
    }

    private fun startSong(song: Song) {
        val contentUri: Uri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            song.id
        )
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(applicationContext, contentUri)
        }
        mediaPlayer?.start()
        isPlaying = true
    }

    private fun pauseMusic() {
        if (mediaPlayer != null && isPlaying) {
            mediaPlayer?.pause()
            isPlaying = false
            pushNotification(cursong)
        }
    }

    private fun resumeMusic() {
        if (mediaPlayer != null && !isPlaying) {
            mediaPlayer?.start()
            isPlaying = true
            pushNotification(cursong)

        }
    }

    private fun pushNotification(song: Song) {
        val remoteView = RemoteViews(packageName, R.layout.notify_layout)
        initRemoteView(remoteView, song)
        initControlRemoteView(remoteView, song)
        val notification =
            NotificationCompat.Builder(this, MyApp.CHANNEL_ID)
                .setCustomContentView(remoteView)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build()
        startForeground(1, notification)
    }

    private fun initControlRemoteView(remoteView: RemoteViews, song: Song) {
        remoteView.apply {
            setOnClickPendingIntent(R.id.btn_next, getPendingIntent(ACTION_NEXT))

            setOnClickPendingIntent(R.id.btn_prev, getPendingIntent(ACTION_PREV))

            if (isPlaying) {
                setOnClickPendingIntent(R.id.btn_pause, getPendingIntent(ACTION_PAUSE))
            } else {
                setOnClickPendingIntent(R.id.btn_pause, getPendingIntent(ACTION_RESUME))
            }
        }
    }

    private fun getPendingIntent(action: Int): PendingIntent {
        val intent = Intent(this, NotifyReceiver::class.java)
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
        val minute = song.duration / 1000 / 60
        val seconds = song.duration / 1000 % 60
        remoteView.setTextViewText(R.id.tv_duration, "$minute:$seconds")
        remoteView.setImageViewResource(R.id.iv_notify, R.drawable.outline_not_started_black_24)
        remoteView.setImageViewResource(
            R.id.btn_pause,
            if (isPlaying) R.drawable.outline_pause_circle_black_24 else R.drawable.outline_not_started_black_24
        )
    }

    private fun handlerActionFromNotify(actionFromNotify: Int) {
        when (actionFromNotify) {
            ACTION_PAUSE -> {
                pauseMusic()
                Log.e("action","pause")
            }
            ACTION_RESUME -> {
                resumeMusic()
                //pauseMusic()
                Log.e("action","resume")
            }
            ACTION_PREV -> {

            }
            ACTION_NEXT -> {

            }
            else -> Unit
        }

    }


    companion object {
        const val ACTION_PREV = 0
        const val ACTION_PAUSE = 1
        const val ACTION_RESUME = 2
        const val ACTION_NEXT = 3
        const val FROM_NOTIFY = "fromNotify"
    }
}