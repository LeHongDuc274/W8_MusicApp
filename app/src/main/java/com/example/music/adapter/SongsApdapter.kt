package com.example.music.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.music.R
import com.example.music.models.Song
import java.sql.Time
import java.util.concurrent.TimeUnit

class SongsApdapter(private val click:(Int)->Unit) : RecyclerView.Adapter<SongsApdapter.ViewHolder>() {
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
    }
}