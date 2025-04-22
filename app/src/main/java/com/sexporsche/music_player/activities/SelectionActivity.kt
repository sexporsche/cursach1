package com.sexporsche.music_player

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.sexporsche.music_player.databinding.ActivitySelectionBinding

// Активность для выбора музыки из списка
class SelectionActivity : AppCompatActivity() {

    // Привязка к макету через ViewBinding
    private lateinit var binding: ActivitySelectionBinding
    // Адаптер для RecyclerView, отображающий список музыки
    private lateinit var adapter: MusicAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE)
        val appLanguage = sharedPreferences.getString("App_Language", "")
        if (!appLanguage.isNullOrEmpty()) {
            setLocale(this, appLanguage)
        }
        // Инициализация привязки к макету
        binding = ActivitySelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Настройка RecyclerView для отображения списка музыки
        binding.selectionRV.setItemViewCacheSize(30) // Кэширование элементов для плавной прокрутки
        binding.selectionRV.setHasFixedSize(true) // Оптимизация производительности
        binding.selectionRV.layoutManager = LinearLayoutManager(this) // Линейный менеджер компоновки
        adapter = MusicAdapter(
            this,
            MainActivity.MusicListMA,
            selectionActivity = true
        ) // Создание адаптера с флагом выбора
        binding.selectionRV.adapter = adapter // Установка адаптера для RecyclerView

        // Обработчик нажатия на кнопку "Назад"
        binding.backBtnSA.setOnClickListener { finish() }

        // Настройка функционала поиска
        binding.searchViewSA.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            // Вызывается при отправке текста в поиске
            override fun onQueryTextSubmit(query: String?): Boolean = true

            // Вызывается при изменении текста в поле поиска
            override fun onQueryTextChange(newText: String?): Boolean {
                MainActivity.musicListSearch = ArrayList() // Очистка списка результатов поиска
                if (newText != null) {
                    val userInput = newText.lowercase() // Преобразование ввода пользователя в нижний регистр
                    for (song in MainActivity.MusicListMA) {
                        // Проверка, содержит ли название песни введенный текст
                        if (song.title.lowercase().contains(userInput)) {
                            MainActivity.musicListSearch.add(song) // Добавление совпадающих песен
                        }
                    }
                    MainActivity.search = true // Установка флага поиска
                    adapter.updateMusicList(searchList = MainActivity.musicListSearch) // Обновление списка песен
                }
                return true
            }
        })
    }
}