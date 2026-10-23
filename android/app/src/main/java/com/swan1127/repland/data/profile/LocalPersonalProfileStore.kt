package com.swan1127.repland.data.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.AtomicFile
import com.swan1127.repland.domain.model.PersonalProfileName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class PersonalProfile(val username: String = PersonalProfileName.Default, val avatar: Bitmap? = null)

/** A bounded private thumbnail; no library access or long-lived URI grant is retained. */
class LocalPersonalProfileStore(context: Context, storageName: String = "personal-profile") {
    init { require(storageName.matches(Regex("[a-zA-Z0-9_-]{1,80}"))) }
    private val app = context.applicationContext
    private val preferences = app.getSharedPreferences(storageName, Context.MODE_PRIVATE)
    private val avatarFile = AtomicFile(File(app.filesDir, "$storageName-avatar.jpg"))
    private val mutex = Mutex()
    private val mutableProfile = MutableStateFlow<PersonalProfile?>(null)
    val profile = mutableProfile.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        mutex.withLock { if (mutableProfile.value == null) mutableProfile.value = read() }
    }

    private fun read() = PersonalProfile(
        preferences.getString("username", null)?.takeIf { PersonalProfileName.error(it) == null }
            ?.let(PersonalProfileName::normalized) ?: PersonalProfileName.Default,
        runCatching { avatarFile.openRead().use { BitmapFactory.decodeStream(it) } }.getOrNull(),
    )

    suspend fun saveUsername(value: String) = withContext(Dispatchers.IO) {
        val normalized = PersonalProfileName.normalized(value)
        mutex.withLock {
            val previous = mutableProfile.value ?: read()
            if (!preferences.edit().putString("username", normalized).commit()) throw IOException("Username not saved")
            mutableProfile.value = previous.copy(username = normalized)
        }
    }

    suspend fun saveAvatar(uri: Uri) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val previous = mutableProfile.value ?: read()
            val decoded = decode(uri)
            val edge = min(decoded.width, decoded.height)
            val cropped = Bitmap.createBitmap(decoded, (decoded.width - edge) / 2, (decoded.height - edge) / 2, edge, edge)
            val size = min(edge, AvatarSize)
            val thumbnail = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            try {
                Canvas(thumbnail).apply {
                    drawColor(Color.WHITE)
                    drawBitmap(cropped, null, android.graphics.Rect(0, 0, size, size), android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))
                }
                val output = avatarFile.startWrite()
                try {
                    if (!thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, output)) throw IOException("Avatar not encoded")
                    avatarFile.finishWrite(output)
                } catch (failure: Exception) {
                    avatarFile.failWrite(output)
                    throw failure
                }
                mutableProfile.value = previous.copy(avatar = thumbnail)
            } catch (failure: Exception) {
                thumbnail.recycle()
                throw failure
            } finally {
                if (cropped !== decoded) cropped.recycle()
                decoded.recycle()
            }
        }
    }

    suspend fun removeAvatar() = withContext(Dispatchers.IO) {
        mutex.withLock {
            val previous = mutableProfile.value ?: read()
            avatarFile.delete()
            if (avatarFile.baseFile.exists()) throw IOException("Avatar not removed")
            mutableProfile.value = previous.copy(avatar = null)
        }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!preferences.edit().clear().commit()) throw IOException("Profile not cleared")
            avatarFile.delete()
            if (avatarFile.baseFile.exists()) throw IOException("Avatar not cleared")
            mutableProfile.value = PersonalProfile()
        }
    }

    private fun decode(uri: Uri): Bitmap {
        if (Build.VERSION.SDK_INT >= 28) {
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(app.contentResolver, uri)) { decoder, info, _ ->
                val factor = min(1f, DecodeSize.toFloat() / max(info.size.width, info.size.height))
                decoder.setTargetSize(max(1, (info.size.width * factor).roundToInt()), max(1, (info.size.height * factor).roundToInt()))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        app.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Not an image")
        var sample = 1
        // BitmapFactory only samples by powers of two. Round the sample down,
        // then scale precisely, so older devices retain the same crop resolution
        // as ImageDecoder instead of turning a 1600x900 source into 400x225.
        while (max(bounds.outWidth, bounds.outHeight) / sample / 2 >= DecodeSize) sample *= 2
        val bitmap = app.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: throw IOException("Image not readable")
        val orientation = runCatching { app.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } }.getOrNull()
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(-90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
            }
        }
        val oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            .also { if (it !== bitmap) bitmap.recycle() }
        val factor = min(1f, DecodeSize.toFloat() / max(oriented.width, oriented.height))
        return Bitmap.createScaledBitmap(oriented, max(1, (oriented.width * factor).roundToInt()),
            max(1, (oriented.height * factor).roundToInt()), true)
            .also { if (it !== oriented) oriented.recycle() }
    }

    companion object {
        const val AvatarSize = 384
        private const val DecodeSize = 768
    }
}
