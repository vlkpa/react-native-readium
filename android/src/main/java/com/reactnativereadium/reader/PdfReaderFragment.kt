/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package com.reactnativereadium.reader

import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.commitNow
import androidx.lifecycle.ViewModelProvider
import com.facebook.react.util.RNLog
import com.reactnativereadium.R
import org.readium.adapters.pdfium.navigator.PdfiumEngineProvider
import org.readium.adapters.pdfium.navigator.PdfiumPreferences
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.pdf.PdfNavigatorFactory
import org.readium.r2.navigator.pdf.PdfNavigatorFragment
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

@OptIn(ExperimentalReadiumApi::class)
class PdfReaderFragment : VisualReaderFragment(), PdfNavigatorFragment.Listener {

  override lateinit var model: ReaderViewModel
  override lateinit var navigator: Navigator
  private lateinit var publication: Publication
  private lateinit var factory: ReaderViewModel.Factory
  val pdfEngine = PdfiumEngineProvider()


  fun initFactory(
    publication: Publication,
    initialLocation: Locator?
  ) {
    factory = ReaderViewModel.Factory(
      publication,
      initialLocation
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    ViewModelProvider(this, factory)
      .get(ReaderViewModel::class.java)
      .let {
        model = it
        publication = it.publication
      }

    val navigatorFactory = PdfNavigatorFactory(publication, pdfEngine)

    childFragmentManager.fragmentFactory =
      navigatorFactory.createFragmentFactory(
        initialLocator = model.initialLocation,
        listener = this,
        initialPreferences = PdfiumPreferences(fit=Fit.CONTAIN,pageSpacing = 100.0)
      )

//    setHasOptionsMenu(true)
    super.onCreate(savedInstanceState)
  }


  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val view = super.onCreateView(inflater, container, savedInstanceState)
    if (savedInstanceState == null) {
      childFragmentManager.commitNow {
        add(
          R.id.fragment_reader_container,
          PdfNavigatorFragment::class.java,
          Bundle(),
          NAVIGATOR_FRAGMENT_TAG
        )
      }
    }
    navigator = childFragmentManager.findFragmentByTag(NAVIGATOR_FRAGMENT_TAG)!! as PdfNavigatorFragment<*, *>

    return view
  }

  override fun onResourceLoadFailed(link: Link, error: Resource.Exception) {
    val message = when (error) {
      is Resource.Exception.OutOfMemory -> "The PDF is too large to be rendered on this device"
      else -> "Failed to render this PDF"
    }

    RNLog.e(message)
  }

  override fun onTap(point: PointF): Boolean {
    println("On tap PDF ---")
//    requireActivity().toggleSystemUi()
    return true
  }


  companion object {
    const val NAVIGATOR_FRAGMENT_TAG = "navigator"

    fun newInstance(): PdfReaderFragment {
      return PdfReaderFragment()
    }
  }
}
