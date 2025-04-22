package com.sexporsche.music_player

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.database.Cursor
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import com.bumptech.glide.Glide
import android.graphics.BitmapFactory
import android.media.audiofx.LoudnessEnhancer
import android.graphics.drawable.GradientDrawable
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sexporsche.music_player.activities.DemoActivity
import com.sexporsche.music_player.databinding.ActivityPlayerBinding
import com.sexporsche.music_player.databinding.AudioBoosterBinding
import com.sexporsche.music_player.databinding.DetailsViewBinding
import com.sexporsche.music_player.databinding.MoreFeaturesBinding

class PlayerActivity : AppCompatActivity(), ServiceConnection, MediaPlayer.OnCompletionListener {

    companion object {
        lateinit var musicListPA: ArrayList<Music>
        var songPosition: Int = 0
        var isPlaying: Boolean = false
        var musicService: MusicService? = null

        @SuppressLint("StaticFieldLeak")
        lateinit var binding: ActivityPlayerBinding
        var repeat: Boolean = false
        var min15: Boolean = false
        var min30: Boolean = false
        var min60: Boolean = false
        var nowPlayingId: String = ""
        var isFavourite: Boolean = false
        var fIndex: Int = -1
        lateinit var loudnessEnhancer: LoudnessEnhancer
    }

    @SuppressLint("ResourceAsColor", "ResourceType", "SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE)
        val appLanguage = sharedPreferences.getString("App_Language", "")
        if (!appLanguage.isNullOrEmpty()) {
            setLocale(this, appLanguage)
        }
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent.data?.scheme.contentEquals("content")) {
            songPosition = 0;
            val intentService = Intent(this, MusicService::class.java)
            bindService(intentService, this, BIND_AUTO_CREATE)
            startService(intentService)
            musicListPA = ArrayList()
            musicListPA.add(getMusicDetails(intent.data!!))
            Glide.with(this)
                .load(getImgArt(musicListPA[songPosition].path))
                .apply(
                    RequestOptions().placeholder(R.drawable.music_player_icon_splash_screen)
                        .centerCrop()
                )
                .into(binding.songImgPA)
            binding.songNamePA.text = musicListPA[songPosition].title
        } else initializeLayout()

