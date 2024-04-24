package com.reactnativereadium.reader

import android.annotation.SuppressLint
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelStore
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.util.RNLog
import com.reactnativereadium.Readium
import com.reactnativereadium.utils.LinkOrLocator
import com.reactnativereadium.utils.extensions.copyToTempFile
import org.readium.adapters.pdfium.document.PdfiumDocumentFactory
import java.io.File
import java.io.IOException
import java.net.ServerSocket
import java.net.URL
import org.readium.r2.shared.extensions.mediaType
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.Injectable
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.asset.FileAsset
import org.readium.r2.shared.publication.Publication
import org.readium.r2.streamer.server.Server
import org.readium.r2.streamer.Streamer
import org.readium.r2.shared.publication.services.isRestricted
import org.readium.r2.shared.publication.services.protectionError
import org.readium.r2.lcp.LcpService
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.mediatype.MediaType
import android.content.*
import android.net.Uri

class ReaderService(
  private val reactContext: ReactApplicationContext
) {
  object CancellationException : Exception()

  private var readium: Readium

  // see R2App.onCreate
  private var server: Server
  // val channel = EventChannel(Channel<Event>(Channel.BUFFERED), viewModelScope)
  private var store = ViewModelStore()

  companion object {
    @SuppressLint("StaticFieldLeak")
    lateinit var server: Server
      private set

    lateinit var R2DIRECTORY: String
      private set

    var isServerStarted = false
      private set
  }

  init {
    val s = ServerSocket(0)
    s.close()
    server = Server(s.localPort, reactContext)
    readium = Readium(reactContext)
    this.startServer()
  }

  fun locatorFromLinkOrLocator(
    location: LinkOrLocator?,
    publication: Publication,
  ): Locator? {

    if (location == null) return null

    when (location) {
      is LinkOrLocator.Link -> {
        return publication.locatorFromLink(location.link)
      }
      is LinkOrLocator.Locator -> {
        return location.locator
      }
    }

    return null
  }


  suspend fun openPublication(
    fileName: String,
    passphrase: String?,
    initialLocation: LinkOrLocator?,
    callback: suspend (fragment: BaseReaderFragment) -> Unit
  ) {
    val tempFile = File(fileName)
    val sourceMediaType = tempFile.mediaType()
    val publicationAsset: FileAsset =
      if (sourceMediaType != MediaType.LCP_LICENSE_DOCUMENT)
        FileAsset(tempFile, sourceMediaType)
      else {
        readium.lcpService
          .flatMap { it.acquirePublication(tempFile) }
          .fold(
            {
              val mediaType =
                MediaType.of(fileExtension = File(it.suggestedFilename).extension)
              FileAsset(it.localFile, mediaType)
            },
            {
              RNLog.w(reactContext, "Error - lcpService $it")
              tryOrNull { tempFile.delete() }
              return println("Error - lcpService $it")
            }
          )
      }


    readium.streamer.open(
      publicationAsset,
      credentials = passphrase,
      allowUserInteraction = true,
      sender = reactContext.currentActivity
    )
      .onSuccess { it ->

        if (it.isRestricted) {
          RNLog.w(reactContext, "protectionError: " + (it.protectionError ?: CancellationException).toString())
        } else {
          val url = prepareToServe(it)
          if (url != null) {
            val locator = locatorFromLinkOrLocator(initialLocation, it)
  //          val readerFragment = EpubReaderFragment.newInstance(url)
  //          readerFragment.initFactory(it, locator)
  //          callback.invoke(readerFragment)
            createReaderFragment(url, it, locator)?.let {
              callback.invoke(it)
            }
          }
        }
      }
      .onFailure {
        tryOrNull { publicationAsset.file.delete() }
        RNLog.w(reactContext, "Error executing ReaderService.openPublication")
        // TODO: implement failure event
      }
  }

  private fun createReaderFragment(
    baseURL: URL,
    publication: Publication,
    initialLocation: Locator?
  ): BaseReaderFragment? {
    return when {
      publication.conformsTo(Publication.Profile.EPUB) -> {
        val readerFragment = EpubReaderFragment.newInstance(baseURL);
        readerFragment.initFactory(publication, initialLocation)
        return readerFragment
      }
      publication.conformsTo(Publication.Profile.PDF) -> {
        val readerFragment = PdfReaderFragment.newInstance();
        readerFragment.initFactory(publication, initialLocation)
        return readerFragment
      }
      else ->
        // The Activity should stop as soon as possible because readerData are fake.
        null
    }
  }

  private fun prepareToServe(publication: Publication): URL? {
    val userProperties =
      reactContext.filesDir.path + "/" + Injectable.Style.rawValue + "/UserProperties.json"
    return server.addPublication(
      publication,
      userPropertiesFile = File(userProperties)
    )
  }

  private fun startServer() {
    if (!server.isAlive) {
      try {
        server.start()
      } catch (e: IOException) {
        RNLog.e(reactContext, "Unable to start the Readium server.")
      }
      if (server.isAlive) {
        // // Add your own resources here
        // server.loadCustomResource(assets.open("scripts/test.js"), "test.js")
        // server.loadCustomResource(assets.open("styles/test.css"), "test.css")
        // server.loadCustomFont(assets.open("fonts/test.otf"), applicationContext, "test.otf")

        isServerStarted = true
      }
    }
  }

  sealed class Event {

    class ImportPublicationFailed(val errorMessage: String?) : Event()

    object UnableToMovePublication : Event()

    object ImportPublicationSuccess : Event()

    object ImportDatabaseFailed : Event()

    class OpenBookError(val errorMessage: String?) : Event()
  }
}
