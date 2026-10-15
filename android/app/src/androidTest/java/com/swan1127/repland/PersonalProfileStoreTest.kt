package com.swan1127.repland

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.swan1127.repland.data.profile.LocalPersonalProfileStore
import com.swan1127.repland.data.room.ReplandDatabase
import com.swan1127.repland.data.room.RoomDataManagementRepository
import com.swan1127.repland.domain.model.PersonalProfileName
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class PersonalProfileStoreTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun namespace() = "profile-test-${UUID.randomUUID()}"
    private fun source(name: String): File {
        val bitmap = Bitmap.createBitmap(1600, 900, Bitmap.Config.ARGB_8888)
        val file = File(context.cacheDir, "$name.jpg")
        Canvas(bitmap).apply {
            drawColor(Color.RED)
            drawRect(0f, 450f, 1600f, 900f, Paint().apply { color = Color.BLUE })
        }
        file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
        bitmap.recycle()
        return file
    }

    @Test fun username_persists_and_invalid_write_preserves_the_saved_value() = runBlocking {
        val key = namespace(); val store = LocalPersonalProfileStore(context, key)
        try {
            store.load(); store.saveUsername("  小林的日程  ")
            assertTrue(runCatching { store.saveUsername(" ") }.isFailure)
            val reopened = LocalPersonalProfileStore(context, key); reopened.load()
            assertEquals("小林的日程", reopened.profile.value!!.username)
        } finally { store.clear() }
    }

    @Test fun thumbnail_is_bounded_and_survives_removal_of_the_source() = runBlocking {
        val key = namespace(); val store = LocalPersonalProfileStore(context, key); val file = source(key)
        try {
            store.saveAvatar(Uri.fromFile(file))
            assertEquals(384, store.profile.value!!.avatar!!.width)
            assertEquals(384, store.profile.value!!.avatar!!.height)
            assertTrue(file.delete())
            val reopened = LocalPersonalProfileStore(context, key); reopened.load()
            assertNotNull(reopened.profile.value!!.avatar)
            assertTrue(File(context.filesDir, "$key-avatar.jpg").length() < 100_000)
        } finally { store.clear(); file.delete() }
    }

    @Test fun corrupt_replacement_keeps_the_previous_avatar_and_name() = runBlocking {
        val key = namespace(); val store = LocalPersonalProfileStore(context, key); val file = source(key)
        val broken = File(context.cacheDir, "$key-broken.jpg").apply { writeText("not an image") }
        try {
            store.saveUsername("保留的名字"); store.saveAvatar(Uri.fromFile(file))
            val old = store.profile.value!!.avatar
            val bytes = File(context.filesDir, "$key-avatar.jpg").readBytes()
            assertTrue(runCatching { store.saveAvatar(Uri.fromFile(broken)) }.isFailure)
            assertSame(old, store.profile.value!!.avatar)
            assertArrayEquals(bytes, File(context.filesDir, "$key-avatar.jpg").readBytes())
            assertEquals("保留的名字", store.profile.value!!.username)
        } finally { store.clear(); file.delete(); broken.delete() }
    }

    @Test fun camera_orientation_is_applied_before_the_center_crop() = runBlocking {
        val key = namespace(); val store = LocalPersonalProfileStore(context, key); val file = source(key)
        try {
            ExifInterface(file.absolutePath).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString()); saveAttributes()
            }
            store.saveAvatar(Uri.fromFile(file))
            val avatar = store.profile.value!!.avatar!!
            assertTrue(Color.blue(avatar.getPixel(20, 192)) > 200)
            assertTrue(Color.red(avatar.getPixel(364, 192)) > 200)
        } finally { store.clear(); file.delete() }
    }

    @Test fun reset_avatar_keeps_name_and_clear_all_data_resets_the_whole_profile() = runBlocking {
        val key = namespace(); val store = LocalPersonalProfileStore(context, key); val file = source(key)
        val db = Room.inMemoryDatabaseBuilder(context, ReplandDatabase::class.java).build()
        try {
            store.saveUsername("我的名字"); store.saveAvatar(Uri.fromFile(file)); store.removeAvatar()
            assertNull(store.profile.value!!.avatar); assertEquals("我的名字", store.profile.value!!.username)
            store.saveAvatar(Uri.fromFile(file))
            RoomDataManagementRepository(db, store::clear).clearAllLocalData()
            val reopened = LocalPersonalProfileStore(context, key); reopened.load()
            assertEquals(PersonalProfileName.Default, reopened.profile.value!!.username)
            assertNull(reopened.profile.value!!.avatar)
            assertFalse(File(context.filesDir, "$key-avatar.jpg").exists())
        } finally { db.close(); store.clear(); file.delete() }
    }
}
