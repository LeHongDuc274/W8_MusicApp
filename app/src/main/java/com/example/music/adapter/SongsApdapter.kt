package com.example.music.adapter

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.music.R
import com.example.music.models.Song
import java.sql.Time
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.Comparator

class SongsApdapter(private val click: (Int) -> Unit) :
    RecyclerView.Adapter<SongsApdapter.ViewHolder>() {
    private var listSongs = mutableListOf<Song>(
    )

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @SuppressLint("SetTextI18n")
        fun onBind(song: Song) {
            itemView.findViewById<TextView>(R.id.tv_title).text = song.title
            itemView.findViewById<TextView>(R.id.tv_singer).text = song.singer ?: ""
            val minute = song.duration / 1000 / 60
            val seconds = song.duration / 1000 % 60
            itemView.findViewById<TextView>(R.id.tv_duration).text = "$minute : $seconds"

            val img = itemView.findViewById<ImageView>(R.id.img_song)
            if (song.byteArray.isEmpty()) {
                img.setImageResource(R.drawable.ic_baseline_music_note_24)
            } else {
                img.setImageBitmap(
                    BitmapFactory.decodeByteArray(song.byteArray, 0, song.byteArray.size)
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_song, parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.onBind(listSongs[position])
        holder.itemView.setOnClickListener {
            click.invoke(position)
        }
    }


    override fun getItemCount(): Int {
        return listSongs.size
    }

    fun setData(list: MutableList<Song>) {
        listSongs = list
        notifyDataSetChanged()
    }

    private var constList = mutableListOf<Song>()
    fun setconstList(list: MutableList<Song>) {
        constList = list
    }

    fun search(text: String): MutableList<Song> {

        val searchList = mutableListOf<Song>()
        constList.forEach {
            if ((it.singer + it.title).contains(text, ignoreCase = true)) {
                searchList.add(it)
            }
        }
        setData(searchList)
        return searchList
    }

    fun sortbyDuration(): MutableList<Song> {
        listSongs.sortWith(object : Comparator<Song> {
            override fun compare(p0: Song, p1: Song): Int {
                if (p0.duration > p1.duration) return 1
                else return -1
            }
        })
        notifyDataSetChanged()
        return listSongs
    }

    fun sorbyName(): MutableList<Song> {
        listSongs.sortWith(object : Comparator<Song> {
            override fun compare(p0: Song, p1: Song): Int {
                if (p0.title.get(0) > p1.title.get(0)) return 1
                else return -1
            }
        })
        notifyDataSetChanged()
        return listSongs
    }
}