// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.util

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Builds a QR bitmap for [text].
 *
 * Error correction is level M: enough redundancy to survive a phone camera at an angle without
 * inflating the symbol so much that a long subscription URL overflows the module count.
 *
 * [sizePx] is the requested edge length; the encoder rounds up to whole modules, so the real
 * bitmap can be slightly larger. Returns `null` for blank input or an encoding failure rather than
 * throwing — a share sheet with no QR is better than a crash.
 */
fun generateQrBitmap(text: String, sizePx: Int = 640): ImageBitmap? {
    if (text.isBlank()) return null

    return runCatching {
        val matrix = QRCodeWriter().encode(
            text,
            BarcodeFormat.QR_CODE,
            sizePx,
            sizePx,
            mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                // No extra quiet zone: the caller pads the surface, and a margin baked into the
                // bitmap would make the code look off-centre inside its own card.
                EncodeHintType.MARGIN to 0,
                EncodeHintType.CHARACTER_SET to "UTF-8",
            ),
        )

        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                pixels[rowOffset + x] = if (matrix[x, y]) BLACK else WHITE
            }
        }

        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            .apply { setPixels(pixels, 0, width, 0, 0, width, height) }
            .asImageBitmap()
    }.getOrNull()
}

private const val BLACK = 0xFF000000.toInt()
private const val WHITE = 0xFFFFFFFF.toInt()

/**
 * The link a shared QR should carry.
 *
 * This is the app's own install-config scheme rather than the subscription URL: scanning it with
 * another install of the app imports the profile directly, which is the point of sharing one.
 */
fun profileShareLink(url: String): String =
    "clashneo://install-config?url=" + java.net.URLEncoder.encode(url, "UTF-8")