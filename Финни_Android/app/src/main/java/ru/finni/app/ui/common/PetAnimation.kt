package ru.finni.app.ui.common

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ru.finni.app.R

/**
 * v0.6: анимированный питомец — animated WebP с прозрачным фоном (ImageDecoder, API 28+).
 * Три персонажа: 0 — Девочка (худи), 1 — Девочка (лапки), 2 — Мальчик.
 * Девочка-худи и мальчик вертикальные (~2:3), девочка-лапки квадратная (~1:1).
 *
 * ТЗ п. 3.1 требует Android 8.0 (API 26): на API 26–27, где ImageDecoder недоступен,
 * показываем статичный первый кадр анимации (BitmapFactory умеет декодировать WebP).
 */
private val PET_RES = listOf(
    R.raw.finni_girl1,   // Девочка · худи (вертикальный ~2:3)
    R.raw.finni_girl2,   // Девочка · лапки (квадратный ~1:1)
    R.raw.finni_boy,     // Мальчик (вертикальный ~2:3)
)
private val PET_ASPECT = listOf(0.67f, 1.0f, 0.67f)

@Composable
fun PetAnimation(character: Int = 0, modifier: Modifier = Modifier, animate: Boolean = true) {
    val resId = PET_RES.getOrElse(character) { PET_RES.first() }
    val aspect = PET_ASPECT.getOrElse(character) { PET_ASPECT.first() }
    val context = LocalContext.current

    // API 26–27 или принудительно выключенная анимация (доступность, ТЗ п. 3.6):
    // статичный первый кадр WebP
    if (Build.VERSION.SDK_INT < 28 || !animate) {
        val frame = remember(resId, animate) {
            runCatching {
                BitmapFactory.decodeResource(context.resources, resId)?.asImageBitmap()
            }.getOrNull()
        }
        if (frame != null) {
            Image(
                bitmap = frame,
                contentDescription = null,
                modifier = modifier
                    .height(260.dp)
                    .aspectRatio(aspect),
            )
        }
        return
    }

    key(resId) {
        val drawable = remember {
            runCatching {
                val src = ImageDecoder.createSource(context.resources, resId)
                (ImageDecoder.decodeDrawable(src) as AnimatedImageDrawable).apply {
                    repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                    start()
                }
            }.getOrNull()
        }
        DisposableEffect(resId) {
            onDispose { (drawable as? AnimatedImageDrawable)?.stop() }
        }
        AndroidView(
            modifier = modifier
                .height(260.dp)
                .aspectRatio(aspect),
            factory = { imageViewContext ->
                ImageView(imageViewContext).apply { setImageDrawable(drawable) }
            },
            update = { it.setImageDrawable(drawable) },
        )
    }
}
