package com.example.music.fragment

import android.content.*
import android.content.Context.AUDIO_SERVICE
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.Image
import android.os.*
import android.service.quicksettings.Tile
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.isVisible
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
    lateinit var tvProgressChange: TextView
    lateinit var progressBar: SeekBar
    lateinit var volumBar: SeekBar
    lateinit var tvCurDuration: TextView
    lateinit var ivContent: ImageView
    lateinit var btnPause: FloatingActionButton
    lateinit var ivVolum : ImageView
    private var curSong: Song? = null
    private var fromUser = false

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
                    updateSeekBar(value, fromUser)
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
    }

    private fun unregisterReceiver() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(broadcast)
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
        initButton(view)
        return view
    }

    private fun initButton(view: View) {
        btnPause = view.findViewById<FloatingActionButton>(R.id.btn_pause)
        changeTogglePausePlayUi()
        val btnPrev = view.findViewById<ImageView>(R.id.btn_prev)
        val btnNext = view.findViewById<ImageView>(R.id.btn_next)
        val btnBack = view.findViewById<ImageButton>(R.id.btn_back)
        btnBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
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
        progressBar = view.findViewById<SeekBar>(R.id.progress_horizontal)
        volumBar = view.findViewById(R.id.volum)
        btnRepeat = view.findViewById(R.id.btn_repeat)
        btnShuffle = view.findViewById(R.id.btn_shuffle)
        tvProgressChange = view.findViewById(R.id.tv_progress_change)
        tvProgressChange.isVisible = false
        ivContent = view.findViewById(R.id.iv_content)
        ivVolum = view.findViewById(R.id.iv_volum)
        updateUiWhenChangeSong()
        val audioManager = requireActivity().getSystemService(AUDIO_SERVICE) as AudioManager
        volumBar.max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0 ){
            ivVolum.setImageResource(R.drawable.ic_baseline_volume_off_24)
        } else ivVolum.setImageResource(R.drawable.ic_baseline_volume_up_24)
        volumBar.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        changeRepeatState()
        changeShuffleState()
        listenSeekBarChange()
    }

    private fun listenSeekBarChange() {
        val audioManager = requireActivity().getSystemService(AUDIO_SERVICE) as AudioManager

        progressBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            var newPos = 0
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if (fromUser) newPos = p1
                tvProgressChange.text = formatTime(newPos)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {
                fromUser = true
                tvProgressChange.isVisible = true
            }
            override fun onStopTrackingTouch(p0: SeekBar?) {
                musicService.seekTo(newPos)
                fromUser = false
                tvProgressChange.isVisible = false
            }

        })

        volumBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, p1, 0)
                if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0 ){
                    ivVolum.setImageResource(R.drawable.ic_baseline_volume_off_24)
                } else ivVolum.setImageResource(R.drawable.ic_baseline_volume_up_24)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {
            }
            override fun onStopTrackingTouch(p0: SeekBar?) {
            }

        })
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
            tvCurDuration.text = formatTime(musicService.getMediaCurrentPos() / 1000)
            progressBar.max = (it.duration / 1000).toInt()
        }
        val byteArray = musicService.cursong.byteArray
        if (byteArray.isEmpty()) {
            ivContent.setImageResource(R.drawable.ic_baseline_music_note_24)
        } else {
            ivContent.setImageBitmap(BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size))
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

    private fun updateSeekBar(value: Int, fromUser: Boolean) {
        if (!fromUser) {
            formatTime(value)
            tvCurDuration.text = formatTime(value)
            progressBar.progress = value
        }
    }
}