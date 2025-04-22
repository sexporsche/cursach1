package com.sexporsche.music_player

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.sexporsche.music_player.databinding.ActivityFavouriteBinding

// Активность для отображения избранных песен
class FavouriteActivity : AppCompatActivity() {

    // Привязка к макету через ViewBinding
    private lateinit var binding: ActivityFavouriteBinding
    // Адаптер для RecyclerView, отображающий список избранных песен
    private lateinit var adapter: FavouriteAdapter

    companion object {
        // Список избранных песен
        var favouriteSongs: ArrayList<Music> = ArrayList()
        // Флаг, указывающий, были ли изменения в списке избранных песен
        var favouritesChanged: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE)
        val appLanguage = sharedPreferences.getString("App_Language", "")
        if (!appLanguage.isNullOrEmpty()) {
            setLocale(this, appLanguage)
        }
        // Инициализация привязки к макету
        binding = ActivityFavouriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Проверка и обновление списка избранных песен
        favouriteSongs = checkPlaylist(favouriteSongs)

        // Обработчик нажатия на кнопку "Назад"
        binding.backBtnFA.setOnClickListener { finish() }

        // Настройка RecyclerView для отображения избранных песен
        binding.favouriteRV.setHasFixedSize(true) // Оптимизация производительности
        binding.favouriteRV.setItemViewCacheSize(13) // Кэширование элементов для плавной прокрутки
        binding.favouriteRV.layoutManager = GridLayoutManager(this, 4) // Используем сетку с 4 столбцами
        adapter = FavouriteAdapter(this, favouriteSongs) // Создание адаптера
        binding.favouriteRV.adapter = adapter // Установка адаптера для RecyclerView

        // Сброс флага изменений при запуске активности
        favouritesChanged = false

        // Если список избранных песен пуст, скрываем кнопку перемешивания
        if (favouriteSongs.size < 1) binding.shuffleBtnFA.visibility = View.INVISIBLE

        // Если список избранных песен не пуст, скрываем текст инструкции
        if (favouriteSongs.isNotEmpty()) binding.instructionFV.visibility = View.GONE

        // Обработчик нажатия на кнопку "Перемешать"
        binding.shuffleBtnFA.setOnClickListener {
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra("index", 0) // Передаем индекс первой песни
            intent.putExtra("class", "FavouriteShuffle") // Указываем режим воспроизведения
            startActivity(intent) // Запуск активности плеера
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        // Если произошли изменения в списке избранных песен, обновляем адаптер
        if (favouritesChanged) {
            adapter.updateFavourites(favouriteSongs)
            favouritesChanged = false // Сбрасываем флаг изменений
        }
    }
}