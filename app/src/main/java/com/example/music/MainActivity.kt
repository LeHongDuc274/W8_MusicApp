package com.example.music

import android.content.*
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.*

import androidx.appcompat.app.AppCompatActivity
import android.provider.MediaStore
import android.util.Log
import android.widget.*

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
    lateinit var searchView: androidx.appcompat.widget.SearchView
    private var listSongs = mutableListOf<Song>()
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

        val bundle = intent.extras
        val list = bundle?.getSerializable("data") as MutableList<Song>
        listSongs = list

        initViews()
        adapterSong.setconstList(listSongs)
        initControlBottomBar()
        initSerachViewListenner()
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

    private fun initSerachViewListenner() {
        searchView = findViewById(R.id.search_view)

        searchView.setOnCloseListener(object : SearchView.OnCloseListener,
            androidx.appcompat.widget.SearchView.OnCloseListener {
            override fun onClose(): Boolean {
                musicService.setPlayList(listSongs)
                adapterSong.setData(listSongs)
                return false
            }
        })
        searchView.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener,
                androidx.appcompat.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }
                override fun onQueryTextChange(newText: String?): Boolean {
                    newText?.let {
                        val newList = adapterSong.search(it)
                        adapterSong.setData(newList)
                        musicService.setPlayList(newList)
                    }
                    return true
                }
            }
        )
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




    override fun onBackPressed() {
        super.onBackPressed()
        changePausePlayBtn()
        changeContent()
    }
}