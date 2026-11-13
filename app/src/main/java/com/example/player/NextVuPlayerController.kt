package com.example.player

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.MediaCodecList
import android.net.Uri
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ColorInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import com.example.data.local.VideoQualityPreset
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class HdrBadgeType(val label: String) {
    NONE(""),
    HDR10("HDR10"),
    HDR10_PLUS("HDR10+"),
    HLG("HLG"),
    DOLBY_VISION("DOLBY VISION")
}

data class MediaTrackInfo(
    val id: String,
    val label: String,
    val language: String,
    val codec: String,
    val groupIndex: Int,
    val trackIndex: Int,
    val isSelected: Boolean,
    val isAc3OrEac3: Boolean = false,
    val isSupported: Boolean = true
)

data class PlayerTelemetryState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = true,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Float = 1.0f,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val hdrType: HdrBadgeType = HdrBadgeType.NONE,
    val bitrateKbps: Long = 0L,
    val videoCodec: String = "",
    val isHevcStream: Boolean = false,
    val isHardwareHevcSupported: Boolean = true,
    val showHevcSoftwareFallbackWarning: Boolean = false,
    val isAc3PassthroughSupported: Boolean = false,
    val audioTracks: List<MediaTrackInfo> = emptyList(),
    val subtitleTracks: List<MediaTrackInfo> = emptyList(),
    val selectedQuality: VideoQualityPreset = VideoQualityPreset.AUTO,
    val errorMessage: String? = null,
    val playbackEnded: Boolean = false
) {
    val is4kStream: Boolean
        get() = videoWidth >= 3840 || videoHeight >= 2160
}

/**
 * Unified Media3 ExoPlayer engine wrapper supporting:
 * - HEVC / H.265 hardware decoding probe + software fallback warning
 * - Full 4K 2160p UHD playback + HDR10/HLG/Dolby Vision badge detection
 * - AC3 / E-AC3 passthrough probe + PCM decoding fallback
 * - Real-time bitrate & resolution telemetry
 * - Dynamic Audio & Subtitle track selection + Quality preset caps
 */
@OptIn(UnstableApi::class)
class NextVuPlayerController(private val context: Context) {

    private val bandwidthMeter = DefaultBandwidthMeter.Builder(context).build()
    private val trackSelector = DefaultTrackSelector(context)

    val isHardwareHevcSupported: Boolean = probeHardwareHevcDecoder()
    val isAc3PassthroughSupported: Boolean = probeAc3Passthrough(context)

    private val _state = MutableStateFlow(
        PlayerTelemetryState(
            isHardwareHevcSupported = isHardwareHevcSupported,
            isAc3PassthroughSupported = isAc3PassthroughSupported
        )
    )
    val state: StateFlow<PlayerTelemetryState> = _state.asStateFlow()

    val exoPlayer: ExoPlayer
    @Volatile
    private var isReleased = false

    init {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("NextVuPlayer/1.1 (Android)")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)

