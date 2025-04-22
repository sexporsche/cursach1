package com.sexporsche.music_player

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

// Класс для обработки действий из уведомления (например, воспроизведение, пауза, переход к следующей/предыдущей песне)
class NotificationReceiver : BroadcastReceiver() {

    // Метод вызывается при получении широковещательного сообщения
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            // Действие: предыдущая песня
            ApplicationClass.PREVIOUS -> if (PlayerActivity.musicListPA.size > 1) prevNextSong(
                increment = false,
                context = context!!
            )

            // Действие: воспроизведение/пауза
            ApplicationClass.PLAY -> if (PlayerActivity.isPlaying) pauseMusic() else playMusic()

            // Действие: следующая песня
            ApplicationClass.NEXT -> if (PlayerActivity.musicListPA.size > 1) prevNextSong(
                increment = true,
                context = context!!
            )

            // Действие: выход из приложения
            ApplicationClass.EXIT -> {
                exitApplication()
            }
        }
    }

    // Метод для воспроизведения музыки
    private fun playMusic() {
        PlayerActivity.isPlaying = true // Установка флага воспроизведения
        PlayerActivity.musicService!!.mediaPlayer!!.start() // Запуск плеера
        PlayerActivity.musicService!!.showNotification(R.drawable.pause_icon) // Обновление уведомления
        PlayerActivity.binding.playPauseBtnPA.setIconResource(R.drawable.pause_icon) // Изменение иконки кнопки

        // Обработка возможных ошибок при обновлении интерфейса NowPlaying
        try {
            NowPlaying.binding.playPauseBtnNP.setIconResource(R.drawable.pause_icon)
        } catch (_: Exception) {
        }
    }

    // Метод для паузы музыки
    private fun pauseMusic() {
        PlayerActivity.isPlaying = false // Сброс флага воспроизведения
        PlayerActivity.musicService!!.mediaPlayer!!.pause() // Пауза плеера
        PlayerActivity.musicService!!.showNotification(R.drawable.play_icon) // Обновление уведомления
        PlayerActivity.binding.playPauseBtnPA.setIconResource(R.drawable.play_icon) // Изменение иконки кнопки

        // Обработка возможных ошибок при обновлении интерфейса NowPlaying
        try {
            NowPlaying.binding.playPauseBtnNP.setIconResource(R.drawable.play_icon)
        } catch (_: Exception) {
        }
    }

    // Метод для перехода к следующей или предыдущей песне
    @SuppressLint("CheckResult")
    private fun prevNextSong(increment: Boolean, context: Context) {
        setSongPosition(increment = increment) // Изменение позиции текущей песни
        PlayerActivity.musicService!!.createMediaPlayer() // Создание нового MediaPlayer для новой песни

        // Загрузка обложки альбома для PlayerActivity
        Glide.with(context)
            .load(PlayerActivity.musicListPA[PlayerActivity.songPosition].artUri)
            .apply(
                RequestOptions().placeholder(R.drawable.music_player_icon_splash_screen)
                    .centerCrop()
            )
            .into(PlayerActivity.binding.songImgPA)

        // Обновление названия песни в PlayerActivity
        PlayerActivity.binding.songNamePA.text =
            PlayerActivity.musicListPA[PlayerActivity.songPosition].title

        // Загрузка обложки альбома для NowPlaying
        Glide.with(context)
            .load(PlayerActivity.musicListPA[PlayerActivity.songPosition].artUri)
            .apply(
                RequestOptions().placeholder(R.drawable.music_player_icon_splash_screen)
                    .centerCrop()
            )
            .into(NowPlaying.binding.songImgNP)

        // Обновление названия песни в NowPlaying
        NowPlaying.binding.songNameNP.text =
            PlayerActivity.musicListPA[PlayerActivity.songPosition].title

        playMusic() // Воспроизведение новой песни

        // Проверка, является ли песня избранной
        PlayerActivity.fIndex =
            favouriteChecker(PlayerActivity.musicListPA[PlayerActivity.songPosition].id)
        if (PlayerActivity.isFavourite) {
            PlayerActivity.binding.favouriteBtnPA.setImageResource(R.drawable.favourite_icon)
        } else {
            PlayerActivity.binding.favouriteBtnPA.setImageResource(R.drawable.favourite_empty_icon)
        }
    }
}