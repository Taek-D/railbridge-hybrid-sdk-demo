package com.demo.railbridge;

import android.os.Bundle;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import com.demo.railbridge.bridge.NativeBridge;
import com.demo.railbridge.logging.ErrorLogger;
import com.demo.railbridge.sdk.MockRailSdk;
import com.demo.railbridge.sdk.MockRailSdkAdapter;
import com.demo.railbridge.sdk.RailPlusSdkAdapter;
import com.demo.railbridge.sdk.SdkErrorCode;

public class WebViewActivity extends AppCompatActivity {

    private static final String TAG = "RailBridge.WebViewActivity";

    private WebView webView;
    private RailPlusSdkAdapter sdkAdapter;
    private ErrorLogger errorLogger;
    private NativeBridge nativeBridge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        sdkAdapter = new MockRailSdkAdapter(new MockRailSdk());
        errorLogger = ErrorLogger.getInstance(this);
        errorLogger.setCrashlyticsEnabled(true);

        webView = findViewById(R.id.webView);
        setupWebView();

        nativeBridge = new NativeBridge(webView, sdkAdapter, errorLogger);
        webView.addJavascriptInterface(nativeBridge, "RailBridge");

        sdkAdapter.initialize(new RailPlusSdkAdapter.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                Log.d(TAG, "SDK initialized successfully");
            }

            @Override
            public void onError(SdkErrorCode errorCode) {
                Log.e(TAG, "SDK initialization failed: " + errorCode);
            }
        });

        webView.loadUrl("file:///android_asset/webview/index.html");
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "Page finished loading: " + url);
                view.evaluateJavascript("window.dispatchEvent(new Event('bridgeReady'))", null);
            }

            @Override
            public void onReceivedError(
                    WebView view,
                    WebResourceRequest request,
                    WebResourceError error
            ) {
                super.onReceivedError(view, request, error);
                Log.e(TAG, "WebView error: " + error.getDescription() + " (code: " + error.getErrorCode() + ")");

                errorLogger.logSdkFailure(
                        "WebView",
                        SdkErrorCode.ERR_NETWORK_TIMEOUT,
                        "URL: " + request.getUrl() + ", Error: " + error.getDescription()
                );
            }
        });

        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
    }

    @Override
    protected void onDestroy() {
        if (nativeBridge != null) {
            nativeBridge.destroy();
        }
        if (sdkAdapter != null) {
            sdkAdapter.shutdown();
        }
        if (webView != null) {
            webView.removeJavascriptInterface("RailBridge");
            webView.stopLoading();
            webView.loadUrl("about:blank");
            webView.clearHistory();
            webView.removeAllViews();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
