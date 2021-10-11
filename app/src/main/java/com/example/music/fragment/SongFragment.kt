package com.example.music.fragment

import android.content.*
import android.os.*
import android.service.quicksettings.Tile
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.music.R
import com.example.music.models.Song
import com.example.music.service.MusicService
import com.google.android.material.floatingactionbutton.FloatingActionButton


class SongFragment(val musicService: MusicService) : Fragment() {

    lateinit var tvSinger: TextView
    lateinit var tvTitle: TextView
    lateinit var tvDuration: TextView
    lateinit var btnRepeat: ImageButton
    lateinit var btnShuffle: ImageButton
    private val listSongs = mutableListOf<Song>()
    lateinit var progressBar: ProgressBar
    lateinit var tvCurDuration: TextView
    lateinit var btnPause: FloatingActionButton
    private var curSong: Song? = null
    private var isBound = false


    val broadcast = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            p1?.let {
                if (it.action == "fromNotifyToActivity") {
                    val value = it.getIntExtra("fromNotifyToActivity", -1)
                    handlerReceiver(value)
                }
                if (it.action == "updatePosition") {
                    val value = it.getIntExtra("value", 0)
                    Log.e("value", value.toString())
                    updateSeekBar(value)
                }
            }
        }
    }
    val broadcast2 = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            p1?.let {

                if (it.action == "updatePosition") {
                    val value = it.getIntExtra("value", 0)
                    Log.e("value", value.toString())
                    updateSeekBar(value)
                }
            }
        }
    }

    private fun handlerReceiver(value: Int) {
        when (value) {
            MusicService.ACTION_CHANGE_SONG -> {
                changeCurSong(musicService.cursong)
                updateUiWhenChangeSong()
                changeTogglePausePlayUi()
            }
            MusicService.ACTION_PAUSE -> changeTogglePausePlayUi()
            else -> Unit
        }

    }

    private fun registerReceiver() {
            val filter = IntentFilter("fromNotifyToActivity")
            filter.addAction("updatePosition")
        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            broadcast,
            filter
        )
//        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
//            broadcast2,
//            IntentFilter("updatePosition")
//        )
    }

    private fun unregisterReceiver() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(broadcast)
       // LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(broadcast2)
    }


    override fun onStart() {
        super.onStart()
        registerReceiver()
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_song, container, false)
        curSong = musicService.getCurSong()
        initView(view)
        // startThread()
        initControls(view)
        return view
    }

    private fun initControls(view: View) {
        btnPause = view.findViewById<FloatingActionButton>(R.id.btn_pause)
        changeTogglePausePlayUi()
        val btnPrev = view.findViewById<ImageView>(R.id.btn_prev)
        val btnNext = view.findViewById<ImageView>(R.id.btn_next)

        btnPause.setOnClickListener {
            musicService.togglePlayPause()
            changeTogglePausePlayUi()
        }
        btnNext.setOnClickListener {
            musicService.nextSong()
            changeCurSong(musicService.getCurSong())
            updateUiWhenChangeSong()
            changeTogglePausePlayUi()
        }
        btnPrev.setOnClickListener {
            musicService.prevSong()
            changeCurSong(musicService.getCurSong())
            updateUiWhenChangeSong()
            changeTogglePausePlayUi()
        }
        btnRepeat.setOnClickListener {
            musicService.setRepeat()
            changeRepeatState()
        }
        btnShuffle.setOnClickListener {
            musicService.setShuffle()
            changeShuffleState()
        }
    }

    private fun initView(view: View) {
        tvTitle = view.findViewById<TextView>(R.id.tv_title)
        tvSinger = view.findViewById<TextView>(R.id.tv_singer)
        tvCurDuration = view.findViewById<TextView>(R.id.tv_current_duration)
        tvDuration = view.findViewById<TextView>(R.id.tv_duration)
        progressBar = view.findViewById<ProgressBar>(R.id.progress_horizontal)
        btnRepeat = view.findViewById(R.id.btn_repeat)
        btnShuffle = view.findViewById(R.id.btn_shuffle)
        updateUiWhenChangeSong()
        changeRepeatState()
        changeShuffleState()
    }

    private fun formatTime(time: Int): String {
        val minute = time / 60
        val seconds = time % 60
        return "$minute:$seconds"
    }

    private fun changeCurSong(newSong: Song) {
        curSong = newSong
    }

    private fun updateUiWhenChangeSong() {
        //change curSong
        // content
        curSong?.let {
            tvTitle.text = it.title
            tvSinger.text = it.singer
            tvDuration.text = formatTime((it.duration / 1000).toInt())
            tvCurDuration.text = formatTime(musicService.getMediaCurrentPos()/1000)
            progressBar.max = (it.duration / 1000).toInt()
        }
        // togglePausePlay
    }

    private fun changeTogglePausePlayUi() {
        if (!musicService.isPlaying()) {
            btnPause.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        } else {
            btnPause.setImageResource(R.drawable.ic_baseline_pause_24)
        }
    }

    private fun changeRepeatState() {
        if (musicService.repeat) {
            btnRepeat.setImageResource(R.drawable.ic_repeat_on)
        } else btnRepeat.setImageResource(R.drawable.ic_baseline_repeat_24)
    }

    private fun changeShuffleState() {
        if (musicService.shuffle) {
            btnShuffle.setImageResource(R.drawable.ic_shuffle_on)
        } else btnShuffle.setImageResource(R.drawable.ic_baseline_shuffle_24)
    }

    private fun updateSeekBar(value: Int) {
        formatTime(value)
        tvCurDuration.text = formatTime(value)
        progressBar.progress = value
    }
}