        binding.playPauseBtnPA.setOnClickListener { if (isPlaying) pauseMusic() else playMusic() }
        binding.backBtnPA.setOnClickListener { finish() }
        binding.previousBtnPA.setOnClickListener { prevNextSong(increment = false) }
        binding.nextBtnPA.setOnClickListener { prevNextSong(increment = true) }
        binding.seekBarPA.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    musicService!!.mediaPlayer!!.seekTo(progress)
                    musicService!!.showNotification(if (isPlaying) R.drawable.pause_icon else R.drawable.play_icon)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        binding.moreFeaturesBtnPA.setOnClickListener {
            pauseMusic()
            val customDialog =
                LayoutInflater.from(this).inflate(R.layout.more_features, binding.root, false)
            val bindingMF = MoreFeaturesBinding.bind(customDialog)
            val dialog = MaterialAlertDialogBuilder(this).setView(customDialog)
                .setOnCancelListener { playMusic() }
                .setBackground(ColorDrawable(0x80000000.toInt()))
                .create()
            dialog.show()
            if (repeat) {
                bindingMF.repeatBtnPA.setBackgroundColor(
                    ContextCompat.getColor(
                        this,
                        R.color.cool_pink
                    )
                )
            }
            bindingMF.repeatBtnPA.setOnClickListener {
                if (!repeat) {
                    repeat = true
                    bindingMF.repeatBtnPA.setBackgroundColor(
                        ContextCompat.getColor(
                            this,
                            R.color.cool_pink
                        )
                    )
                } else {
                    repeat = false
                    bindingMF.repeatBtnPA.setBackgroundColor(Color.TRANSPARENT)
                }
            }
            bindingMF.equalizerBtnPA.setOnClickListener {
                try {
                    val eqIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                    eqIntent.putExtra(
                        AudioEffect.EXTRA_AUDIO_SESSION,
                        musicService!!.mediaPlayer!!.audioSessionId
                    )
                    eqIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, baseContext.packageName)
                    eqIntent.putExtra(
                        AudioEffect.EXTRA_CONTENT_TYPE,
                        AudioEffect.CONTENT_TYPE_MUSIC
                    )
                    startActivityForResult(eqIntent, 13)
                } catch (e: Exception) {
                    Toast.makeText(this, "Equalizer Feature not Supported!!", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            bindingMF.shareBtnPA.setOnClickListener {
                dialog.dismiss()
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.type = "audio/*"
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(musicListPA[songPosition].path))
                startActivity(Intent.createChooser(shareIntent, "Sharing Music File!!"))
            }
            bindingMF.detailsBtnPA.setOnClickListener {
                dialog.dismiss()
                val customDialogIF =
                    LayoutInflater.from(this).inflate(R.layout.details_view, binding.root, false)
                val bindingIF = DetailsViewBinding.bind(customDialogIF)
                val dialogIF = MaterialAlertDialogBuilder(this).setView(customDialogIF)
                    .setCancelable(false)
                    .setBackground(ColorDrawable(0x80000000.toInt()))
                    .setPositiveButton("Ok") { self, _ ->
                        self.dismiss()
                    }
                    .create()
                dialogIF.show()
                val infoText = SpannableStringBuilder().bold { append("DETAILS\n\nName: ") }
                    .append(musicListPA[songPosition].title)
                    .bold { append("\n\nDuration: ") }
                    .append(DateUtils.formatElapsedTime(musicListPA[songPosition].duration / 1000))
                    .bold { append("\n\nLocation: ") }.append(musicListPA[songPosition].path)

                bindingIF.detailTV.text = infoText
                dialogIF.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(MaterialColors.getColor(baseContext, R.color.white, Color.WHITE))
                dialogIF.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(
                    MaterialColors.getColor(
                        baseContext,
                        R.color.black_T,
                        Color.BLACK
                    )
                )
            }
            bindingMF.deleteBtnPA.setOnClickListener {
                dialog.dismiss()
                Toast.makeText(this, "Under Maintenance", Toast.LENGTH_SHORT).show()
            }
            //audio booster feature
            bindingMF.boosterBtnPA.setOnClickListener {
                val customDialogB =
                    LayoutInflater.from(this).inflate(R.layout.audio_booster, binding.root, false)
                val bindingB = AudioBoosterBinding.bind(customDialogB)
                val dialogB = MaterialAlertDialogBuilder(this).setView(customDialogB)
                    .setOnCancelListener { playMusic() }
                    .setPositiveButton("OK") { self, _ ->
                        loudnessEnhancer.setTargetGain(bindingB.verticalBar.progress * 100)
                        playMusic()
                        self.dismiss()
                    }
                    .setBackground(ColorDrawable(0x803700B3.toInt()))
                    .create()
                dialogB.show()

                bindingB.verticalBar.progress = loudnessEnhancer.targetGain.toInt() / 100
                bindingB.progressText.text =
                    "Audio Boost\n\n${loudnessEnhancer.targetGain.toInt() / 10} %"
                bindingB.verticalBar.setOnProgressChangeListener {
                    bindingB.progressText.text = "Audio Boost\n\n${it * 10} %"
                }
            }
        }
        binding.timerBtnPA.setOnClickListener {
            val timer = min15 || min30 || min60
            if (!timer) showBottomSheetDialog()
            else {
                val customDialogTL =
                    LayoutInflater.from(this).inflate(R.layout.artist_view, binding.root, false)
                val bindingTL = DetailsViewBinding.bind(customDialogTL)
                val dialogTL = MaterialAlertDialogBuilder(this).setView(customDialogTL)
                    .setCancelable(false)
                    .setBackground(ColorDrawable(0x80000000.toInt()))
                    .setPositiveButton("Yes") { _, _ ->
                        min15 = false
                        min30 = false
                        min60 = false
                        binding.timerBtnPA.setColorFilter(
                            ContextCompat.getColor(
                                this,
                                R.color.black
                            )
                        )
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                dialogTL.show()
                val artistText = "Stop Timer\n\nDo you want to stop timer?"
                bindingTL.detailTV.text = artistText
                dialogTL.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(MaterialColors.getColor(baseContext, R.color.white, Color.WHITE))
                dialogTL.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(
                    MaterialColors.getColor(
                        baseContext,
                        R.color.black_T,
                        Color.BLACK
                    )
                )
                dialogTL.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(MaterialColors.getColor(baseContext, R.color.white, Color.WHITE))
                dialogTL.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(
                    MaterialColors.getColor(
                        baseContext,
                        R.color.black_T,
                        Color.BLACK
                    )
                )
            }
        }
        binding.favouriteBtnPA.setOnClickListener {
            fIndex = favouriteChecker(musicListPA[songPosition].id)
            if (isFavourite) {
                isFavourite = false
                binding.favouriteBtnPA.setImageResource(R.drawable.favourite_empty_icon)
                FavouriteActivity.favouriteSongs.removeAt(fIndex)
            } else {
                isFavourite = true
                binding.favouriteBtnPA.setImageResource(R.drawable.favourite_icon)
                FavouriteActivity.favouriteSongs.add(musicListPA[songPosition])
            }
            FavouriteActivity.favouritesChanged = true
        }
        binding.openVisualizationBtn.setOnClickListener {
            val intent = Intent(this, DemoActivity::class.java)
            startActivity(intent)
        }
    }

    private fun initializeLayout() {
        songPosition = intent.getIntExtra("index", 0)
        when (intent.getStringExtra("class")) {
            "NowPlaying" -> {
                setLayout()
                binding.tvSeekBarStart.text =
                    formatDuration(musicService!!.mediaPlayer!!.currentPosition.toLong())
                binding.tvSeekBarEnd.text =
                    formatDuration(musicService!!.mediaPlayer!!.duration.toLong())
                binding.seekBarPA.progress = musicService!!.mediaPlayer!!.currentPosition
                binding.seekBarPA.max = musicService!!.mediaPlayer!!.duration
                if (isPlaying) binding.playPauseBtnPA.setIconResource(R.drawable.pause_icon)
                else binding.playPauseBtnPA.setIconResource(R.drawable.play_icon)
            }

            "MusicAdapterSearch" -> initServiceAndPlaylist(
                MainActivity.musicListSearch,
                shuffle = false
            )

            "MusicAdapter" -> initServiceAndPlaylist(MainActivity.MusicListMA, shuffle = false)
            "FavouriteAdapter" -> initServiceAndPlaylist(
                FavouriteActivity.favouriteSongs,
                shuffle = false
            )

            "MainActivity" -> initServiceAndPlaylist(MainActivity.MusicListMA, shuffle = true)
            "FavouriteShuffle" -> initServiceAndPlaylist(
                FavouriteActivity.favouriteSongs,
                shuffle = true
            )

            "PlaylistDetailsAdapter" ->
                initServiceAndPlaylist(
                    PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist,
                    shuffle = false
                )

            "PlaylistDetailsShuffle" ->
                initServiceAndPlaylist(
                    PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist,
                    shuffle = true
                )
//            "PlayNext"->initServiceAndPlaylist(PlayNext.playNextList, shuffle = false, playNext = true)
        }
        if (musicService != null && !isPlaying) playMusic()
    }

    private fun setLayout() {
        fIndex = favouriteChecker(musicListPA[songPosition].id)
        Glide.with(applicationContext)
            .load(musicListPA[songPosition].artUri)
            .apply(
                RequestOptions().placeholder(R.drawable.music_player_icon_splash_screen)
                    .centerCrop()
            )
            .into(binding.songImgPA)
        binding.songNamePA.text = musicListPA[songPosition].title
        if (min15 || min30 || min60) binding.timerBtnPA.setColorFilter(
            ContextCompat.getColor(
                applicationContext,
                R.color.purple_500
            )
        )
        if (isFavourite) binding.favouriteBtnPA.setImageResource(R.drawable.favourite_icon)
        else binding.favouriteBtnPA.setImageResource(R.drawable.favourite_empty_icon)

        val img = getImgArt(musicListPA[songPosition].path)
        val image = if (img != null) {
            BitmapFactory.decodeByteArray(img, 0, img.size)
        } else {
            BitmapFactory.decodeResource(
                resources,
                R.drawable.music_player_icon_splash_screen
            )
        }
        val bgColor = getMainColor(image)
        val gradient =
            GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, intArrayOf(0xFFFFFF, bgColor))
        binding.root.background = gradient
        window?.statusBarColor = bgColor
    }

