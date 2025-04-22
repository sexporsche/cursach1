package com.sexporsche.music_player

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sexporsche.music_player.databinding.PlaylistViewBinding

// Адаптер для RecyclerView, отображающий список плейлистов
class PlaylistViewAdapter(
    private val context: Context, // Контекст приложения
    private var playlistList: ArrayList<Playlist> // Список плейлистов
) : RecyclerView.Adapter<PlaylistViewAdapter.MyHolder>() {

    // ViewHolder для элементов списка
    class MyHolder(binding: PlaylistViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val image = binding.playlistImg // ImageView для обложки плейлиста
        val name = binding.playlistName // TextView для названия плейлиста
        val root = binding.root // Корневой элемент макета
        val delete = binding.playlistDeleteBtn // Кнопка удаления плейлиста
    }

    // Создание нового ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(
            PlaylistViewBinding.inflate(
                LayoutInflater.from(context), // Инфлейт макета элемента списка
                parent,
                false
            )
        )
    }

    // Привязка данных к ViewHolder
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        // Установка названия плейлиста
        holder.name.text = playlistList[position].name
        holder.name.isSelected = true // Включение прокрутки текста, если он длинный

        // Обработчик нажатия на кнопку удаления плейлиста
        holder.delete.setOnClickListener {
            val builder = MaterialAlertDialogBuilder(context)
            builder.setTitle(playlistList[position].name) // Заголовок диалога
                .setMessage("Вы хотите удалить плейлист?") // Сообщение в диалоге
                .setPositiveButton("Да") { dialog, _ ->
                    // Удаление плейлиста из общего списка
                    PlaylistActivity.musicPlaylist.ref.removeAt(position)
                    refreshPlaylist() // Обновление списка плейлистов
                    dialog.dismiss() // Закрытие диалога
                }
                .setNegativeButton("Нет") { dialog, _ ->
                    dialog.dismiss() // Закрытие диалога без действий
                }
            val customDialog = builder.create() // Создание диалога
            customDialog.show() // Показ диалога
        }

        // Обработчик нажатия на элемент списка
        holder.root.setOnClickListener {
            val intent = Intent(context, PlaylistDetails::class.java)
            intent.putExtra("index", position) // Передача индекса плейлиста
            ContextCompat.startActivity(context, intent, null) // Запуск активности деталей плейлиста
        }

        // Загрузка обложки плейлиста (используется обложка первой песни в плейлисте)
        if (PlaylistActivity.musicPlaylist.ref[position].playlist.size > 0) {
            Glide.with(context)
                .load(PlaylistActivity.musicPlaylist.ref[position].playlist[0].artUri) // URI обложки
                .apply(
                    RequestOptions()
                        .placeholder(R.drawable.music_player_icon_splash_screen) // Placeholder на случай ошибки загрузки
                        .centerCrop() // Обрезка изображения по центру
                )
                .into(holder.image) // Установка изображения в ImageView
        }
    }

    // Возвращает количество элементов в списке
    override fun getItemCount(): Int {
        return playlistList.size
    }

    // Обновление списка плейлистов
    @SuppressLint("NotifyDataSetChanged")
    fun refreshPlaylist() {
        playlistList = ArrayList() // Очистка старого списка
        playlistList.addAll(PlaylistActivity.musicPlaylist.ref) // Добавление новых данных
        notifyDataSetChanged() // Уведомление RecyclerView об изменении данных
    }
}