package com.example.music

import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.provider.MediaStore
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import com.example.music.models.Song
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class SplashActivity : AppCompatActivity() {

    lateinit var progressBar: ProgressBar
    lateinit var tvStatus: TextView
    private var listSongs = mutableListOf<Song>()
    private val REQUEST_CODE = 1
    private val START = 1
    private val FETCHING = 2
    private val SUCCESS = 3
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        initViews()
        runtimePermission()
    }

    private fun initViews() {
        progressBar = findViewById(R.id.progress_fetch)
        tvStatus = findViewById(R.id.tv_status)
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

    val runnable = Runnable {
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
            handler.sendMessage(handler.obtainMessage(START, cursor.count,0))
            while (cursor.moveToNext()) {
                try {
                    Thread.sleep(200)
                } catch (e:InterruptedException){
                    e.printStackTrace()
                }
                val id = cursor.getLong(0)
                val title = cursor.getString(1)
                val singer = cursor.getString(2)
                val duration = cursor.getLong(3)
                val uri =
                    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                retriever.setDataSource(applicationContext, uri)
                val bitmap = retriever.embeddedPicture ?: byteArrayOf()
                val song = Song(id, title, singer, duration, bitmap)
                handler.sendMessage(handler.obtainMessage(FETCHING, song))

            }
            handler.sendMessage(handler.obtainMessage(SUCCESS))
            cursor.close()
        }
    }
    val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                START -> {
                    progressBar.max = msg.arg1
                }
                FETCHING -> {
                    val song = msg.obj as Song
                    listSongs.add(song)
                    progressBar.progress = listSongs.size
                    var status = "Fetch ${listSongs.size} /${progressBar.max}"
                    tvStatus.text = status
                }
                SUCCESS -> {
                    stopThread()
                    gotoHomeActivity()
                }
            }
        }
    }

    var thread = Thread()
    private fun startThread() {
        thread = Thread(runnable)
        handler.post { thread.start() }
    }
    private fun stopThread() {
        handler.removeCallbacksAndMessages(null)
        if (thread.isInterrupted == false) thread.interrupt()
    }

    private fun getAllSongs() {
        startThread()
    }
    private fun gotoHomeActivity(){
        if(listSongs.isNotEmpty()){
            val intent = Intent(this,MainActivity::class.java)
            val list = listSongs.toTypedArray()
            intent.putExtras( bundleOf("data" to listSongs))
            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopThread()
    }
}