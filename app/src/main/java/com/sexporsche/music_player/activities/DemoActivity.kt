package com.sexporsche.music_player.activities

// Импорты необходимых библиотек и классов Android SDK
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import android.view.SurfaceView
import android.view.animation.LinearInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import com.sexporsche.music_player.AbsAudioDataConverter
import com.sexporsche.music_player.AudioDataConverterFactory
import com.sexporsche.music_player.R
import com.sexporsche.music_player.setLocale
// Импорты библиотеки NierVisualizer для визуализации аудио
import me.bogerchan.niervisualizer.NierVisualizerManager
import me.bogerchan.niervisualizer.renderer.IRenderer
import me.bogerchan.niervisualizer.renderer.circle.CircleBarRenderer
import me.bogerchan.niervisualizer.renderer.circle.CircleRenderer
import me.bogerchan.niervisualizer.renderer.circle.CircleSolidRenderer
import me.bogerchan.niervisualizer.renderer.circle.CircleWaveRenderer
import me.bogerchan.niervisualizer.renderer.columnar.ColumnarType1Renderer
import me.bogerchan.niervisualizer.renderer.columnar.ColumnarType2Renderer
import me.bogerchan.niervisualizer.renderer.columnar.ColumnarType3Renderer
import me.bogerchan.niervisualizer.renderer.columnar.ColumnarType4Renderer
import me.bogerchan.niervisualizer.renderer.line.LineRenderer
import me.bogerchan.niervisualizer.renderer.other.ArcStaticRenderer
import me.bogerchan.niervisualizer.util.NierAnimator

// Главная активность для демонстрации визуализации аудио
class DemoActivity : AppCompatActivity() {

    companion object {
        // Константы для управления разрешениями, состояниями и настройками аудио
        const val REQUEST_CODE_AUDIO_PERMISSION = 1 // Код запроса для разрешения записи звука
        const val STATE_PLAYING = 0 // Состояние: воспроизведение
        const val STATE_PAUSE = 1 // Состояние: пауза
        const val STATE_STOP = 2 // Состояние: остановка
        const val STATUS_UNKNOWN = 0 // Неизвестный статус
        const val STATUS_AUDIO_RECORD = 1 // Статус: запись аудио
        const val STATUS_MEDIA_PLAYER = 2 // Статус: воспроизведение медиа
        const val SAMPLING_RATE = 44100 // Частота дискретизации (44.1 кГц)
        const val AUDIO_RECORD_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO // Моно канал
        const val AUDIO_RECORD_FORMAT = AudioFormat.ENCODING_PCM_16BIT // Формат PCM 16-bit
    }

    // Lazy-инициализация элементов интерфейса
    private val svWave by lazy { findViewById<SurfaceView>(R.id.sv_wave) } // SurfaceView для отображения визуализации
    private var mVisualizerManager: NierVisualizerManager? = null // Менеджер визуализации
    private val tvChangeStyle by lazy { findViewById<TextView>(R.id.tv_change_style) } // TextView для изменения стиля визуализации
    private val tvAudioRecordStartOrStop by lazy { findViewById<TextView>(R.id.tv_audio_record_start_or_stop) } // TextView для старта/остановки записи

    // Определение размера буфера для AudioRecord
    private val mAudioBufferSize by lazy {
        val size = AudioRecord.getMinBufferSize(
            SAMPLING_RATE,
            AUDIO_RECORD_CHANNEL_CONFIG,
            AUDIO_RECORD_FORMAT
        )
        if (size == AudioRecord.ERROR || size == AudioRecord.ERROR_BAD_VALUE) {
            4096 // Устанавливаем безопасное значение по умолчанию
        } else {
            size
        }
    }

    // Инициализация AudioRecord для записи звука с микрофона
    private val mAudioRecord by lazy {
        AudioRecord(
            MediaRecorder.AudioSource.MIC, // Используем микрофон как источник звука
            SAMPLING_RATE,
            AUDIO_RECORD_CHANNEL_CONFIG,
            AUDIO_RECORD_FORMAT,
            mAudioBufferSize
        )
    }

