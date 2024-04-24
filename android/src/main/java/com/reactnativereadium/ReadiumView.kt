package com.reactnativereadium

import android.view.Choreographer
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import com.facebook.react.bridge.Arguments
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.events.RCTEventEmitter
import com.reactnativereadium.reader.BaseReaderFragment
import com.reactnativereadium.reader.EpubReaderFragment
import com.reactnativereadium.reader.ReaderViewModel
import com.reactnativereadium.utils.Dimensions
import com.reactnativereadium.utils.File
import com.reactnativereadium.utils.LinkOrLocator
import org.readium.r2.shared.extensions.toMap
import com.facebook.react.bridge.WritableMap
import kotlinx.coroutines.*
import org.readium.r2.shared.publication.Locator

class ReadiumView(
  val reactContext: ThemedReactContext
) : FrameLayout(reactContext) {
  var dimensions: Dimensions = Dimensions(0,0)
  var file: File? = null
  var fragment: BaseReaderFragment? = null
  var isViewInitialized: Boolean = false
  var lateInitSettings: Map<String, Any>? = null
  private val viewScope = CoroutineScope(Dispatchers.Main)

  fun updateLocation(location: LinkOrLocator) : Boolean {
    if (fragment == null) {
      return false
    } else {
      return this.fragment!!.go(location, false)
    }
  }

  fun search(query: String) : Boolean {
       if (fragment == null) {
      return false
    } else {
      val id = this.id.toInt()
      val module = reactContext.getJSModule(RCTEventEmitter::class.java)
      val map: WritableMap = Arguments.createMap()
      viewScope.launch {
        try {
          val iterator = fragment!!.search(query)
          val locatorList = mutableListOf<Locator>()
          iterator?.forEach { result ->
            locatorList.addAll(result.locators)
          }
          val locatorMap = locatorList.map { it.toJSON().toMap() }
          val payload = Arguments.makeNativeMap(mapOf("locators" to locatorMap))
          module.receiveEvent(id, ReadiumViewManager.ON_SEARCH, payload)
        } catch (e: Exception) {
          module.receiveEvent(id, ReadiumViewManager.ON_SEARCH, map)
        }
      }
      return true
    }
  }

  fun updateSettingsFromMap(map: Map<String, Any>?) {
    if (map == null) {
      return
    } else if (fragment == null) {
      lateInitSettings = map
      return
    }

    if (fragment is EpubReaderFragment) {
      (fragment as EpubReaderFragment).updateSettingsFromMap(map)
    }

    lateInitSettings = null
  }

  fun addFragment(frag: BaseReaderFragment) {
    fragment = frag
    setupLayout()
    updateSettingsFromMap(lateInitSettings)
    val activity: FragmentActivity? = reactContext.currentActivity as FragmentActivity?
    activity!!.supportFragmentManager
      .beginTransaction()
      .replace(this.id, frag, this.id.toString())
      .commit()

    val module = reactContext.getJSModule(RCTEventEmitter::class.java)
    // subscribe to reader events
    frag.channel.receive(frag) { event ->
      when (event) {
        is ReaderViewModel.Event.LocatorUpdate -> {
          val json = event.locator.toJSON()
          val payload = Arguments.makeNativeMap(json.toMap())
          module.receiveEvent(
            this.id.toInt(),
            ReadiumViewManager.ON_LOCATION_CHANGE,
            payload
          )
        }
        is ReaderViewModel.Event.TableOfContentsLoaded -> {
          val map = event.toc.map { it.toJSON().toMap() }
          val payload = Arguments.makeNativeMap(mapOf("toc" to map))
          module.receiveEvent(
            this.id.toInt(),
            ReadiumViewManager.ON_TABLE_OF_CONTENTS,
            payload
          )
        }

        is ReaderViewModel.Event.Failure -> TODO()
        ReaderViewModel.Event.OpenDrmManagementRequested -> TODO()
        ReaderViewModel.Event.OpenOutlineRequested -> TODO()
        ReaderViewModel.Event.StartNewSearch -> TODO()
        ReaderViewModel.Event.OnTap -> {
          val module = reactContext.getJSModule(RCTEventEmitter::class.java)
          module.receiveEvent(
            this.id.toInt(),
            ReadiumViewManager.ON_PRESS_CONTENT,
            null
          )
        }
      }
    }
  }

  private fun setupLayout() {
    Choreographer.getInstance().postFrameCallback(object : Choreographer.FrameCallback {
      override fun doFrame(frameTimeNanos: Long) {
        manuallyLayoutChildren()
        this@ReadiumView.viewTreeObserver.dispatchOnGlobalLayout()
        Choreographer.getInstance().postFrameCallback(this)
      }
    })
  }

  /**
   * Layout all children properly
   */
  private fun manuallyLayoutChildren() {
    // propWidth and propHeight coming from react-native props
    val width = dimensions.width
    val height = dimensions.height
    this.measure(
      MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
      MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
    this.layout(0, 0, width, height)
  }
}
