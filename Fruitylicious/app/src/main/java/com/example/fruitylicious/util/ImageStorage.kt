package com.example.fruitylicious.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

object ImageStorage {

    private const val ROOT_FOLDER = "fruitylicious/images"

    fun saveImageFromUri(
        context: Context,
        sourceUri: Uri,
        folder: String
    ): String {
        val directory = File(
            context.filesDir,
            "$ROOT_FOLDER/$folder"
        )

        if (!directory.exists()) {
            directory.mkdirs()
        }

        val fileName = "${UUID.randomUUID()}.jpg"
        val destinationFile = File(directory, fileName)

        context.contentResolver.openInputStream(sourceUri).use { input ->
            destinationFile.outputStream().use { output ->
                requireNotNull(input) {
                    "Unable to open selected image."
                }.copyTo(output)
            }
        }

        return "$folder/$fileName"
    }

    fun getImageFile(
        context: Context,
        relativePath: String
    ): File {
        return File(
            context.filesDir,
            "$ROOT_FOLDER/$relativePath"
        )
    }
}