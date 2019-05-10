package com.cumulations.libreV2.fragments

import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.*
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import com.libre.R
import com.libre.Scanning.ScanningHandler
import com.libre.util.LibreLogger
import kotlinx.android.synthetic.main.ct_fragment_tutorials.*


class CTTutorialsFragment:Fragment() {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            super.onCreateView(inflater, container, savedInstanceState)
        return inflater?.inflate(R.layout.ct_fragment_tutorials,container,false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun initViews() {
        web_view.webViewClient = MyWebViewClient(progress_bar)

        web_view.settings.loadsImagesAutomatically = true
        web_view.settings.javaScriptEnabled = true
        web_view.settings.setRenderPriority(WebSettings.RenderPriority.HIGH)
//        web_view.settings.cacheMode = WebSettings.LOAD_NO_CACHE

        web_view.scrollBarStyle = View.SCROLLBARS_OUTSIDE_OVERLAY
        web_view.isScrollbarFadingEnabled = true
//        web_view.settings.userAgentString = "Mozilla/5.0 (iPhone; U; CPU like Mac OS X; en) AppleWebKit/420+ (KHTML, like Gecko) Version/3.0 Mobile/1A543a Safari/419.3"
        web_view.canGoBack()
        web_view.canGoForward()
        web_view.loadUrl(getString(R.string.riva_tutorial_url))

        web_view.setOnKeyListener { view, keyCode, keyEvent ->
            if (keyCode == KeyEvent.KEYCODE_BACK
                    && keyEvent.action == MotionEvent.ACTION_UP
                    && web_view.canGoBack()) {
                web_view.goBack()
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }
    }

    internal class MyWebViewClient(var progressBar: ProgressBar) : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            view.loadUrl(url)
            return true
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {

            LibreLogger.d(this, "page started")
            progressBar.visibility = View.VISIBLE
            super.onPageStarted(view, url, favicon)
        }

        override fun onPageFinished(view: WebView, url: String) {
            LibreLogger.d(this, "page finished")
            progressBar.visibility = View.INVISIBLE
            super.onPageFinished(view, url)
        }

    }
}