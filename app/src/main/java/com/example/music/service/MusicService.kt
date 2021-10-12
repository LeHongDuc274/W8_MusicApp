package com.example.music.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.ContentUris
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.music.MainActivity
import com.example.music.R
import com.example.music.SplashActivity
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
    private var thread = Thread()


    inner class MyBinder() : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onCreate() {
        super.onCreate()

        stopThread()
        startThread()
    }

    override fun onBind(p0: Intent?): IBinder {
        return MyBinder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        pushNotification(cursong)
        Log.e("size" ,cursong.title)
        val actionFromNotify = intent?.getIntExtra("fromNotify", -1)
        actionFromNotify?.let { handlerActionFromNotify(actionFromNotify) }
        return START_NOT_STICKY
    }


    override fun onDestroy() {
        super.onDestroy()
        stopThread()
        mediaPlayer.stop()
        mediaPlayer.release()
    }

    fun setPlayList(list: MutableList<Song>) {
        playlist = list
        for (i in 0..playlist.size) {
            listSongPos.add(i)
        }
    }

    fun setNewSong(newPos: Int) {
        songPos = newPos
        mediaPlayer.stop()
        cursong = playlist[songPos]
        val contentUri: Uri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            cursong.id
        )
        mediaPlayer = MediaPlayer.create(applicationContext, contentUri)
        mediaPlayer.setOnCompletionListener {
            nextSong()
        }
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
        sendToActivity(ACTION_PAUSE)
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
            playSong()
        }
        pushNotification(cursong)
        sendToActivity(ACTION_CHANGE_SONG)
    }

    fun prevSong() {

        if (mediaPlayer.currentPosition > 20000) {
            mediaPlayer.seekTo(0)
            return
        } else if (songPos > 0) {
            songPos--
            setNewSong(songPos)
            playSong()
        }
        sendToActivity(ACTION_CHANGE_SONG)
        pushNotification(cursong)
    }

    fun isPlaying() = mediaPlayer.isPlaying
    fun getCurSong() = cursong

    fun getMediaCurrentPos() = mediaPlayer.currentPosition


    private fun pushNotification(song: Song) {
        val remoteView = RemoteViews(packageName, R.layout.notify_layout)
        initRemoteView(remoteView, song)
        val pending = PendingIntent.getActivity(
            this, 0, Intent(this, SplashActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        initControlRemoteView(remoteView, song)
        val notification =
            NotificationCompat.Builder(this, MyApp.CHANNEL_ID)
                .setCustomContentView(remoteView)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pending)
                .setSound(null)
                .build()
        startForeground(1, notification)
    }

    private fun initControlRemoteView(remoteView: RemoteViews, song: Song) {
        remoteView.apply {
            setOnClickPendingIntent(R.id.btn_next, getPendingIntent(ACTION_NEXT))

            setOnClickPendingIntent(R.id.btn_prev, getPendingIntent(ACTION_PREV))

            setOnClickPendingIntent(R.id.btn_pause, getPendingIntent(ACTION_PAUSE))

            setOnClickPendingIntent(R.id.btn_cancel, getPendingIntent(ACTION_CANCEL))

        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
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
        setImageNotify(remoteView)
        remoteView.setImageViewResource(
            R.id.btn_pause,
            if (mediaPlayer.isPlaying) R.drawable.outline_pause_circle_black_24 else R.drawable.outline_not_started_black_24
        )
    }

    private fun setImageNotify(remoteView: RemoteViews) {
        val byteArray = cursong.byteArray
        if (byteArray.isNotEmpty()) {
            remoteView.setImageViewBitmap(
                R.id.iv_notify,
                BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            )
        } else remoteView.setImageViewResource(R.id.iv_notify, R.drawable.ic_baseline_music_note_24)
    }

    private fun handlerActionFromNotify(actionFromNotify: Int) {
        when (actionFromNotify) {
            ACTION_PAUSE -> {
                togglePlayPause()
                pushNotification(cursong)
            }
            ACTION_PREV -> {
                prevSong()
            }
            ACTION_NEXT -> {
                nextSong()
            }
            ACTION_CANCEL -> {
                stopForeground(true)
                mediaPlayer.stop()
                sendToActivity(ACTION_CANCEL)
            }
            else -> Unit
        }
    }

    private fun sendToActivity(action: Int) {
        val intent = Intent()
        intent.action = "fromNotifyToActivity"
        intent.putExtra("fromNotifyToActivity", action)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

    }

    fun seekTo(newPos: Int) {
        mediaPlayer.seekTo(newPos * 1000)
    }

    val runnable = Runnable {
        while (true) {

            val curPos = this.getMediaCurrentPos()
            if (isPlaying()) {
                handler.sendMessage(handler.obtainMessage(1, curPos, 0))
            }
            try {
                Thread.sleep(200)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }
    val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {

            when (msg.what) {
                1 -> {
                    val intent = Intent()
                    intent.action = "updatePosition"
                    val curPosition = msg.arg1 / 1000
                    intent.putExtra("value", curPosition)
                    // Log.e("pos", curPosition.toString())
                    LocalBroadcastManager.getInstance(this@MusicService).sendBroadcast(intent)
                }
            }
        }
    }

    fun startThread() {
        thread = Thread(runnable)
        handler.post { thread.start() }
    }

    fun stopThread() {
        handler.removeCallbacksAndMessages(null)
        if (thread.isInterrupted == false) thread.interrupt()
    }

    companion object {
        const val ACTION_UPDATE_POSITION = 6
        const val ACTION_PREV = 0
        const val ACTION_PAUSE = 1
        const val ACTION_RESUME = 2
        const val ACTION_CANCEL = 4
        const val ACTION_NEXT = 3
        const val ACTION_CHANGE_SONG = 5
        const val FROM_NOTIFY = "fromNotify"
    }
}