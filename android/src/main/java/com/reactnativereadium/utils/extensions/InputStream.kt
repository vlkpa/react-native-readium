package com.reactnativereadium.utils.extensions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.extensions.tryOrNull
import java.io.File
import java.io.InputStream
import java.util.*


suspend fun InputStream.toFile(file: File) {
  withContext(Dispatchers.IO) {
    use { input ->
      file.outputStream().use { input.copyTo(it) }
    }
  }
}

suspend fun InputStream.copyToTempFile(dir: File): File? = tryOrNull {
  val filename = UUID.randomUUID().toString()
  File(dir, filename)
    .also { toFile(it) }
}

