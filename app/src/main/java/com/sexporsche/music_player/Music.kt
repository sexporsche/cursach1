package com.sexporsche.music_player

import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import com.google.android.material.color.MaterialColors
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

// Класс данных для хранения информации о музыкальном треке
data class Music(
    val id: String, // Уникальный идентификатор трека
    val title: String, // Название трека
    val album: String, // Альбом
    val artist: String, // Исполнитель
    val duration: Long = 0, // Длительность трека в миллисекундах
    val path: String, // Путь к файлу трека
    val artUri: String // URI обложки альбома
)

// Класс для представления плейлиста
class Playlist {
    lateinit var name: String // Название плейлиста
    lateinit var playlist: ArrayList<Music> // Список треков в плейлисте
    lateinit var createdBy: String // Создатель плейлиста
    lateinit var createdOn: String // Дата создания плейлиста
}

// Класс для хранения ссылок на все плейлисты
class MusicPlaylist {
    var ref: ArrayList<Playlist> = ArrayList() // Список плейлистов
}

// Функция для форматирования длительности трека в формат "минуты:секунды"
fun formatDuration(duration: Long): String {
    val minutes = TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS) // Конвертация в минуты
    val seconds = (TimeUnit.SECONDS.convert(duration, TimeUnit.MILLISECONDS) -
            minutes * TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES)) // Конвертация в секунды
    return String.format("%02d:%02d", minutes, seconds) // Форматирование строки
}

// Функция для получения обложки альбома из метаданных трека
fun getImgArt(path: String): ByteArray? {
    val retriever = MediaMetadataRetriever() // Инициализация объекта для чтения метаданных
    retriever.setDataSource(path) // Установка пути к файлу
    return retriever.embeddedPicture // Возвращение массива байтов обложки
}

// Функция для изменения позиции текущего трека в списке воспроизведения
fun setSongPosition(increment: Boolean) {
    if (!PlayerActivity.repeat) { // Если повтор не включен
        if (increment) {
            // Переход к следующему треку
            if (PlayerActivity.musicListPA.size - 1 == PlayerActivity.songPosition)
                PlayerActivity.songPosition = 0 // Возврат к началу списка
            else ++PlayerActivity.songPosition
        } else {
            // Переход к предыдущему треку
            if (0 == PlayerActivity.songPosition)
                PlayerActivity.songPosition = PlayerActivity.musicListPA.size - 1 // Переход в конец списка
            else --PlayerActivity.songPosition
        }
    }
}

// Функция для выхода из приложения
fun exitApplication() {
    if (PlayerActivity.musicService != null) {
        // Освобождение ресурсов службы воспроизведения
        PlayerActivity.musicService!!.audioManager.abandonAudioFocus(PlayerActivity.musicService)
        PlayerActivity.musicService!!.stopForeground(true)
        PlayerActivity.musicService!!.mediaPlayer!!.release()
        PlayerActivity.musicService = null
    }
    exitProcess(1) // Завершение процесса приложения
}

// Функция для проверки, является ли трек избранным
fun favouriteChecker(id: String): Int {
    PlayerActivity.isFavourite = false // Сброс флага избранного
    FavouriteActivity.favouriteSongs.forEachIndexed { index, music ->
        if (id == music.id) {
            PlayerActivity.isFavourite = true // Установка флага избранного
            return index // Возвращение индекса трека в списке избранных
        }
    }
    return -1 // Возвращение -1, если трек не найден в избранных
}

// Функция для проверки существования файлов в плейлисте
fun checkPlaylist(playlist: ArrayList<Music>): ArrayList<Music> {
    playlist.forEachIndexed { index, music ->
        val file = File(music.path) // Создание объекта файла по пути
        if (!file.exists())
            playlist.removeAt(index) // Удаление трека из плейлиста, если файл не существует
    }
    return playlist // Возвращение обновленного плейлиста
}

// Функция для настройки внешнего вида кнопок диалогового окна


// Функция для получения основного цвета из изображения
fun getMainColor(img: Bitmap): Int {
    val newImg = Bitmap.createScaledBitmap(img, 1, 1, true) // Масштабирование изображения до 1x1 пикселя
    val color = newImg.getPixel(0, 0) // Получение цвета единственного пикселя
    newImg.recycle() // Освобождение ресурсов изображения
    return color // Возвращение цвета
}