    // Массив рендеров для различных стилей визуализации
    private val mRenderers = arrayOf<Array<IRenderer>>(
        // Примеры различных типов рендеров
        arrayOf(ColumnarType1Renderer()),
        arrayOf(ColumnarType2Renderer()),
        arrayOf(ColumnarType3Renderer()),
        arrayOf(ColumnarType4Renderer()),
        arrayOf(LineRenderer(true)),
        arrayOf(CircleBarRenderer()),
        arrayOf(CircleRenderer(true)),
        arrayOf(
            CircleRenderer(true),
            CircleBarRenderer(),
            ColumnarType4Renderer()
        ),
        arrayOf(CircleRenderer(true), CircleBarRenderer(), LineRenderer(true)),
        arrayOf(
            ArcStaticRenderer(
                paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#cfa9d0fd")
                }),
            ArcStaticRenderer(
                paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#dad2eafe")
                },
                amplificationOuter = .83f,
                startAngle = -90f,
                sweepAngle = 225f
            ),
            ArcStaticRenderer(
                paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#7fa9d0fd")
                },
                amplificationOuter = .93f,
                amplificationInner = 0.8f,
                startAngle = -45f,
                sweepAngle = 135f
            ),
            CircleSolidRenderer(
                paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#d2eafe")
                },
                amplification = .45f
            ),
            CircleBarRenderer(
                paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    strokeWidth = 4f
                    color = Color.parseColor("#efe3f2ff")
                },
                modulationStrength = 1f,
                type = CircleBarRenderer.Type.TYPE_A_AND_TYPE_B,
                amplification = 1f, divisions = 8
            ),
            CircleBarRenderer(
                paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    strokeWidth = 5f
                    color = Color.parseColor("#e3f2ff")
                },
                modulationStrength = 0.1f,
                amplification = 1.2f,
                divisions = 8
            ),
            CircleWaveRenderer(
                paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    strokeWidth = 6f
                    color = Color.WHITE
                },
                modulationStrength = 0.2f,
                type = CircleWaveRenderer.Type.TYPE_B,
                amplification = 1f,
                animator = NierAnimator(
                    interpolator = LinearInterpolator(),
                    duration = 20000,
                    values = floatArrayOf(0f, -360f)
                )
            ),
            CircleWaveRenderer(
                paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    strokeWidth = 6f
                    color = Color.parseColor("#7fcee7fe")
                },
                modulationStrength = 0.2f,
                type = CircleWaveRenderer.Type.TYPE_B,
                amplification = 1f,
                divisions = 8,
                animator = NierAnimator(
                    interpolator = LinearInterpolator(),
                    duration = 20000,
                    values = floatArrayOf(0f, -360f)
                )
            )
        )
    )

    // Переменные для управления состоянием приложения
    private var mCurrentStyleIndex = 0 // Текущий индекс стиля визуализации
    private var mMediaPlayerState = STATE_STOP // Состояние медиаплеера
    private var mAudioRecordState = STATE_STOP // Состояние записи аудио
    private var mStatus = STATUS_UNKNOWN // Текущий статус приложения

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE)
        val appLanguage = sharedPreferences.getString("App_Language", "")
        if (!appLanguage.isNullOrEmpty()) {
            setLocale(this, appLanguage)
        }
        // Настройка цвета навигационной панели для версий Android >= Lollipop
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.navigationBarColor =
                ResourcesCompat.getColor(resources, R.color.cool_pink, theme)
        }

        supportActionBar?.hide() // Скрываем ActionBar
        setContentView(R.layout.activity_visualization) // Устанавливаем макет активности

        // Настройка SurfaceView для отображения визуализации
        svWave.setZOrderOnTop(true)
        svWave.holder.setFormat(PixelFormat.TRANSLUCENT)

        // Обработчик нажатия на кнопку изменения стиля визуализации
        tvChangeStyle.setOnClickListener {
            changeStyle()
        }

        // Обработчик нажатия на кнопку старта/остановки записи
        tvAudioRecordStartOrStop.setOnClickListener {
            ensurePermissionAllowed() // Проверяем наличие разрешений
            mAudioRecord.apply {
                when (mAudioRecordState) {
                    STATE_PLAYING -> {
                        stop() // Останавливаем запись
                        mAudioRecordState = STATE_STOP
                        mVisualizerManager?.stop() // Останавливаем визуализацию
                        tvAudioRecordStartOrStop.text = "START" // Изменяем текст кнопки
                    }
                    STATE_STOP -> {
                        startRecording() // Начинаем запись
                        mAudioRecordState = STATE_PLAYING
                        mStatus = STATUS_AUDIO_RECORD
                        createNewVisualizerManager() // Создаем новый менеджер визуализации
                        useStyle(mCurrentStyleIndex) // Применяем текущий стиль
                        tvAudioRecordStartOrStop.text = "STOP" // Изменяем текст кнопки
                    }
                }
            }
        }

        createNewVisualizerManager() // Инициализация менеджера визуализации
        useStyle(mCurrentStyleIndex) // Применение начального стиля
        mStatus = STATUS_AUDIO_RECORD
    }

    // Метод для изменения стиля визуализации
    private fun changeStyle() {
        useStyle(++mCurrentStyleIndex)
    }

    // Метод для применения выбранного стиля визуализации
    private fun useStyle(idx: Int) {
        mVisualizerManager?.start(svWave, mRenderers[idx % mRenderers.size])
    }

    // Метод для создания нового менеджера визуализации
    private fun createNewVisualizerManager() {
        mVisualizerManager?.release() // Освобождаем предыдущий менеджер
        mVisualizerManager = NierVisualizerManager().apply {
            init(object : NierVisualizerManager.NVDataSource {
                private val mBuffer: ByteArray = ByteArray(512) // Буфер для данных аудио
                private val mAudioDataConverter: AbsAudioDataConverter =
                    AudioDataConverterFactory.getConverterByAudioRecord(mAudioRecord)

                override fun getDataSamplingInterval() = 0L // Возвращает интервал выборки данных
                override fun getDataLength() = mBuffer.size // Возвращает длину данных
                override fun fetchFftData(): ByteArray? = null // Возвращает данные FFT (не используется)
                override fun fetchWaveData(): ByteArray? {
                    return if (mAudioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        mAudioDataConverter.convertWaveDataTo(mBuffer) // Преобразуем данные волн в буфер
                        mBuffer
                    } else {
                        ByteArray(mBuffer.size) // Возвращаем пустой массив, если запись не активна
                    }
                }
            })
        }
    }

    // Метод вызывается при переходе активности в состояние onStart
    override fun onStart() {
        super.onStart()
        if (mStatus == STATUS_AUDIO_RECORD && mAudioRecordState == STATE_PLAYING) {
            mVisualizerManager?.resume() // Возобновляем визуализацию
        }
    }

    // Метод вызывается при переходе активности в состояние onStop
    override fun onStop() {
        super.onStop()
        mVisualizerManager?.pause() // Приостанавливаем визуализацию
    }

    // Метод вызывается при уничтожении активности
    override fun onDestroy() {
        super.onDestroy()
        mVisualizerManager?.release() // Освобождаем ресурсы менеджера визуализации
        mVisualizerManager = null
        if (mAudioRecordState == STATE_PLAYING) {
            mAudioRecord.stop() // Останавливаем запись, если она активна
        }
        mAudioRecord.release() // Освобождаем ресурсы AudioRecord
    }

    // Метод для получения ID аудиосессии системы
    private fun getSystemAudioSessionId(): Int {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        return audioManager.generateAudioSessionId()
    }

    // Метод для проверки наличия разрешения на запись звука
    private fun ensurePermissionAllowed() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.RECORD_AUDIO),
                REQUEST_CODE_AUDIO_PERMISSION
            )
        }
    }

    // Обработка результатов запроса разрешений
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_AUDIO_PERMISSION -> {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(
                        this,
                        "Разрешение на запись звука не предоставлено",
                        Toast.LENGTH_LONG
                    ).show()
                    finish() // Закрываем активность, если разрешение не предоставлено
                }
            }
        }
    }
}