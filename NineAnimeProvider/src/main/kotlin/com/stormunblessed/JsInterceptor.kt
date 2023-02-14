package com.stormunblessed


import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.lagradost.cloudstream3.AcraApplication.Companion.context
import okhttp3.*
import java.util.concurrent.CountDownLatch
import com.stormunblessed.JsGET

// This was extracted from https://github.com/jmir1/aniyomi-extensions/blob/master/src/en/nineanime/src/eu/kanade/tachiyomi/animeextension/en/nineanime/JsInterceptor.kt
// The following code is under the Apache License 2.0 https://github.com/jmir1/aniyomi-extensions/blob/master/LICENSE
class JsInterceptor(private val lang: String) : Interceptor {

    private val handler by lazy { Handler(Looper.getMainLooper()) }

    class JsObject(var payload: String = "") {
        @JavascriptInterface
        fun passPayload(passedPayload: String) {
            payload = passedPayload
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        handler.post {
            context.let { Toast.makeText(it, "Getting video, please wait", Toast.LENGTH_SHORT).show() }
        }
        val newRequest = resolveWithWebView(originalRequest) ?: throw Exception("Someting went wrong")

        return chain.proceed(newRequest)
    }


    @SuppressLint("SetJavaScriptEnabled")
    private fun resolveWithWebView(request: Request): Request? {

        val latch = CountDownLatch(1)

        var webView: WebView? = null

        val origRequestUrl = request.url.toString()

        val jsinterface = JsObject()

        // JavaSrcipt gets the Dub or Sub link of vidstream
        val jsScript = """
            (function(){
                let jqclk = jQuery.Event('click');
                jqclk.isTrusted = true;
                jqclk.originalEvent = {
                  isTrusted: true
                };
                ${'$'}('div[data-type="$lang"] ul li[data-sv-id="41"]').trigger(jqclk);
                let intervalId = setInterval(() => {
                    let element = document.querySelector("#player iframe");
                    if (element) {
                        clearInterval(intervalId);
                        window.android.passPayload(element.src);
                    }
                }, 500);
            })();
        """

        val headers = request.headers.toMultimap().mapValues { it.value.getOrNull(0) ?: "" }.toMutableMap()

        var newRequest: Request? = null

        handler.post {
            val webview = WebView(context!!)
            webView = webview
            with(webview.settings) {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                useWideViewPort = false
                loadWithOverviewMode = false
                userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:106.0) Gecko/20100101 Firefox/106.0"
                webview.addJavascriptInterface(jsinterface, "android")
                webview.webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                        if (!request?.url.toString().contains("vidstream") &&
                            !request?.url.toString().contains("vizcloud")
                        ) return null

                        if (request?.url.toString().contains("/simple/")) {
                            newRequest = JsGET(
                                request?.url.toString(),
                                Headers.headersOf("referer", "/orp.maertsdiv//:sptth".reversed())
                            )
                            latch.countDown()
                            return null
                        }
                        return super.shouldInterceptRequest(view, request)
                    }
                    override fun onPageFinished(view: WebView?, url: String?) {
                        view?.evaluateJavascript(jsScript) {}
                    }
                }
                webView?.loadUrl(origRequestUrl, headers)
            }
        }

        latch.await()

        handler.post {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
        return newRequest
    }
}