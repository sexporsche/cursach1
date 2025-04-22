package com.sexporsche.music_player

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.*
import android.media.audiofx.LoudnessEnhancer
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat

// Сервис для воспроизведения музыки
class MusicService : Service(), AudioManager.OnAudioFocusChangeListener {

    private var myBinder = MyBinder() // Binder для привязки сервиса к активности
    var mediaPlayer: MediaPlayer? = null // Плеер для воспроизведения музыки
    private lateinit var mediaSession: MediaSessionCompat // Медиасессия для управления воспроизведением
    private lateinit var runnable: Runnable // Runnable для обновления SeekBar
    lateinit var audioManager: AudioManager // Управление аудиофокусом

    // Привязка сервиса к активности
    override fun onBind(intent: Intent?): IBinder {
        mediaSession = MediaSessionCompat(baseContext, "My Music") // Инициализация медиасессии
        return myBinder
    }

    // Внутренний класс для предоставления доступа к сервису
    inner class MyBinder : Binder() {
        fun currentService(): MusicService {
            return this@MusicService // Возвращает текущий экземпляр сервиса
        }
    }

    // Отображение уведомления с элементами управления плеером
    @SuppressLint("UnspecifiedImmutableFlag")
    fun showNotification(playPauseBtn: Int) {

        // Создание интента для перехода в PlayerActivity
        val intent = Intent(baseContext, PlayerActivity::class.java)
        intent.putExtra("index", PlayerActivity.songPosition)
        intent.putExtra("class", "NowPlaying")

        // Флаг для PendingIntent (разные для разных версий Android)
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        // PendingIntent для перехода в PlayerActivity
        val contentIntent = PendingIntent.getActivity(this, 0, intent, flag)

        // Интенты для кнопок управления в уведомлении
        val prevIntent = Intent(
            baseContext,
            NotificationReceiver::class.java
        ).setAction(ApplicationClass.PREVIOUS)
        val prevPendingIntent = PendingIntent.getBroadcast(baseContext, 0, prevIntent, flag)

        val playIntent =
            Intent(baseContext, NotificationReceiver::class.java).setAction(ApplicationClass.PLAY)
        val playPendingIntent = PendingIntent.getBroadcast(baseContext, 0, playIntent, flag)

        val nextIntent =
            Intent(baseContext, NotificationReceiver::class.java).setAction(ApplicationClass.NEXT)
        val nextPendingIntent = PendingIntent.getBroadcast(baseContext, 0, nextIntent, flag)

        val exitIntent =
            Intent(baseContext, NotificationReceiver::class.java).setAction(ApplicationClass.EXIT)
        val exitPendingIntent = PendingIntent.getBroadcast(baseContext, 0, exitIntent, flag)

        // Получение обложки трека или использование placeholder
        val imgArt = getImgArt(PlayerActivity.musicListPA[PlayerActivity.songPosition].path)
        val image = if (imgArt != null) {
            BitmapFactory.decodeByteArray(imgArt, 0, imgArt.size)
        } else {
            BitmapFactory.decodeResource(resources, R.drawable.music_player_icon_splash_screen)
        }

        // Создание уведомления
        val notification =
            androidx.core.app.NotificationCompat.Builder(baseContext, ApplicationClass.CHANNEL_ID)
                .setContentIntent(contentIntent) // Действие при нажатии на уведомление
                .setContentTitle(PlayerActivity.musicListPA[PlayerActivity.songPosition].title) // Название трека
                .setContentText(PlayerActivity.musicListPA[PlayerActivity.songPosition].artist) // Исполнитель
                .setSmallIcon(R.drawable.music_icon) // Иконка уведомления
                .setLargeIcon(image) // Большая иконка (обложка)
                .setStyle(
                    androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.sessionToken) // Стиль медиауведомления
                )
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH) // Приоритет уведомления
                .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC) // Видимость уведомления
                .setOnlyAlertOnce(true) // Уведомление не должно показываться повторно
                .addAction(R.drawable.previous_icon, "Previous", prevPendingIntent) // Кнопка "Предыдущий"
                .addAction(playPauseBtn, "Play", playPendingIntent) // Кнопка "Воспроизведение/Пауза"
                .addAction(R.drawable.next_icon, "Next", nextPendingIntent) // Кнопка "Следующий"
                .addAction(R.drawable.exit_icon, "Exit", exitPendingIntent) // Кнопка "Выход"
                .build()

        // Настройка медиасессии для Android Q и выше
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val playbackSpeed = if (PlayerActivity.isPlaying) 1F else 0F // Скорость воспроизведения
            mediaSession.setMetadata(
                MediaMetadataCompat.Builder()
                    .putLong(
                        MediaMetadataCompat.METADATA_KEY_DURATION,
                        mediaPlayer!!.duration.toLong()
                    )
                    .build()
            )
            val playBackState = PlaybackStateCompat.Builder()
                .setState(
                    PlaybackStateCompat.STATE_PLAYING,
                    mediaPlayer!!.currentPosition.toLong(),
                    playbackSpeed
                )
                .setActions(PlaybackStateCompat.ACTION_SEEK_TO)
                .build()
            mediaSession.setPlaybackState(playBackState)
            mediaSession.setCallback(object : MediaSessionCompat.Callback() {

                // Обработка нажатия на кнопку воспроизведения/паузы
                override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
                    if (PlayerActivity.isPlaying) {
                        // Пауза музыки
                        PlayerActivity.binding.playPauseBtnPA.setIconResource(R.drawable.play_icon)
                        PlayerActivity.isPlaying = false
                        mediaPlayer!!.pause()
                        showNotification(R.drawable.play_icon)
                    } else {
                        // Возобновление воспроизведения
                        PlayerActivity.binding.playPauseBtnPA.setIconResource(R.drawable.pause_icon)
                        PlayerActivity.isPlaying = true
                        mediaPlayer!!.start()
                        showNotification(R.drawable.pause_icon)
                    }
                    return super.onMediaButtonEvent(mediaButtonEvent)
                }

                // Обработка перемотки
                override fun onSeekTo(pos: Long) {
                    super.onSeekTo(pos)
                    mediaPlayer!!.seekTo(pos.toInt())
                    val playBackStateNew = PlaybackStateCompat.Builder()
                        .setState(
                            PlaybackStateCompat.STATE_PLAYING,
                            mediaPlayer!!.currentPosition.toLong(),
                            playbackSpeed
                        )
                        .setActions(PlaybackStateCompat.ACTION_SEEK_TO)
                        .build()
                    mediaSession.setPlaybackState(playBackStateNew)
                }
            })
        }

        startForeground(13, notification) // Запуск уведомления как foreground-сервиса
    }

    // Создание и подготовка MediaPlayer
    fun createMediaPlayer() {
        try {
            if (mediaPlayer == null) mediaPlayer = MediaPlayer()
            mediaPlayer!!.reset()
            mediaPlayer!!.setDataSource(PlayerActivity.musicListPA[PlayerActivity.songPosition].path) // Установка пути к файлу
            mediaPlayer!!.prepare()
            PlayerActivity.binding.playPauseBtnPA.setIconResource(R.drawable.pause_icon) // Установка иконки паузы
            showNotification(R.drawable.pause_icon) // Обновление уведомления
            PlayerActivity.binding.tvSeekBarStart.text =
                formatDuration(mediaPlayer!!.currentPosition.toLong()) // Текущее время трека
            PlayerActivity.binding.tvSeekBarEnd.text =
                formatDuration(mediaPlayer!!.duration.toLong()) // Общая длительность трека
            PlayerActivity.binding.seekBarPA.progress = 0 // Сброс SeekBar
            PlayerActivity.binding.seekBarPA.max = mediaPlayer!!.duration // Установка максимального значения SeekBar
            PlayerActivity.nowPlayingId = PlayerActivity.musicListPA[PlayerActivity.songPosition].id // ID текущего трека
            PlayerActivity.loudnessEnhancer = LoudnessEnhancer(mediaPlayer!!.audioSessionId) // Усиление громкости
            PlayerActivity.loudnessEnhancer.enabled = true
        } catch (e: Exception) {
            return
        }
    }

    // Настройка SeekBar для отслеживания прогресса воспроизведения
    fun seekBarSetup() {
        runnable = Runnable {
            PlayerActivity.binding.tvSeekBarStart.text =
                formatDuration(mediaPlayer!!.currentPosition.toLong()) // Обновление текущего времени
            PlayerActivity.binding.seekBarPA.progress = mediaPlayer!!.currentPosition // Обновление прогресса SeekBar
            Handler(Looper.getMainLooper()).postDelayed(runnable, 200) // Повторный запуск через 200 мс
        }
        Handler(Looper.getMainLooper()).postDelayed(runnable, 0) // Первый запуск
    }

    // Обработка изменений аудиофокуса
    override fun onAudioFocusChange(focusChange: Int) {
        if (focusChange <= 0) {
            // Потеря фокуса: пауза музыки
            PlayerActivity.binding.playPauseBtnPA.setIconResource(R.drawable.play_icon)
            PlayerActivity.isPlaying = false
            mediaPlayer!!.pause()
            showNotification(R.drawable.play_icon)
        } else {
            // Возврат фокуса: возобновление воспроизведения
            PlayerActivity.binding.playPauseBtnPA.setIconResource(R.drawable.pause_icon)
            PlayerActivity.isPlaying = true
            mediaPlayer!!.start()
            showNotification(R.drawable.pause_icon)
        }
    }

    // Делает сервис persistent (перезапускаемым после завершения)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}