package com.example.music

import android.content.*
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.*

import androidx.appcompat.app.AppCompatActivity
import android.provider.MediaStore
import android.util.Log

import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast

import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.music.adapter.SongsApdapter
import com.example.music.fragment.SongFragment
import com.example.music.models.Song
import com.example.music.service.MusicService
import com.example.music.service.MusicService.Companion.ACTION_CHANGE_SONG
import com.example.music.service.MusicService.Companion.ACTION_PAUSE
import java.util.concurrent.TimeUnit
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {
    lateinit var rv_songs: RecyclerView
    lateinit var musicService: MusicService
    lateinit var adapterSong: SongsApdapter
    lateinit var btnPause: ImageView
    lateinit var tvContent: TextView
    lateinit var ivSong: ImageView
    private val listSongs = mutableListOf<Song>()
    private var isBound = false
    private val REQUEST_CODE = 1

    val broadcast = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            p1?.let {
                if (it.action == "fromNotifyToActivity") {
                    val value = it.getIntExtra("fromNotifyToActivity", -1)
                    handlerReceiver(value)
                }
            }
        }
    }

    private fun handlerReceiver(value: Int) {
        when (value) {
            ACTION_CHANGE_SONG -> {
                changeContent()
                changePausePlayBtn()
            }
            ACTION_PAUSE -> changePausePlayBtn()
            else -> Unit
        }
    }

    val connection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            p1?.let {
                val binder = p1 as MusicService.MyBinder
                musicService = binder.getService()
                isBound = true
                if (!listSongs.isEmpty()) {
                    musicService.setPlayList(listSongs)
                    if (!musicService.isPlaying()) {
                        musicService.setNewSong(0)
                    }
                    changeContent()
                    changePausePlayBtn()
                }
                LocalBroadcastManager.getInstance(this@MainActivity).registerReceiver(
                    broadcast,
                    IntentFilter("fromNotifyToActivity")
                )
            }
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        initViews()
        runtimePermission()
        initControlBottomBar()
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, MusicService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcast)
        if (!musicService.isPlaying()) stopService(Intent(this, MusicService::class.java))
    }

    private fun initControlBottomBar() {
        btnPause = findViewById<ImageView>(R.id.btn_pause)
        val layout = findViewById<RelativeLayout>(R.id.ll_layout)

        btnPause.setOnClickListener {
            musicService.togglePlayPause()
            changePausePlayBtn()
        }

        layout.setOnClickListener {
            if (isBound && listSongs.isNotEmpty()) {
                val transaction = supportFragmentManager.beginTransaction()
                transaction.add(R.id.nav_host_fragment, SongFragment(musicService))
                transaction.addToBackStack(null)
                transaction.commit()
            }
        }
    }

    private fun changePausePlayBtn() {
        if (musicService.isPlaying()) {
            btnPause.setImageResource(R.drawable.ic_baseline_pause_24)
        } else btnPause.setImageResource(R.drawable.ic_baseline_play_arrow_24)
    }

    private fun changeContent() {
        tvContent.text = musicService.cursong.title
        val byteArray = musicService.cursong.byteArray
        if (byteArray.isEmpty()) {
            ivSong.setImageResource(R.drawable.ic_baseline_music_note_24)
        } else {
            ivSong.setImageBitmap(BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size))
        }
    }

    private fun initViews() {
        tvContent = findViewById(R.id.tv_infor)
        ivSong = findViewById(R.id.img_song)
        rv_songs = findViewById(R.id.rv_songs)
        adapterSong = SongsApdapter {
            itemClick(it)
        }
        adapterSong.setData(listSongs)
        rv_songs.adapter = adapterSong
        rv_songs.layoutManager =
            LinearLayoutManager(applicationContext, LinearLayoutManager.VERTICAL, false)
    }

    private fun itemClick(pos: Int) {
        val intent = Intent(this, MusicService::class.java)
        musicService.setNewSong(pos)
        startService(intent)
        musicService.playSong()
        btnPause.setImageResource(R.drawable.ic_baseline_pause_24)
        changeContent()
    }

    private fun getAllSongs() {
        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY
                )
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(
            TimeUnit.MILLISECONDS.convert(2, TimeUnit.MINUTES).toString()
        )
        val query = contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            null
        )
        val retriever = MediaMetadataRetriever()

        query?.let { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val title = cursor.getString(1)
                val singer = cursor.getString(2)
                val duration = cursor.getLong(3)
                val uri =
                    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                retriever.setDataSource(applicationContext, uri)
                val bitmap = retriever.embeddedPicture ?: byteArrayOf()
                listSongs.add(Song(id, title, singer, duration, bitmap))
            }
            cursor.close()
        }
        adapterSong.notifyDataSetChanged()
    }

    private fun runtimePermission() {
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_CODE
            )
        } else {
            getAllSongs()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getAllSongs()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_CODE
                )
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        changePausePlayBtn()
        changeContent()
    }
}