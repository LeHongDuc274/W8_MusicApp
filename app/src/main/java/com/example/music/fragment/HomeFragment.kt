package com.example.music.fragment

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.music.R
import com.example.music.adapter.SongsApdapter
import com.example.music.models.Song
import com.example.music.service.MusicService
import java.util.concurrent.TimeUnit


class HomeFragment : Fragment() {
    lateinit var rv_songs: RecyclerView
    lateinit var adapterSong: SongsApdapter
    private var mediaPlayer = MediaPlayer()
    val listSongs = mutableListOf<Song>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        getAllSongs(requireActivity().applicationContext)
        initeViews(view)
        return view

    }

    private fun initeViews(view: View) {
        rv_songs = view.findViewById(R.id.rv_songs)
        adapterSong = SongsApdapter {
            itemClick(it)
        }
        adapterSong.setData(listSongs)
        rv_songs.adapter = adapterSong
        rv_songs.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
    }

    private fun itemClick(pos: Int) {
        val curSong = listSongs[pos]
        val bundle = bundleOf(
            "song" to curSong
        )
        val intent = Intent(requireActivity(),MusicService::class.java)
        intent.putExtras(bundle)
        requireActivity().startService(intent)
//        mediaPlayer.stop()
//        mediaPlayer.release()
//        mediaPlayer = MediaPlayer.create(requireActivity().applicationContext, curUri)
//        mediaPlayer.start()
//        Log.e("tag", curSong.contentUri.toString())
    }

    private fun getAllSongs(context: Context) {

        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY
                )
            } else {
                MediaStore.Audio.Media.INTERNAL_CONTENT_URI
            }
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(
            TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES).toString()
        )
        val query = context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            null
        )
        query?.let { cursor ->
            var i = 0
            Log.e("size", cursor.columnCount.toString())
            while (cursor.moveToNext()) {
                i++
                Log.e("sizeCol", i.toString())

                val id = cursor.getLong(0)
                val title = cursor.getString(1)
                val singer = cursor.getString(2)
                val duration = cursor.getLong(3)
                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                //Log.e("id",contentUri.toString())
                // extractVideoLocationInfo(contentUri)
                listSongs.add(Song(id, title, singer, duration))
            }
            cursor.close()
        }
    }

}