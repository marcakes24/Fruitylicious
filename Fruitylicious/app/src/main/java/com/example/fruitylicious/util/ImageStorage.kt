package com.example.fruitylicious.util

import android.content.Context
import android.net.Uri
import android.util.Base64
import java.io.File
import java.util.UUID

object ImageStorage {

    private const val ROOT_FOLDER = "fruitylicious/images"
    private const val BASE64_PREFIX = "base64:"

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

    fun imageFileToBase64(
        context: Context,
        relativePath: String?
    ): String? {
        if (relativePath.isNullOrBlank()) {
            return null
        }

        if (relativePath.startsWith(BASE64_PREFIX)) {
            return relativePath
        }

        return try {
            val file = getImageFile(
                context = context,
                relativePath = relativePath
            )

            if (!file.exists()) {
                return null
            }

            val encoded = Base64.encodeToString(
                file.readBytes(),
                Base64.NO_WRAP
            )

            "$BASE64_PREFIX$encoded"
        } catch (exception: Exception) {
            null
        }
    }

    fun saveBase64Image(
        context: Context,
        base64Value: String?,
        folder: String
    ): String? {
        if (base64Value.isNullOrBlank()) {
            return null
        }

        if (!base64Value.startsWith(BASE64_PREFIX)) {
            return base64Value
        }

        return try {
            val cleanBase64 = base64Value.removePrefix(BASE64_PREFIX)

            val directory = File(
                context.filesDir,
                "$ROOT_FOLDER/$folder"
            )

            if (!directory.exists()) {
                directory.mkdirs()
            }

            val fileName = "${UUID.randomUUID()}.jpg"
            val destinationFile = File(directory, fileName)

            val bytes = Base64.decode(
                cleanBase64,
                Base64.NO_WRAP
            )

            destinationFile.writeBytes(bytes)

            "$folder/$fileName"
        } catch (exception: Exception) {
            null
        }
    }

    fun isBase64Image(
        value: String?
    ): Boolean {
        return value?.startsWith(BASE64_PREFIX) == true
    }
}