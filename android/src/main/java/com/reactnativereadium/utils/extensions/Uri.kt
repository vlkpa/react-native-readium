package com.reactnativereadium.utils.extensions

import android.content.Context
import android.net.Uri
import com.reactnativereadium.utils.ContentResolverUtil
import java.io.File
import java.util.*
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.mediatype.MediaType

suspend fun Uri.copyToTempFile(context: Context, dir: File): Try<File, Exception> =
  try {
    val filename = UUID.randomUUID().toString()
    val mediaType = MediaType.ofUri(this, context.contentResolver)
    val file = File(dir, "$filename.${mediaType?.fileExtension ?: "tmp"}")
    ContentResolverUtil.getContentInputStream(context, this, file)
    Try.success(file)
  } catch (e: Exception) {
    Try.failure(e)
  }

