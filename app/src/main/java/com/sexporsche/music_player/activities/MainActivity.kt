package com.sexporsche.music_player

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.sexporsche.music_player.databinding.ActivityMainBinding
import java.io.File
import java.util.Locale

// Главная активность приложения для отображения списка музыки
class MainActivity : AppCompatActivity() {

    // Привязка к макету через ViewBinding
    private lateinit var binding: ActivityMainBinding
    // Объект для управления боковой панелью навигации
    private lateinit var toggle: ActionBarDrawerToggle
    // Адаптер для RecyclerView, отображающий список музыки
    private var musicAdapter: MusicAdapter? = null

    companion object {
        // Список всех доступных песен
        lateinit var MusicListMA: ArrayList<Music>
        // Список песен для поиска
        lateinit var musicListSearch: ArrayList<Music>
        // Флаг, указывающий, активен ли режим поиска
        var search: Boolean = false
    }

    // Флаг, указывающий, предоставлены ли необходимые разрешения
    private var permissionGranted: Boolean = false


    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE)
        val appLanguage = sharedPreferences.getString("App_Language", "")
        if (!appLanguage.isNullOrEmpty()) {
            setLocale(this, appLanguage)
        }

        setTheme(R.style.MyMusicThemeNav) // Установка темы приложения
        binding = ActivityMainBinding.inflate(layoutInflater) // Инициализация привязки к макету
        setContentView(binding.root) // Установка корневого макета

        // Настройка боковой панели навигации
        toggle = ActionBarDrawerToggle(this, binding.root, R.string.open, R.string.close)
        binding.root.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Запрос разрешений и инициализация интерфейса
        if (requestRuntimePermission()) {
            initializeLayout()
            restoreFavouritesAndPlaylists()
        }

        // Обработчики нажатий на элементы меню боковой панели
        binding.navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.shuffleBtn -> { // Кнопка "Перемешать"
                    val intent = Intent(this@MainActivity, PlayerActivity::class.java)
                    intent.putExtra("index", 0) // Передаем индекс первой песни
                    intent.putExtra("class", "MainActivity") // Указываем источник
                    startActivity(intent) // Запуск плеера
                }
                R.id.favouriteBtn -> startActivity(Intent(this@MainActivity, FavouriteActivity::class.java)) // Переход в избранное
                R.id.playlistBtn -> startActivity(Intent(this@MainActivity, PlaylistActivity::class.java)) // Переход в плейлисты
                R.id.changeLanguageBtn -> showLanguageDialog() // Добавляем вызов диалога для смены языка
                R.id.navExit -> showExitDialog() // Показ диалога выхода
            }
            true
        }

        // Обновление списка музыки при свайпе вниз
        binding.musicSwipeRefresh.setOnRefreshListener {
            MusicListMA = getAllAudio() // Получение обновленного списка музыки
            musicAdapter?.updateMusicList(MusicListMA) // Обновление адаптера
            binding.musicSwipeRefresh.isRefreshing = false // Остановка анимации обновления
        }
    }

    // Восстановление избранных песен и плейлистов из SharedPreferences
    private fun restoreFavouritesAndPlaylists() {
        FavouriteActivity.favouriteSongs = ArrayList()
        val editor = getSharedPreferences("FAVOURITES", MODE_PRIVATE)
        val jsonString = editor.getString("FavouriteSongs", null)
        val typeToken = object : TypeToken<ArrayList<Music>>() {}.type
        if (jsonString != null) {
            val data: ArrayList<Music> = GsonBuilder().create().fromJson(jsonString, typeToken)
            FavouriteActivity.favouriteSongs.addAll(data)
        }
        PlaylistActivity.musicPlaylist = MusicPlaylist()
        val jsonStringPlaylist = editor.getString("MusicPlaylist", null)
        if (jsonStringPlaylist != null) {
            val dataPlaylist: MusicPlaylist =
                GsonBuilder().create().fromJson(jsonStringPlaylist, MusicPlaylist::class.java)
            PlaylistActivity.musicPlaylist = dataPlaylist
        }
    }

    // Запрос необходимых разрешений во время выполнения
    private fun requestRuntimePermission(): Boolean {
        if (permissionGranted) return true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_MEDIA_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_MEDIA_AUDIO),
                    13
                )
                return false
            }
        } else {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    13
                )
                return false
            }
        }
        permissionGranted = true
        return true
    }

    // Обработка результатов запроса разрешений
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 13) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permissionGranted = true
                initializeLayout() // Инициализация интерфейса после предоставления разрешений
            } else {
                Toast.makeText(this, "Разрешение отклонено", Toast.LENGTH_SHORT).show()
                openSettingsForPermissions() // Открытие настроек для предоставления разрешений
            }
        }
    }

    // Открытие настроек приложения для предоставления разрешений
    private fun openSettingsForPermissions() {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", packageName, null)
        startActivity(intent)
    }

    // Инициализация интерфейса и настройка RecyclerView
    @SuppressLint("SetTextI18n")
    private fun initializeLayout() {
        search = false
        MusicListMA = getAllAudio() // Получение списка всех доступных песен
        binding.musicRV.setHasFixedSize(true) // Оптимизация производительности
        binding.musicRV.setItemViewCacheSize(13) // Кэширование элементов
        binding.musicRV.layoutManager = LinearLayoutManager(this@MainActivity) // Линейный менеджер компоновки
        musicAdapter = MusicAdapter(this@MainActivity, MusicListMA) // Создание адаптера
        binding.musicRV.adapter = musicAdapter // Установка адаптера для RecyclerView
    }

    // Получение всех доступных аудиофайлов из медиабиблиотеки устройства
    @SuppressLint("Recycle", "Range")
    private fun getAllAudio(): ArrayList<Music> {
        val tempList = ArrayList<Music>()
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID
        )
        val cursor = this.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null,
            MediaStore.Audio.Media.DATE_ADDED + " DESC", null
        )
        if (cursor != null) {
            if (cursor.moveToFirst())
                do {
                    val titleC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)) ?: "Unknown"
                    val idC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID)) ?: "Unknown"
                    val albumC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)) ?: "Unknown"
                    val artistC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)) ?: "Unknown"
                    val pathC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))
                    val durationC = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))
                    val albumIdC = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)).toString()
                    val uri = Uri.parse("content://media/external/audio/albumart")
                    val artUriC = Uri.withAppendedPath(uri, albumIdC).toString()
                    val music = Music(
                        id = idC,
                        title = titleC,
                        album = albumC,
                        artist = artistC,
                        path = pathC,
                        duration = durationC,
                        artUri = artUriC
                    )
                    val file = File(music.path)
                    if (file.exists()) tempList.add(music) // Добавляем только существующие файлы
                } while (cursor.moveToNext())
            cursor.close()
        }
        return tempList
    }

    // Вызывается при уничтожении активности
    override fun onDestroy() {
        super.onDestroy()
        if (!PlayerActivity.isPlaying && PlayerActivity.musicService != null) {
            exitApplication() // Выход из приложения
        }
    }

    // Вызывается при возобновлении активности
    override fun onResume() {
        super.onResume()
        if (musicAdapter != null) {
            MusicListMA = getAllAudio() // Обновление списка музыки
            musicAdapter?.updateMusicList(MusicListMA) // Обновление адаптера
        }
        if (PlayerActivity.musicService != null) binding.nowPlaying.visibility = View.VISIBLE // Показ текущего воспроизведения
    }

    // Создание меню с функциями поиска
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val searchItem = menu?.findItem(R.id.searchView)
        val searchView = searchItem?.actionView as? SearchView
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) {
                    musicListSearch = MusicListMA.filter {
                        it.title.contains(newText, true) // Поиск по названию песни
                    } as ArrayList<Music>
                    search = true
                    musicAdapter?.updateMusicList(musicListSearch) // Обновление списка песен
                }
                return true
            }
        })
        return super.onCreateOptionsMenu(menu)
    }

    // Обработка выбора пунктов меню
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) return true
        when (item.itemId) {
            // Здесь можно добавить обработку других пунктов меню
        }
        return super.onOptionsItemSelected(item)
    }

    // Показ диалога выхода из приложения
    private fun showExitDialog() {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle("Выход")
            .setMessage("Вы уверены, что хотите выйти?")
            .setPositiveButton("Да") { _, _ -> exitApplication() } // Подтверждение выхода
            .setNegativeButton("Нет") { dialog, _ -> dialog.dismiss() } // Отмена
            .show()
    }

    // Выход из приложения
    private fun exitApplication() {
        PlayerActivity.musicService?.stopSelf() // Остановка службы плеера
        PlayerActivity.musicService = null
        finish() // Завершение активности
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("English", "Русский") // Список доступных языков
        var selectedLanguage = 0 // По умолчанию выбран английский язык

        MaterialAlertDialogBuilder(this)
            .setTitle("Select Language")
            .setSingleChoiceItems(languages, selectedLanguage) { dialog, which ->
                selectedLanguage = which
            }
            .setPositiveButton("OK") { dialog, _ ->
                when (selectedLanguage) {
                    0 -> setLocale(this, "en") // Английский язык
                    1 -> setLocale(this, "ru") // Русский язык
                }
                recreate() // Пересоздаем активность для применения изменений
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}