package com.ngerasimov.rtkar.ui.login

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.ngerasimov.rtkar.databinding.FragmentLoginBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()
    private var isTokenCaptured = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Check if user is already authorized
        lifecycleScope.launch {
            val authToken = viewModel.getAuthToken()
            if (!authToken.isNullOrEmpty()) {
                // User is already authorized, navigate to Settings
                // Add a small delay to ensure fragment is fully attached
                delay(200)
                navigateToSettings()
            } else {
                // User not authorized, show login WebView
                setupWebView()
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val webView = binding.webView
        
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            javaScriptCanOpenWindowsAutomatically = true
        }

        // Add JavaScript interface to receive Authorization header
        webView.addJavascriptInterface(WebAppInterface(), "AndroidInterface")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): android.webkit.WebResourceResponse? {
                // Check if request is to household.key.rt.ru and has Authorization header
                request?.url?.let { url ->
                    if (url.toString().startsWith("https://household.key.rt.ru")) {
                        request.requestHeaders?.get("Authorization")?.let { authHeader ->
                            if (authHeader.startsWith("Bearer") && !isTokenCaptured) {
                                isTokenCaptured = true
                                lifecycleScope.launch {
                                    viewModel.saveAuthToken(authHeader)
                                    Log.d("LoginFragment", "Auth token captured from request: $authHeader")
                                    // Add a small delay to ensure navigation graph is ready
                                    delay(100)
                                    navigateToSettings()
                                }
                            }
                        }
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                // Inject script early to catch requests
                view?.postDelayed({
                    injectInterceptionScript(view)
                }, 100)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                
                // Inject JavaScript to intercept XMLHttpRequest and fetch calls
                // Use postDelayed to ensure page is fully loaded
                view?.postDelayed({
                    injectInterceptionScript(view)
                }, 500)
            }
        }

        // Load the login URL
        webView.loadUrl("https://key.rt.ru/main/pwa/dashboard")
    }

    private fun injectInterceptionScript(webView: WebView?) {
        val script = """
            (function() {
                // Intercept XMLHttpRequest
                const originalXHROpen = XMLHttpRequest.prototype.open;
                const originalXHRSend = XMLHttpRequest.prototype.send;
                const originalSetRequestHeader = XMLHttpRequest.prototype.setRequestHeader;
                
                XMLHttpRequest.prototype.open = function(method, url, ...args) {
                    this._url = url;
                    this._headers = {};
                    return originalXHROpen.apply(this, [method, url, ...args]);
                };
                
                XMLHttpRequest.prototype.setRequestHeader = function(header, value) {
                    if (!this._headers) {
                        this._headers = {};
                    }
                    this._headers[header] = value;
                    if (header.toLowerCase() === 'authorization' && 
                        value.startsWith('Bearer') && 
                        this._url && 
                        this._url.startsWith('https://household.key.rt.ru')) {
                        if (window.AndroidInterface) {
                            window.AndroidInterface.onAuthTokenCaptured(value);
                        }
                    }
                    return originalSetRequestHeader.apply(this, [header, value]);
                };
                
                XMLHttpRequest.prototype.send = function(...args) {
                    // Check headers before sending
                    if (this._url && this._url.startsWith('https://household.key.rt.ru')) {
                        const headers = this._headers || {};
                        const authHeader = headers['Authorization'] || headers['authorization'];
                        if (authHeader && authHeader.startsWith('Bearer')) {
                            if (window.AndroidInterface) {
                                window.AndroidInterface.onAuthTokenCaptured(authHeader);
                            }
                        }
                    }
                    return originalXHRSend.apply(this, args);
                };
                
                // Intercept fetch API
                const originalFetch = window.fetch;
                window.fetch = function(url, options) {
                    if (typeof url === 'string' && url.startsWith('https://household.key.rt.ru')) {
                        const headers = options?.headers || {};
                        let authHeader = null;
                        
                        if (headers instanceof Headers) {
                            authHeader = headers.get('Authorization') || headers.get('authorization');
                        } else if (typeof headers === 'object') {
                            authHeader = headers['Authorization'] || headers['authorization'];
                        }
                        
                        if (authHeader && authHeader.startsWith('Bearer')) {
                            if (window.AndroidInterface) {
                                window.AndroidInterface.onAuthTokenCaptured(authHeader);
                            }
                        }
                    }
                    return originalFetch.apply(this, arguments);
                };
            })();
        """.trimIndent()
        
        webView?.evaluateJavascript(script, null)
    }

    private fun navigateToSettings() {
        if (isAdded && view != null) {
            try {
                findNavController().navigate(com.ngerasimov.rtkar.R.id.nav_settings)
            } catch (e: Exception) {
                Log.e("LoginFragment", "Failed to navigate to settings", e)
            }
        }
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun onAuthTokenCaptured(token: String) {
            if (!isTokenCaptured && token.startsWith("Bearer")) {
                isTokenCaptured = true
                // JavaScript interface runs on a background thread, so we need to post to main thread
                activity?.runOnUiThread {
                    lifecycleScope.launch {
                        viewModel.saveAuthToken(token)
                        Log.d("LoginFragment", "Auth token captured from JS: $token")
                        // Add a small delay to ensure navigation graph is ready
                        delay(100)
                        navigateToSettings()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.webView.destroy()
        _binding = null
    }
}