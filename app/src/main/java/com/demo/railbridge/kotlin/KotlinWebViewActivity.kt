package com.demo.railbridge.kotlin

import android.os.Bundle
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.demo.railbridge.BuildConfig
import com.demo.railbridge.R
import com.demo.railbridge.logging.ErrorLogger
import com.demo.railbridge.sdk.MockRailSdk
import com.demo.railbridge.sdk.RailPlusSdkAdapter
import com.demo.railbridge.sdk.SdkErrorCode

class KotlinWebViewActivity : AppCompatActivity() {

    private var webView: WebView? = null
    private var sdkAdapter: RailPlusSdkAdapter? = null
    private var errorLogger: ErrorLogger? = null
    private var nativeBridge: KotlinNativeBridge? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        sdkAdapter = KotlinMockRailSdkAdapter(MockRailSdk())
        errorLogger = ErrorLogger.getInstance(this).apply {
            setCrashlyticsEnabled(true)
        }

        webView = findViewById(R.id.webView)
        setupWebView(requireNotNull(webView), requireNotNull(errorLogger))

        val bridge = KotlinNativeBridge(
            requireNotNull(webView),
            requireNotNull(sdkAdapter),
            requireNotNull(errorLogger)
        )
        nativeBridge = bridge
        webView?.addJavascriptInterface(bridge, "RailBridge")

        sdkAdapter?.initialize(object : RailPlusSdkAdapter.Callback<Boolean> {
            override fun onSuccess(result: Boolean) {
                Log.d(TAG, "Kotlin SDK initialized successfully")
            }

            override fun onError(errorCode: SdkErrorCode) {
                Log.e(TAG, "Kotlin SDK initialization failed: $errorCode")
            }
        })

        webView?.loadUrl("file:///android_asset/webview/index.html")
    }

    private fun setupWebView(webView: WebView, errorLogger: ErrorLogger) {
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.javaScriptCanOpenWindowsAutomatically = true

        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = false

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                Log.d(TAG, "Kotlin page finished loading: $url")
                view.evaluateJavascript("window.dispatchEvent(new Event('bridgeReady'))", null)
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)
                Log.e(TAG, "Kotlin WebView error: ${error.description} (code: ${error.errorCode})")
                errorLogger.logSdkFailure(
                    "Kotlin WebView",
                    SdkErrorCode.ERR_NETWORK_TIMEOUT,
                    "URL: ${request.url}, Error: ${error.description}"
                )
            }
        }

        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }

    override fun onDestroy() {
        nativeBridge?.destroy()
        sdkAdapter?.shutdown()
        webView?.removeJavascriptInterface("RailBridge")
        webView?.stopLoading()
        webView?.loadUrl("about:blank")
        webView?.clearHistory()
        webView?.removeAllViews()
        webView?.destroy()
        webView = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "RailBridge.KotlinWebViewActivity"
    }
}
