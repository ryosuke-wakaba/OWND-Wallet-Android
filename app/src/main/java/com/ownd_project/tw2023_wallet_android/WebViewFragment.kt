package com.ownd_project.tw2023_wallet_android

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.navArgs
import com.ownd_project.tw2023_wallet_android.utils.DisplayUtil
import java.net.URI


class WebViewFragment : Fragment() {

    private lateinit var webView: WebView
    private lateinit var backButton: Button
    private val args: WebViewFragmentArgs by navArgs()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_webview, container, false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()
        val menuProvider = WebViewFragmentMenuProvider(this, activity.menuInflater)
        activity.addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)

        webView = view.findViewById(R.id.webview)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val uri: Uri = request.url
                if ("openid-credential-offer" == uri.scheme) {
                    val intent = Intent(activity, MainActivity::class.java)
                    intent.data = uri
                    startActivity(intent)
                    return true // URL ロードをキャンセルし、カスタム処理に委ねる
                } else {
                    view?.loadUrl(request?.url.toString())
                    return true // WebView内でリンクを処理することを示す
                }
                return super.shouldOverrideUrlLoading(view, request)
            }
        }

        webView.settings.javaScriptEnabled = true  // JavaScriptを有効にする

        val url = args.url
        val cookies = args.cookies ?: arrayOf()

        if (url.startsWith("http")) {
            DisplayUtil.setFragmentTitle(
                activity as? AppCompatActivity,
                URI.create(url).host
            )
        }

        print("set cookies")
        setCookies(url, cookies, webView)
        print("load url: $url")
        webView.loadUrl(url)
    }

    private fun setCookies(urlString: String, cookieStrings: Array<String>, webView: WebView) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        val uri = URI.create(urlString)
        val domain = uri.host ?: ""

        print("cookies: $cookieStrings")
        cookieStrings.forEach { cookieString ->
            // `key=value`形式を解析
            val parts = cookieString.split("=", limit = 2)
            if (parts.size == 2) {
                val name = parts[0]
                val value = parts[1]
                // val cookieValue = "$name=$value; domain=$domain; path=/"
                val hasPath = cookieString.contains("path=", ignoreCase = true)
                val cookieValue = if (hasPath) {
                    "$name=$value; domain=$domain"
                } else {
                    "$name=$value; domain=$domain; path=/"
                }
                print("cookie: $cookieValue")
                cookieManager.setCookie(urlString, cookieValue)
                cookieManager.setAcceptThirdPartyCookies(webView, true)
            }
        }
    }
}

class WebViewFragmentMenuProvider(
    private val fragment: Fragment,
    private val menuInflater: MenuInflater
) : MenuProvider {

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        // メニューをインフレート
        menuInflater.inflate(R.menu.menu_cancel, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        // メニューアイテムの選択を処理
        return when (menuItem.itemId) {
            R.id.action_cancel -> {
                // キャンセルが選択されたときの処理
                fragment.requireActivity().finish()
                true
            }

            else -> false
        }
    }
}
