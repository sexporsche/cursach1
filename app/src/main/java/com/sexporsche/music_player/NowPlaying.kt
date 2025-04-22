package com.sexporsche.music_player

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.sexporsche.music_player.databinding.FragmentNowPlayingBinding

// Фрагмент для отображения текущего воспроизводимого трека
class NowPlaying : Fragment() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var binding: FragmentNowPlayingBinding // Привязка к макету фрагмента
    }

    // Создание представления фрагмента
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_now_playing, container, false)
        binding = FragmentNowPlayingBinding.bind(view) // Привязка макета
        binding.root.visibility = View.INVISIBLE // Скрываем фрагмент до загрузки данных

        // Обработчик нажатия на кнопку Play/Pause
        binding.playPauseBtnNP.setOnClickListener {
            if (PlayerActivity.isPlaying) pauseMusic() else playMusic()
        }

        // Обработчик нажатия на корневой элемент фрагмента
        binding.root.setOnClickListener {
            // Открытие PlayerActivity при нажатии на фрагмент
            val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                putExtra("index", PlayerActivity.songPosition) // Передача индекса текущей песни
                putExtra("class", "NowPlaying") // Указание источника
            }
            ContextCompat.startActivity(requireContext(), intent, null) // Запуск активности
        }
        return view
    }

    // Вызывается при возобновлении фрагмента
    override fun onResume() {
        super.onResume()
        if (PlayerActivity.musicService != null) {
            binding.root.visibility = View.VISIBLE // Делаем фрагмент видимым
            binding.songNameNP.isSelected = true // Включение анимации прокрутки текста

            // Загрузка обложки трека с использованием Glide
            Glide.with(requireContext())
                .load(PlayerActivity.musicListPA[PlayerActivity.songPosition].artUri) // URI обложки
                .apply(
                    RequestOptions()
                        .placeholder(R.drawable.music_player_icon_splash_screen) // Placeholder на случай ошибки загрузки
                        .centerCrop() // Обрезка изображения по центру
                )
                .into(binding.songImgNP) // Установка изображения в ImageView

            // Установка названия трека
            binding.songNameNP.text = PlayerActivity.musicListPA[PlayerActivity.songPosition].title

            // Установка иконки кнопки Play/Pause в зависимости от состояния воспроизведения
            if (PlayerActivity.isPlaying) {
                binding.playPauseBtnNP.setIconResource(R.drawable.pause_icon) // Иконка паузы
            } else {
                binding.playPauseBtnNP.setIconResource(R.drawable.play_icon) // Иконка воспроизведения
            }
        }
    }

    // Метод для воспроизведения музыки
    private fun playMusic() {
        PlayerActivity.isPlaying = true // Установка флага воспроизведения
        PlayerActivity.musicService?.mediaPlayer?.start() // Запуск плеера
        binding.playPauseBtnNP.setIconResource(R.drawable.pause_icon) // Изменение иконки кнопки
        PlayerActivity.musicService?.showNotification(R.drawable.pause_icon) // Обновление уведомления
    }

    // Метод для паузы музыки
    private fun pauseMusic() {
        PlayerActivity.isPlaying = false // Сброс флага воспроизведения
        PlayerActivity.musicService?.mediaPlayer?.pause() // Пауза плеера
        binding.playPauseBtnNP.setIconResource(R.drawable.play_icon) // Изменение иконки кнопки
        PlayerActivity.musicService?.showNotification(R.drawable.play_icon) // Обновление уведомления
    }
}