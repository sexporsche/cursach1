package com.sexporsche.music_player

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sexporsche.music_player.databinding.MoreFeaturesBinding
import com.sexporsche.music_player.databinding.MusicViewBinding

// Адаптер для RecyclerView, отображающий список музыки
class MusicAdapter(
    private val context: Context, // Контекст приложения
    private var musicList: ArrayList<Music>, // Список песен
    private val playlistDetails: Boolean = false, // Флаг для режима плейлиста
    private val selectionActivity: Boolean = false // Флаг для режима выбора
) : RecyclerView.Adapter<MusicAdapter.MyHolder>() {

    // ViewHolder для элементов списка
    class MyHolder(binding: MusicViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val title = binding.songNameMV // Название песни
        val album = binding.songAlbumMV // Альбом
        val image = binding.imageMV // Обложка песни
        val duration = binding.songDuration // Длительность песни
        val root = binding.root // Корневой элемент макета
    }

    // Создание нового ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(
            MusicViewBinding.inflate(
                LayoutInflater.from(context), // Инфлейт макета элемента списка
                parent,
                false
            )
        )
    }

    // Привязка данных к ViewHolder
    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        // Установка названия песни
        holder.title.text = musicList[position].title
        // Установка названия альбома
        holder.album.text = musicList[position].album
        // Форматирование и установка длительности песни
        holder.duration.text = formatDuration(musicList[position].duration)

        // Загрузка обложки песни с использованием Glide
        Glide.with(context)
            .load(musicList[position].artUri) // URI обложки
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.music_player_icon_splash_screen) // Placeholder на случай ошибки загрузки
                    .centerCrop() // Обрезка изображения по центру
            )
            .into(holder.image) // Установка изображения в ImageView

        // Обработчик долгого нажатия для отображения дополнительных функций
        if (!selectionActivity) {
            holder.root.setOnLongClickListener {
                // Инфлейт макета дополнительных функций
                val customDialog = LayoutInflater.from(context).inflate(R.layout.more_features, holder.root, false)
                val bindingMF = MoreFeaturesBinding.bind(customDialog)
                // Создание диалога с дополнительными функциями
                val dialog = MaterialAlertDialogBuilder(context)
                    .setView(customDialog)
                    .create()
                dialog.show()
                // Установка полупрозрачного фона для диалога
                dialog.window?.setBackgroundDrawable(ColorDrawable(0x99000000.toInt()))
                true
            }
        }

        // Обработка кликов в зависимости от режима
        when {
            playlistDetails -> {
                // Режим плейлиста: отправка данных о выбранной песне
                holder.root.setOnClickListener {
                    sendIntent(ref = "PlaylistDetailsAdapter", pos = position)
                }
            }
            selectionActivity -> {
                // Режим выбора: добавление или удаление песни из плейлиста
                holder.root.setOnClickListener {
                    if (addSong(musicList[position])) {
                        // Если песня добавлена, изменяем фон элемента
                        holder.root.setBackgroundColor(ContextCompat.getColor(context, R.color.cool_pink))
                    } else {
                        // Если песня удалена, возвращаем фон к исходному
                        holder.root.setBackgroundColor(ContextCompat.getColor(context, R.color.white))
                    }
                }
            }
            else -> {
                // Обычный режим: отправка данных о выбранной песне
                holder.root.setOnClickListener {
                    when {
                        MainActivity.search -> sendIntent(ref = "MusicAdapterSearch", pos = position) // Режим поиска
                        musicList[position].id == PlayerActivity.nowPlayingId ->
                            sendIntent(ref = "NowPlaying", pos = PlayerActivity.songPosition) // Текущая воспроизводимая песня
                        else -> sendIntent(ref = "MusicAdapter", pos = position) // Обычный режим
                    }
                }
            }
        }
    }

    // Возвращает количество элементов в списке
    override fun getItemCount(): Int {
        return musicList.size
    }

    // Обновление списка песен (например, при поиске)
    @SuppressLint("NotifyDataSetChanged")
    fun updateMusicList(searchList: ArrayList<Music>) {
        musicList = ArrayList() // Очистка старого списка
        musicList.addAll(searchList) // Добавление новых данных
        notifyDataSetChanged() // Уведомление RecyclerView об изменении данных
    }

    // Отправка данных о выбранной песне в PlayerActivity
    private fun sendIntent(ref: String, pos: Int) {
        val intent = Intent(context, PlayerActivity::class.java)
        intent.putExtra("index", pos) // Передача индекса песни
        intent.putExtra("class", ref) // Указание источника
        ContextCompat.startActivity(context, intent, null) // Запуск активности плеера
    }

    // Добавление или удаление песни из текущего плейлиста
    private fun addSong(song: Music): Boolean {
        // Проверка, существует ли песня уже в плейлисте
        PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist.forEachIndexed { index, music ->
            if (song.id == music.id) {
                // Если песня найдена, удаляем её
                PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist.removeAt(index)
                return false
            }
        }
        // Если песня не найдена, добавляем её
        PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist.add(song)
        return true
    }

    // Обновление списка песен для текущего плейлиста
    @SuppressLint("NotifyDataSetChanged")
    fun refreshPlaylist() {
        musicList = ArrayList() // Очистка старого списка
        musicList =
            PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist // Получение нового списка
        notifyDataSetChanged() // Уведомление RecyclerView об изменении данных
    }
}