        val renderersFactory = NextRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            .setEnableDecoderFallback(true)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                10_000, // minBufferMs
                25_000, // maxBufferMs (capped to prevent RAM pressure alongside large catalogs)
                2_000,  // bufferForPlaybackMs
                4_000   // bufferForPlaybackAfterRebufferMs
            )
            .setPrioritizeTimeOverSizeThresholds(false)
            .build()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        exoPlayer = ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setBandwidthMeter(bandwidthMeter)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        attachListeners()
    }

    private fun attachListeners() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (isReleased) return
                val dur = try {
                    exoPlayer.duration.coerceAtLeast(0L)
                } catch (_: Throwable) {
                    0L
                }
                _state.update {
                    it.copy(
                        isBuffering = playbackState == Player.STATE_BUFFERING,
                        playbackEnded = playbackState == Player.STATE_ENDED,
                        durationMs = dur
                    )
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                _state.update {
                    it.copy(
                        videoWidth = videoSize.width,
                        videoHeight = videoSize.height
                    )
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                inspectTracks(tracks)
            }

            override fun onPlayerError(error: PlaybackException) {
                _state.update {
                    it.copy(
                        isBuffering = false,
                        isPlaying = false,
                        errorMessage = error.localizedMessage ?: "Playback error (${error.errorCodeName})"
                    )
                }
            }
        })

        exoPlayer.addAnalyticsListener(object : AnalyticsListener {
            override fun onBandwidthEstimate(
                eventTime: AnalyticsListener.EventTime,
                totalLoadTimeMs: Int,
                totalBytesLoaded: Long,
                bitrateEstimate: Long
            ) {
                val estimateKbps = (bitrateEstimate / 1000L).coerceAtLeast(0L)
                if (estimateKbps > 0L) {
                    _state.update { it.copy(bitrateKbps = estimateKbps) }
                }
            }
        })
    }

    private fun inspectTracks(tracks: Tracks) {
        val audioList = mutableListOf<MediaTrackInfo>()
        val subList = mutableListOf<MediaTrackInfo>()
        var detectedHdr = HdrBadgeType.NONE
        var detectedCodec = ""
        var hevcDetected = false
        var formatBitrateKbps = 0L

        tracks.groups.forEachIndexed { groupIndex, group ->
            when (group.type) {
                C.TRACK_TYPE_VIDEO -> {
                    for (i in 0 until group.length) {
                        if (group.isTrackSelected(i)) {
                            val fmt = group.getTrackFormat(i)
                            val mime = fmt.sampleMimeType.orEmpty()
                            detectedCodec = mime
                            if (mime.equals(MimeTypes.VIDEO_H265, ignoreCase = true) ||
                                fmt.codecs.orEmpty().contains("hev", ignoreCase = true) ||
                                fmt.codecs.orEmpty().contains("hvc", ignoreCase = true)
                            ) {
                                hevcDetected = true
                            }
                            if (fmt.bitrate > 0) {
                                formatBitrateKbps = fmt.bitrate / 1000L
                            }
                            detectedHdr = detectHdrBadge(fmt.colorInfo, mime, fmt.codecs)
                        }
                    }
                }

                C.TRACK_TYPE_AUDIO -> {
                    for (i in 0 until group.length) {
                        val fmt = group.getTrackFormat(i)
                        val mime = fmt.sampleMimeType.orEmpty()
                        val isAc3 = mime == MimeTypes.AUDIO_AC3 ||
                            mime == MimeTypes.AUDIO_E_AC3 ||
                            mime == MimeTypes.AUDIO_E_AC3_JOC
                        val lang = fmt.language ?: "und"
                        val codecTag = when (mime) {
                            MimeTypes.AUDIO_AC3 -> "AC3"
                            MimeTypes.AUDIO_E_AC3, MimeTypes.AUDIO_E_AC3_JOC -> "E-AC3"
                            MimeTypes.AUDIO_AAC -> "AAC"
                            MimeTypes.AUDIO_DTS -> "DTS"
                            else -> mime.substringAfter("audio/").uppercase()
                        }
                        val isSupported = group.isTrackSupported(i)
                        val passthroughNote = when {
                            !isSupported -> " (Unsupported)"
                            isAc3 && !isAc3PassthroughSupported -> " (FFmpeg PCM)"
                            else -> ""
                        }
                        val label = buildString {
                            append(fmt.label ?: lang.uppercase())
                            if (codecTag.isNotBlank()) append(" • $codecTag")
                            if (fmt.channelCount > 0) append(" ${fmt.channelCount}ch")
                            append(passthroughNote)
                        }
                        audioList.add(
                            MediaTrackInfo(
                                id = "a_${groupIndex}_$i",
                                label = label,
                                language = lang,
                                codec = codecTag,
                                groupIndex = groupIndex,
                                trackIndex = i,
                                isSelected = group.isTrackSelected(i),
                                isAc3OrEac3 = isAc3,
                                isSupported = isSupported
                            )
                        )
                    }
                }

                C.TRACK_TYPE_TEXT -> {
                    for (i in 0 until group.length) {
                        val fmt = group.getTrackFormat(i)
                        val lang = fmt.language ?: "und"
                        val label = fmt.label ?: lang.uppercase()
                        subList.add(
                            MediaTrackInfo(
                                id = "s_${groupIndex}_$i",
                                label = label,
                                language = lang,
                                codec = fmt.sampleMimeType.orEmpty(),
                                groupIndex = groupIndex,
                                trackIndex = i,
                                isSelected = group.isTrackSelected(i)
                            )
                        )
                    }
                }
            }
        }

        _state.update { prev ->
            prev.copy(
                hdrType = detectedHdr,
                videoCodec = detectedCodec,
                isHevcStream = hevcDetected,
                showHevcSoftwareFallbackWarning = hevcDetected && !isHardwareHevcSupported,
                bitrateKbps = if (prev.bitrateKbps == 0L && formatBitrateKbps > 0L) formatBitrateKbps else prev.bitrateKbps,
                audioTracks = audioList,
                subtitleTracks = subList
            )
        }
    }

    private fun detectHdrBadge(colorInfo: ColorInfo?, mime: String, codecs: String?): HdrBadgeType {
        if (mime.equals(MimeTypes.VIDEO_DOLBY_VISION, ignoreCase = true) ||
            codecs.orEmpty().startsWith("dvhe", ignoreCase = true) ||
            codecs.orEmpty().startsWith("dvh1", ignoreCase = true)
        ) {
            return HdrBadgeType.DOLBY_VISION
        }
        if (colorInfo == null || !ColorInfo.isTransferHdr(colorInfo)) {
            return HdrBadgeType.NONE
        }
        return when (colorInfo.colorTransfer) {
            C.COLOR_TRANSFER_HLG -> HdrBadgeType.HLG
            C.COLOR_TRANSFER_ST2084 -> if (colorInfo.hdrStaticInfo != null) HdrBadgeType.HDR10_PLUS else HdrBadgeType.HDR10
            else -> HdrBadgeType.HDR10
        }
    }

    fun prepareAndPlay(
        streamUrl: String,
        startPositionMs: Long = 0L,
        qualityPreset: VideoQualityPreset = VideoQualityPreset.AUTO
    ) {
        if (isReleased) return
        val trimmedUrl = streamUrl.trim()
        if (trimmedUrl.isBlank()) {
            _state.update {
                it.copy(
                    isBuffering = false,
                    isPlaying = false,
                    errorMessage = "Empty stream URL"
                )
            }
            return
        }
        try {
            applyQualityPreset(qualityPreset)
            _state.update {
                it.copy(
                    isBuffering = true,
                    errorMessage = null,
                    playbackEnded = false,
                    hdrType = HdrBadgeType.NONE
                )
            }
            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(trimmedUrl))
                .build()
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            if (startPositionMs > 2_000L) {
                exoPlayer.seekTo(startPositionMs)
            }
            exoPlayer.playWhenReady = true
        } catch (t: Throwable) {
            _state.update {
                it.copy(
                    isBuffering = false,
                    isPlaying = false,
                    errorMessage = t.localizedMessage ?: "Failed to load stream"
                )
            }
        }
    }

    fun stopAndClear() {
        if (isReleased) return
        try {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            _state.update {
                it.copy(
                    isBuffering = false,
                    isPlaying = false,
                    errorMessage = null
                )
            }
        } catch (_: Throwable) {
        }
    }

    fun applyQualityPreset(preset: VideoQualityPreset) {
        if (isReleased) return
        try {
            val builder = trackSelector.buildUponParameters()
            if (preset.maxHeightPx == null) {
                builder.clearVideoSizeConstraints()
            } else {
                val maxWidth = when (preset) {
                    VideoQualityPreset.HD_720 -> 1280
                    VideoQualityPreset.FHD_1080 -> 1920
                    VideoQualityPreset.UHD_4K -> 3840
                    VideoQualityPreset.AUTO -> Int.MAX_VALUE
                }
                builder.setMaxVideoSize(maxWidth, preset.maxHeightPx)
            }
            trackSelector.setParameters(builder)
            _state.update { it.copy(selectedQuality = preset) }
        } catch (_: Throwable) {
        }
    }

    fun selectAudioTrack(track: MediaTrackInfo) {
        if (isReleased) return
        try {
            val groups = exoPlayer.currentTracks.groups
            val group = groups.getOrNull(track.groupIndex) ?: return
            if (!group.isTrackSupported(track.trackIndex)) return
            val params = trackSelector.buildUponParameters()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, track.trackIndex))
                .build()
            trackSelector.setParameters(params)
        } catch (_: Throwable) {
        }
    }

    fun selectSubtitleTrack(track: MediaTrackInfo?) {
        if (isReleased) return
        try {
            val builder = trackSelector.buildUponParameters()
            if (track == null) {
                builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            } else {
                val group = exoPlayer.currentTracks.groups.getOrNull(track.groupIndex) ?: return
                builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                builder.setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, track.trackIndex))
            }
            trackSelector.setParameters(builder.build())
        } catch (_: Throwable) {
        }
    }

    fun togglePlayPause() {
        if (isReleased) return
        try {
            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
        } catch (_: Throwable) {
        }
    }

    fun seekTo(positionMs: Long) {
        if (isReleased) return
        try {
            exoPlayer.seekTo(positionMs.coerceAtLeast(0L))
            syncPosition()
        } catch (_: Throwable) {
        }
    }

    fun seekBy(deltaMs: Long) {
        if (isReleased) return
        try {
            val dur = exoPlayer.duration
            val maxBound = if (dur > 0L) dur else Long.MAX_VALUE
            val target = (exoPlayer.currentPosition + deltaMs).coerceIn(0L, maxBound)
            exoPlayer.seekTo(target)
            syncPosition()
        } catch (_: Throwable) {
        }
    }

    fun setVolume(volume: Float) {
        if (isReleased) return
        try {
            val clamped = volume.coerceIn(0f, 1f)
            exoPlayer.volume = clamped
            _state.update { it.copy(volume = clamped) }
        } catch (_: Throwable) {
        }
    }

    fun syncPosition() {
        if (isReleased) return
        try {
            val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
            val dur = exoPlayer.duration.coerceAtLeast(0L)
            val estimatedKbps = (bandwidthMeter.bitrateEstimate / 1000L).coerceAtLeast(0L)
            _state.update {
                it.copy(
                    positionMs = pos,
                    durationMs = dur,
                    bitrateKbps = if (estimatedKbps > 0L) estimatedKbps else it.bitrateKbps
                )
            }
        } catch (_: Throwable) {
        }
    }

    fun release() {
        if (isReleased) return
        isReleased = true
        try {
            exoPlayer.clearVideoSurface()
            exoPlayer.release()
        } catch (_: Throwable) {
        }
    }

    companion object {
        fun probeHardwareHevcDecoder(): Boolean {
            return try {
                val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
                codecList.codecInfos.any { info ->
                    if (info.isEncoder) return@any false
                    val isHardware = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        info.isHardwareAccelerated
                    } else {
                        val name = info.name.lowercase()
                        !name.startsWith("omx.google.") && !name.startsWith("c2.android.")
                    }
                    isHardware && info.supportedTypes.any { it.equals(MediaFormatMime.HEVC, ignoreCase = true) }
                }
            } catch (_: Exception) {
                false
            }
        }

        fun probeAc3Passthrough(context: Context): Boolean {
            return try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                    ?: return false
                val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                devices.any { device ->
                    val typeOk = device.type == AudioDeviceInfo.TYPE_HDMI ||
                        device.type == AudioDeviceInfo.TYPE_HDMI_ARC ||
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && device.type == AudioDeviceInfo.TYPE_HDMI_EARC) ||
                        device.type == AudioDeviceInfo.TYPE_LINE_DIGITAL
                    val encodings = device.encodings
                    typeOk && (
                        encodings.contains(AudioFormat.ENCODING_AC3) ||
                            encodings.contains(AudioFormat.ENCODING_E_AC3)
                        )
                }
            } catch (_: Exception) {
                false
            }
        }
    }

    private object MediaFormatMime {
        const val HEVC = "video/hevc"
    }
}
