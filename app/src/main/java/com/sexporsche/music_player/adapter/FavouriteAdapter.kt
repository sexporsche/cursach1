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
import com.sexporsche.music_player.databinding.FavouriteViewBinding

// Адаптер для RecyclerView, отображающий список избранных песен
class FavouriteAdapter(
    private val context: Context, // Контекст приложения
    private var musicList: ArrayList<Music>, // Список избранных песен
    val playNext: Boolean = false // Флаг для определения поведения (не используется в текущей реализации)
) : RecyclerView.Adapter<FavouriteAdapter.MyHolder>() {

    // ViewHolder для элементов списка
    class MyHolder(binding: FavouriteViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val image = binding.songImgFV // ImageView для обложки песни
        val name = binding.songNameFV // TextView для названия песни
        val root = binding.root // Корневой элемент макета
    }

    // Создание нового ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(
            FavouriteViewBinding.inflate(
                LayoutInflater.from(context), // Инфлейт макета элемента списка
                parent,
                false
            )
        )
    }

    // Привязка данных к ViewHolder
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        // Установка названия песни
        holder.name.text = musicList[position].title

        // Загрузка обложки песни с использованием Glide
        Glide.with(context)
            .load(musicList[position].artUri) // URI обложки
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.music_player_icon_splash_screen) // Placeholder на случай ошибки загрузки
                    .centerCrop() // Обрезка изображения по центру
            )
            .into(holder.image) // Установка изображения в ImageView

        // Обработчик нажатия на элемент списка
        holder.root.setOnClickListener {
            val intent = Intent(context, PlayerActivity::class.java)
            intent.putExtra("index", position) // Передача индекса песни
            intent.putExtra("class", "FavouriteAdapter") // Указание источника
            ContextCompat.startActivity(context, intent, null) // Запуск активности плеера
        }
    }

    // Возвращает количество элементов в списке
    override fun getItemCount(): Int {
        return musicList.size
    }

    // Обновление списка избранных песен
    @SuppressLint("NotifyDataSetChanged")
    fun updateFavourites(newList: ArrayList<Music>) {
        musicList = ArrayList() // Очистка старого списка
        musicList.addAll(newList) // Добавление новых данных
        notifyDataSetChanged() // Уведомление RecyclerView об изменении данных
    }
}