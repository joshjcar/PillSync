package com.tally.app.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi

/**
 * Keeps a silent "safety copy" of the user's data in the public Downloads/PillSync folder so that
 * reminders survive uninstalling the app or moving to a new phone — the one thing an in-place
 * update can't cover. The file lives entirely on the device and is never uploaded anywhere.
 *
 * Only runs on Android 10+ (API 29), where writing to Downloads needs no storage permission.
 * Restore also looks in the legacy Downloads/Tally location so older backups still work.
 */
object AutoBackup {
    private const val TAG = "AutoBackup"

    private const val FILE_NAME = "pillsync-backup.json"
    private const val REL_PATH = "Download/PillSync/"

    private const val LEGACY_FILE_NAME = "tally-backup.json"
    private const val LEGACY_REL_PATH = "Download/Tally/"

    data class Found(val uri: Uri, val savedAt: Long)

    /** Write a fresh safety copy. Always on; only skipped on pre-Android-10 (needs no permission there). */
    suspend fun maybeWrite(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        try {
            val json = BackupManager.exportJson(context)
            writeToDownloads(context, json.toByteArray())
            Log.i(TAG, "Safety backup updated")
        } catch (t: Throwable) {
            Log.w(TAG, "Auto-backup failed (non-fatal)", t)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun writeToDownloads(context: Context, bytes: ByteArray) {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val existing = findUri(context, FILE_NAME, REL_PATH)
        if (existing != null) {
            resolver.openOutputStream(existing, "wt")?.use { it.write(bytes) }
            return
        }
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, FILE_NAME)
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/PillSync/")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: return
        resolver.openOutputStream(uri)?.use { it.write(bytes) }
        resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun findUri(context: Context, name: String, relPath: String): Uri? {
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val projection = arrayOf(MediaStore.Downloads._ID)
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ? AND ${MediaStore.Downloads.RELATIVE_PATH} = ?"
        val args = arrayOf(name, relPath)
        return try {
            context.contentResolver.query(collection, projection, selection, args, null)?.use { c ->
                if (c.moveToFirst()) ContentUris.withAppendedId(collection, c.getLong(0)) else null
            }
        } catch (t: Throwable) {
            Log.w(TAG, "findUri failed", t); null
        }
    }

    /** Best-effort: locate a readable safety copy for the first-run restore prompt. May be null. */
    fun find(context: Context): Found? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val uri = findUri(context, FILE_NAME, REL_PATH)
            ?: findUri(context, LEGACY_FILE_NAME, LEGACY_REL_PATH)
            ?: return null
        return try {
            context.contentResolver.openInputStream(uri)?.use { } // confirm readable
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            var modified = System.currentTimeMillis()
            context.contentResolver.query(
                collection, arrayOf(MediaStore.Downloads.DATE_MODIFIED),
                "${MediaStore.Downloads._ID} = ?", arrayOf(ContentUris.parseId(uri).toString()), null
            )?.use { c -> if (c.moveToFirst()) modified = c.getLong(0) * 1000L }
            Found(uri, modified)
        } catch (t: Throwable) {
            null
        }
    }

    fun readText(context: Context, uri: Uri): String =
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("Could not read backup")
}
