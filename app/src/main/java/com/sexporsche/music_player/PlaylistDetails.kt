package com.sexporsche.music_player

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.GsonBuilder
import com.sexporsche.music_player.databinding.ActivityPlaylistDetailsBinding

// Активность для отображения деталей плейлиста
class PlaylistDetails : AppCompatActivity() {

    private lateinit var binding: ActivityPlaylistDetailsBinding // Привязка к макету
    private lateinit var adapter: MusicAdapter // Адаптер для RecyclerView

    companion object {
        var currentPlaylistPos: Int = -1 // Текущая позиция плейлиста в списке
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaylistDetailsBinding.inflate(layoutInflater) // Инициализация привязки к макету
        setContentView(binding.root)

        // Получение индекса текущего плейлиста из переданных данных
        currentPlaylistPos = intent.extras?.get("index") as Int

        try {
            // Проверка и обновление списка песен в плейлисте (удаление недоступных файлов)
            PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].playlist =
                checkPlaylist(playlist = PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].playlist)
        } catch (e: Exception) {
            // Обработка возможных ошибок
        }

        // Настройка RecyclerView для отображения списка песен в плейлисте
        binding.playlistDetailsRV.setItemViewCacheSize(10) // Кэширование элементов
        binding.playlistDetailsRV.setHasFixedSize(true) // Оптимизация производительности
        binding.playlistDetailsRV.layoutManager = LinearLayoutManager(this) // Линейный менеджер компоновки
        adapter = MusicAdapter(
            this,
            PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].playlist,
            playlistDetails = true // Флаг для режима плейлиста
        )
        binding.playlistDetailsRV.adapter = adapter // Установка адаптера для RecyclerView

        // Обработчик нажатия на кнопку "Назад"
        binding.backBtnPD.setOnClickListener { finish() }

        // Обработчик нажатия на кнопку "Перемешать"
        binding.shuffleBtnPD.setOnClickListener {
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra("index", 0) // Передача индекса первой песни
            intent.putExtra("class", "PlaylistDetailsShuffle") // Указание источника
            startActivity(intent) // Запуск активности плеера
        }

        // Обработчик нажатия на кнопку "Добавить"
        binding.addBtnPD.setOnClickListener {
            startActivity(Intent(this, SelectionActivity::class.java)) // Переход в активность выбора песен
        }

        // Обработчик нажатия на кнопку "Удалить все"
        binding.removeAllPD.setOnClickListener {
            val builder = MaterialAlertDialogBuilder(this)
            builder.setTitle("Удалить") // Заголовок диалога
                .setMessage("Вы хотите удалить все песни из плейлиста?") // Сообщение в диалоге
                .setPositiveButton("Да") { dialog, _ ->
                    // Очистка списка песен в плейлисте
                    PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].playlist.clear()
                    adapter.refreshPlaylist() // Обновление адаптера
                    dialog.dismiss() // Закрытие диалога
                }
                .setNegativeButton("Нет") { dialog, _ ->
                    dialog.dismiss() // Закрытие диалога без действий
                }
            val customDialog = builder.create() // Создание диалога
            customDialog.show() // Показ диалога
        }
    }

    // Вызывается при возобновлении активности
    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()

        // Установка имени плейлиста
        binding.playlistNamePD.text = PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].name

        // Установка дополнительной информации о плейлисте
        binding.moreInfoPD.text = "Всего ${adapter.itemCount} песен.\n\n" +
                "Создано:\n${PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].createdOn}\n\n" +
                "  -- ${PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].createdBy}"

        // Если в плейлисте есть песни, отображаем обложку первой песни
        if (adapter.itemCount > 0) {
            Glide.with(this)
                .load(PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].playlist[0].artUri) // URI обложки
                .apply(
                    RequestOptions().placeholder(R.drawable.music_player_icon_splash_screen) // Placeholder
                        .centerCrop() // Обрезка изображения по центру
                )
                .into(binding.playlistImgPD) // Установка изображения в ImageView
            binding.shuffleBtnPD.visibility = View.VISIBLE // Показ кнопки "Перемешать"
        }

        adapter.notifyDataSetChanged() // Обновление списка песен

        // Сохранение данных плейлиста в SharedPreferences
        val editor = getSharedPreferences("FAVOURITES", MODE_PRIVATE).edit()
        val jsonStringPlaylist = GsonBuilder().create().toJson(PlaylistActivity.musicPlaylist)
        editor.putString("MusicPlaylist", jsonStringPlaylist)
        editor.apply()
    }
}