    private fun createMediaPlayer() {
        try {
            if (musicService!!.mediaPlayer == null) musicService!!.mediaPlayer = MediaPlayer()
            musicService!!.mediaPlayer!!.reset()
            musicService!!.mediaPlayer!!.setDataSource(musicListPA[songPosition].path)
            musicService!!.mediaPlayer!!.prepare()
            binding.tvSeekBarStart.text =
                formatDuration(musicService!!.mediaPlayer!!.currentPosition.toLong())
            binding.tvSeekBarEnd.text =
                formatDuration(musicService!!.mediaPlayer!!.duration.toLong())
            binding.seekBarPA.progress = 0
            binding.seekBarPA.max = musicService!!.mediaPlayer!!.duration
            musicService!!.mediaPlayer!!.setOnCompletionListener(this)
            nowPlayingId = musicListPA[songPosition].id
            playMusic()
            loudnessEnhancer = LoudnessEnhancer(musicService!!.mediaPlayer!!.audioSessionId)
            loudnessEnhancer.enabled = true
        } catch (e: Exception) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
        }
    }

    private fun playMusic() {
        isPlaying = true
        musicService!!.mediaPlayer!!.start()
        binding.playPauseBtnPA.setIconResource(R.drawable.pause_icon)
        musicService!!.showNotification(R.drawable.pause_icon)
    }

    private fun pauseMusic() {
        isPlaying = false
        musicService!!.mediaPlayer!!.pause()
        binding.playPauseBtnPA.setIconResource(R.drawable.play_icon)
        musicService!!.showNotification(R.drawable.play_icon)


    }

    private fun prevNextSong(increment: Boolean) {
        if (increment) {
            setSongPosition(increment = true)
            setLayout()
            createMediaPlayer()
        } else {
            setSongPosition(increment = false)
            setLayout()
            createMediaPlayer()
        }
    }


    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        if (musicService == null) {
            val binder = service as MusicService.MyBinder
            musicService = binder.currentService()
            musicService!!.audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            musicService!!.audioManager.requestAudioFocus(
                musicService,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        createMediaPlayer()
        musicService!!.seekBarSetup()


    }

    override fun onServiceDisconnected(name: ComponentName?) {
        musicService = null
    }

    override fun onCompletion(mp: MediaPlayer?) {
        setSongPosition(increment = true)
        createMediaPlayer()
        setLayout()

        //for refreshing now playing image & text on song completion
        NowPlaying.binding.songNameNP.isSelected = true
        Glide.with(applicationContext)
            .load(musicListPA[songPosition].artUri)
            .apply(
                RequestOptions().placeholder(R.drawable.music_player_icon_splash_screen)
                    .centerCrop()
            )
            .into(NowPlaying.binding.songImgNP)
        NowPlaying.binding.songNameNP.text = musicListPA[songPosition].title
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 13 || resultCode == RESULT_OK)
            return
    }

    private fun showBottomSheetDialog() {
        val dialog = BottomSheetDialog(this@PlayerActivity)
        dialog.setContentView(R.layout.bottom_sheet_dialog)
        dialog.show()
        dialog.findViewById<LinearLayout>(R.id.min_15)?.setOnClickListener {
            Toast.makeText(baseContext, "Music will stop after 15 minutes", Toast.LENGTH_SHORT)
                .show()
            binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.cool_pink))
            min15 = true
            Thread {
                Thread.sleep((15 * 60000).toLong())
                if (min15) exitApplication()
            }.start()
            dialog.dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.min_30)?.setOnClickListener {
            Toast.makeText(baseContext, "Music will stop after 30 minutes", Toast.LENGTH_SHORT)
                .show()
            binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.cool_pink))
            min30 = true
            Thread {
                Thread.sleep((30 * 60000).toLong())
                if (min30) exitApplication()
            }.start()
            dialog.dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.min_60)?.setOnClickListener {
            Toast.makeText(baseContext, "Music will stop after 60 minutes", Toast.LENGTH_SHORT)
                .show()
            binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.cool_pink))
            min60 = true
            Thread {
                Thread.sleep((60 * 60000).toLong())
                if (min60) exitApplication()
            }.start()
            dialog.dismiss()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getMusicDetails(contentUri: Uri): Music {
        var cursor: Cursor? = null
        try {
            val projection = arrayOf(MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DURATION)
            cursor = this.contentResolver.query(contentUri, projection, null, null, null)
            val dataColumn = cursor?.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val durationColumn = cursor?.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            cursor!!.moveToFirst()
            val path = dataColumn?.let { cursor.getString(it) }
            val duration = durationColumn?.let { cursor.getLong(it) }!!
            return Music(
                id = "Unknown",
                title = path.toString(),
                album = "Unknown",
                artist = "Unknown",
                duration = duration,
                artUri = "Unknown",
                path = path.toString()
            )
        } finally {
            cursor?.close()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (musicListPA[songPosition].id == "Unknown" && !isPlaying) exitApplication()
    }

    private fun initServiceAndPlaylist(
        playlist: ArrayList<Music>,
        shuffle: Boolean,
        playNext: Boolean = false
    ) {
        val intent = Intent(this, MusicService::class.java)
        bindService(intent, this, BIND_AUTO_CREATE)
        startService(intent)
        musicListPA = ArrayList()
        musicListPA.addAll(playlist)
        if (shuffle) musicListPA.shuffle()
        setLayout()
//        if(!playNext) PlayNext.playNextList = ArrayList()
    }

}
