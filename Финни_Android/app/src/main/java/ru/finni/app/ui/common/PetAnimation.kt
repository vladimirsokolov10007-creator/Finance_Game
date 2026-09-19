package ru.finni.app.ui.common

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ru.finni.app.R

/**
 * v0.5: анимированный питомец — зацикленное видео вместо эмодзи.
 * Стандартный MediaPlayer + TextureView, без внешних зависимостей.
 * Три персонажа: 0 — Девочка (худи), 1 — Девочка (лапки), 2 — Мальчик.
 * Ролики девочки-худи и мальчика вертикальные (~2:3), девочки-лапки — квадратные (~1:1).
 */
private val PET_RES = listOf(
    R.raw.finni_girl1,   // Девочка · худи (вертикальный ~2:3)
    R.raw.finni_girl2,   // Девочка · лапки (квадратный ~1:1)
    R.raw.finni_boy,     // Мальчик (вертикальный ~2:3)
)
private val PET_ASPECT = listOf(0.67f, 1.0f, 0.67f)

@Composable
fun PetAnimation(character: Int = 0, modifier: Modifier = Modifier) {
    val resId = PET_RES.getOrElse(character) { PET_RES.first() }
    val aspect = PET_ASPECT.getOrElse(character) { PET_ASPECT.first() }
    key(resId) {
        val mediaPlayer = remember { MediaPlayer() }
        DisposableEffect(Unit) {
            onDispose { runCatching { mediaPlayer.release() } }
        }
        AndroidView(
            modifier = modifier
                .height(260.dp)
                .aspectRatio(aspect)
                .clip(RoundedCornerShape(20.dp)),
            factory = { context ->
                TextureView(context).apply {
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        private var surface: Surface? = null

                        override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                            surface = Surface(st)
                            runCatching {
                                mediaPlayer.reset()
                                val afd = context.resources.openRawResourceFd(resId)
                                mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                                afd.close()
                                mediaPlayer.setSurface(surface)
                                mediaPlayer.isLooping = true
                                mediaPlayer.setOnPreparedListener { it.start() }
                                mediaPlayer.prepareAsync()
                            }
                        }

                        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) = Unit

                        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                            runCatching { mediaPlayer.setSurface(null) }
                            surface?.release()
                            surface = null
                            return true
                        }

                        override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
                    }
                }
            },
        )
    }
}
