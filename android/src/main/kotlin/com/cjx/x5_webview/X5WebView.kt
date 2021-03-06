package com.cjx.x5_webview

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.inputmethodservice.Keyboard
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.Window
import com.tencent.smtt.export.external.interfaces.IX5WebChromeClient
import com.tencent.smtt.export.external.interfaces.WebResourceRequest
import com.tencent.smtt.sdk.WebChromeClient
import com.tencent.smtt.sdk.WebSettings
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient
import io.flutter.embedding.engine.systemchannels.KeyEventChannel
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView


@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
class X5WebView(private val context: Context, private val id: Int, private val params: Map<String, Any>, val messenger: BinaryMessenger? = null, private val containerView: View?) : PlatformView, MethodChannel.MethodCallHandler {
    private var webView: WebView
    private val channel: MethodChannel = MethodChannel(messenger, "com.cjx/x5WebView_$id")



    init {
        webView = WebView(context)
        channel.setMethodCallHandler(this)
        webView.apply {
            settings.javaScriptEnabled = params["javaScriptEnabled"] as Boolean
//            settings.useWideViewPort = true
            settings.domStorageEnabled = true
            settings.lightTouchEnabled= true
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            Log.e("x5webview/constructor",params.toString())
            if(params["cacheMode"] != null) {
                Log.e("flutter/cachemode",params["cacheMode"].toString())
                settings.cacheMode =params["cacheMode"] as Int
            }
            if (params["javascriptChannels"] != null) {
                val names = params["javascriptChannels"] as List<String>
                for (name in names) {
                    webView.addJavascriptInterface(JavascriptChannel(name, channel, context), name)
                }
            }
            if (params["header"] != null) {
                val header = params["header"] as Map<String, String>
                loadUrl(params["url"].toString(), header)
            } else {
                loadUrl(params["url"].toString())
            }

            if(params["userAgentString"] != null){
                settings.userAgentString=params["userAgentString"].toString()
            }

            val urlInterceptEnabled = params["urlInterceptEnabled"] as Boolean

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, loadUrl: String?): Boolean {

                    if (urlInterceptEnabled) {
                        val arg = hashMapOf<String, String>()
                        arg["url"] = loadUrl?:""
                        channel.invokeMethod("onUrlLoading", arg)
                        return true
                    }
                    view.loadUrl(url)
                    return super.shouldOverrideUrlLoading(view, url)
                }


//                }

                override fun shouldOverrideKeyEvent(p0: WebView?, event: KeyEvent?): Boolean {

                    // Let action bars open menus in response to the menu key prioritized over
                    // the window handling it
                    event?.let {
                        var type = "unknow"
                        if(event.action == KeyEvent.ACTION_UP) {
                            type = "keyup"
                        }

                        if(event.action == KeyEvent.ACTION_DOWN) {
                            type = "keydown"

                        }

                        val message = hashMapOf<String,Any>(
                                "flags" to event.flags,
                                "keyCode" to event.keyCode,
                                "scanCode" to event.scanCode,
                                "metaState" to event.metaState,
                                "eventSource" to event.source,
                                "deviceId" to event.deviceId,
                                "repeatCount" to event.repeatCount,
                                "character" to event.characters,
                                "keymap" to "android",
                                "type" to type,
                                "codePoint" to event.unicodeChar,
                                "plainCodePoint" to event.getUnicodeChar(0)
                        )

                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                            message["vendorId"] = event.device.vendorId
                            message["productId"] = event.device.productId
                        }


                        channel.invokeMethod("shouldOverrideKeyEvent", message)
                    }

                    return true
                }



                override fun onUnhandledKeyEvent(p0: WebView?, p1: KeyEvent) {

                    val arg = hashMapOf<String, Any>()
                    arg["url"] = url
                    KeyEventChannel.FlutterKeyEvent(p1)
                }

                override fun onPageFinished(p0: WebView?, url: String) {
                    super.onPageFinished(p0, url)
                    //向flutter通信
                    val arg = hashMapOf<String, Any>()
                    arg["url"] = url
                    channel.invokeMethod("onPageFinished", arg)
                }

            }
            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View?, call: IX5WebChromeClient.CustomViewCallback?) {
                    super.onShowCustomView(view, call)
                    channel.invokeMethod("onShowCustomView", null)
                }

                override fun onHideCustomView() {
                    super.onHideCustomView()
                    channel.invokeMethod("onHideCustomView", null)
                }

                override fun onProgressChanged(p0: WebView?, p1: Int) {
                    super.onProgressChanged(p0, p1)
                    //加载进度
                    val arg = hashMapOf<String, Any>()
                    arg["progress"] = p1
                    channel.invokeMethod("onProgressChanged", arg)
                }
            }

//            val data= Bundle()
            //true表示标准全屏，false表示X5全屏；不设置默认false，
//            data.putBoolean("standardFullScreen",true)
            //false：关闭小窗；true：开启小窗；不设置默认true，
//            data.putBoolean("supportLiteWnd",false)
            //1：以页面内开始播放，2：以全屏开始播放；不设置默认：1
//            data.putInt("DefaultVideoScreen",2)
//            x5WebViewExtension.invokeMiscMethod("setVideoParams",data)
        }

    }


    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "loadUrl" -> {
                val arg = call.arguments as Map<String, Any>
                val url = arg["url"].toString()
                val headers = arg["headers"] as? Map<String, String>
                webView.loadUrl(url, headers)
                result.success(null)
            }
            "canGoBack" -> {
                result.success(webView.canGoBack())
            }
            "canGoForward" -> {
                result.success(webView.canGoForward())
            }
            "goBack" -> {
                webView.goBack()
                result.success(null)
            }
            "goForward" -> {
                webView.goForward()
                result.success(null)
            }
            "goBackOrForward" -> {
                val arg = call.arguments as Map<String, Any>
                val point = arg["i"] as Int
                webView.goBackOrForward(point)
                result.success(null)
            }
            "reload" -> {
                webView.reload()
                result.success(null)
            }
            "currentUrl" -> {
                result.success(webView.url)
            }
            "evaluateJavascript" -> {
                val arg = call.arguments as Map<String, Any>
                val js = arg["js"].toString()
                webView.evaluateJavascript(js) { value -> result.success(value) }
            }

            "addJavascriptChannels" -> {
                val arg = call.arguments as Map<String, Any>
                val names = arg["names"] as List<String>
                for (name in names) {
                    webView.addJavascriptInterface(JavascriptChannel(name, channel, context), name)
                }
                webView.reload()
                result.success(null)

            }
            "isX5WebViewLoadSuccess" -> {
                val exception = webView.x5WebViewExtension
                if (exception == null) {
                    result.success(false)
                } else {
                    result.success(true)
                }
            }

            else -> {
                result.notImplemented()
            }
        }
    }

    override fun getView(): View {
        return webView
    }

    override fun dispose() {
        channel.setMethodCallHandler(null)
        webView.destroy()
    }
}