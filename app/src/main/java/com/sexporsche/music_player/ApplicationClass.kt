package com.sexporsche.music_player

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

// Класс приложения, который инициализируется при запуске приложения
class ApplicationClass : Application() {

    companion object {
        // Константы для идентификации уведомлений и действий
        const val CHANNEL_ID = "channel1" // ID канала уведомлений
        const val PLAY = "play" // Действие: воспроизведение
        const val NEXT = "next" // Действие: следующая песня
        const val PREVIOUS = "previous" // Действие: предыдущая песня
        const val EXIT = "exit" // Действие: выход
    }

    // Метод вызывается при создании приложения
    override fun onCreate() {
        super.onCreate()

        // Создание канала уведомлений для Android 8.0 (API 26) и выше
        val notificationChannel = NotificationChannel(
            CHANNEL_ID, // ID канала
            "Now Playing Song", // Название канала
            NotificationManager.IMPORTANCE_HIGH // Уровень важности канала
        )
        notificationChannel.description = "Это важный канал для отображения текущей песни!!" // Описание канала

        // Получение менеджера уведомлений и регистрация канала
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)
    }
}