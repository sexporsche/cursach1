package com.sexporsche.music_player

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sexporsche.music_player.databinding.ActivityPlaylistBinding
import com.sexporsche.music_player.databinding.AddPlaylistDialogBinding
import java.text.SimpleDateFormat
import java.util.*

// Активность для управления плейлистами
class PlaylistActivity : AppCompatActivity() {

    // Привязка к макету через ViewBinding
    private lateinit var binding: ActivityPlaylistBinding
    // Адаптер для RecyclerView, отображающий список плейлистов
    private lateinit var adapter: PlaylistViewAdapter

    companion object {
        // Объект, содержащий все плейлисты
        var musicPlaylist: MusicPlaylist = MusicPlaylist()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE)
        val appLanguage = sharedPreferences.getString("App_Language", "")
        if (!appLanguage.isNullOrEmpty()) {
            setLocale(this, appLanguage)
        }
        // Инициализация привязки к макету
        binding = ActivityPlaylistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Настройка RecyclerView для отображения плейлистов
        binding.playlistRV.setHasFixedSize(true) // Оптимизация производительности
        binding.playlistRV.setItemViewCacheSize(13) // Кэширование элементов
        binding.playlistRV.layoutManager = GridLayoutManager(this@PlaylistActivity, 2) // Сетка с 2 столбцами
        adapter = PlaylistViewAdapter(this, playlistList = musicPlaylist.ref) // Создание адаптера
        binding.playlistRV.adapter = adapter // Установка адаптера для RecyclerView

        // Обработчик нажатия на кнопку "Назад"
        binding.backBtnPLA.setOnClickListener { finish() }

        // Обработчик нажатия на кнопку добавления нового плейлиста
        binding.addPlaylistBtn.setOnClickListener { customAlertDialog() }

        // Если есть плейлисты, скрываем текст инструкции
        if (musicPlaylist.ref.isNotEmpty()) binding.instructionPA.visibility = View.GONE
    }

    // Показ диалога для создания нового плейлиста
    private fun customAlertDialog() {
        // Инфлейт макета диалога добавления плейлиста
        val customDialog = LayoutInflater.from(this@PlaylistActivity)
            .inflate(R.layout.add_playlist_dialog, binding.root, false)
        val binder = AddPlaylistDialogBinding.bind(customDialog) // Привязка к макету диалога
        val builder = MaterialAlertDialogBuilder(this) // Создание диалога
        val dialog = builder.setView(customDialog)
            .setTitle("Детали плейлиста") // Заголовок диалога
            .setPositiveButton("ДОБАВИТЬ") { dialog, _ ->
                // Получение введенных данных из полей диалога
                val playlistName = binder.playlistName.text // Название плейлиста
                val createdBy = binder.yourName.text // Имя создателя
                if (playlistName != null && createdBy != null)
                    if (playlistName.isNotEmpty() && createdBy.isNotEmpty()) {
                        addPlaylist(playlistName.toString(), createdBy.toString()) // Добавление плейлиста
                    }
                dialog.dismiss() // Закрытие диалога
            }.create()
        dialog.show() // Показ диалога
    }

    // Метод для добавления нового плейлиста
    private fun addPlaylist(name: String, createdBy: String) {
        var playlistExists = false
        // Проверка, существует ли уже плейлист с таким именем
        for (i in musicPlaylist.ref) {
            if (name == i.name) {
                playlistExists = true
                break
            }
        }
        if (playlistExists) {
            // Если плейлист существует, показываем уведомление
            Toast.makeText(this, "Плейлист уже существует!", Toast.LENGTH_SHORT).show()
        } else {
            // Создание нового плейлиста
            val tempPlaylist = Playlist()
            tempPlaylist.name = name // Установка имени плейлиста
            tempPlaylist.playlist = ArrayList() // Инициализация списка песен плейлиста
            tempPlaylist.createdBy = createdBy // Установка имени создателя
            val calendar = Calendar.getInstance().time // Текущая дата
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH) // Формат даты
            tempPlaylist.createdOn = sdf.format(calendar) // Установка даты создания
            musicPlaylist.ref.add(tempPlaylist) // Добавление плейлиста в список
            adapter.refreshPlaylist() // Обновление адаптера
        }
    }

    // Вызывается при возобновлении активности
    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged() // Обновление списка плейлистов
    }
}