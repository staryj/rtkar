package com.ngerasimov.rtkar.ui.login

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebStorage
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
    private var navigationRetryCount = 0
    private val maxNavigationRetries = 3

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    private var hasCheckedAuth = false
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Reset flags when view is created (handles fragment recreation/reuse after logout)
        hasCheckedAuth = false
        isTokenCaptured = false
        
        Log.d("LoginFragment", "onViewCreated - checking auth status")
        
        // Check auth status and set up WebView if needed
        lifecycleScope.launch {
            val authToken = viewModel.getAuthToken()
            Log.d("LoginFragment", "Auth token in onViewCreated: ${if (authToken.isNullOrEmpty()) "empty" else "present"}")
            
            if (authToken.isNullOrEmpty()) {
                // User not authorized, show login WebView
                Log.d("LoginFragment", "User not authorized, setting up WebView")
                setupWebView()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        Log.d("LoginFragment", "onResume - hasCheckedAuth: $hasCheckedAuth")
        
        // Check auth and navigate in onResume when NavController is definitely ready
        if (!hasCheckedAuth) {
            hasCheckedAuth = true
            lifecycleScope.launch {
                val authToken = viewModel.getAuthToken()
                Log.d("LoginFragment", "Auth token in onResume: ${if (authToken.isNullOrEmpty()) "empty" else "present"}")
                
                if (!authToken.isNullOrEmpty()) {
                    // User is already authorized, check if entities exist
                    val entities = viewModel.getEntities()
                    // Wait for fragment to be fully resumed and NavController graph to be initialized
                    delay(1000)
                    if (isAdded && view != null && lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                        if (entities.isNotEmpty()) {
                            // Entities exist, navigate to Control
                            // Wait additional time and verify graph is ready
                            view?.postDelayed({
                                if (isAdded && isResumed) {
                                    try {
                                        val navController = findNavController()
                                        val graph = navController.graph
                                        // Verify graph has the destination before navigating
                                        if (graph.findNode(com.ngerasimov.rtkar.R.id.nav_control) != null) {
                                            navigateToControl()
                                        } else {
                                            Log.w("LoginFragment", "Control node still not in graph, will retry")
                                            view?.postDelayed({
                                                if (isAdded && isResumed) {
                                                    navigateToControl()
                                                }
                                            }, 500)
                                        }
                                    } catch (e: Exception) {
                                        Log.e("LoginFragment", "Error checking graph", e)
                                    }
                                }
                            }, 300)
                        } else {
                            // No entities, navigate to Settings
                            view?.postDelayed({
                                if (isAdded && isResumed) {
                                    navigateToSettings()
                                }
                            }, 300)
                        }
                    }
                } else {
                    // User is not authorized - ensure WebView is set up
                    Log.d("LoginFragment", "User not authorized in onResume, ensuring WebView is set up")
                    // Always set up WebView if user is not authorized (handles case after logout)
                    if (binding.webView != null) {
                        // Check if WebView needs to be set up (no URL loaded or WebView was destroyed)
                        val currentUrl = binding.webView.url
                        if (currentUrl.isNullOrEmpty() || !currentUrl.startsWith("http")) {
                            Log.d("LoginFragment", "WebView not loaded, setting up now")
                            setupWebView()
                        } else {
                            Log.d("LoginFragment", "WebView already loaded with URL: $currentUrl")
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val webView = binding.webView
        
        Log.d("LoginFragment", "Setting up WebView")
        
        if (webView == null) {
            Log.e("LoginFragment", "WebView is null, cannot set up")
            return
        }
        
        // Clear all WebView data before loading login page
        Log.d("LoginFragment", "Clearing WebView cache, cookies, and storage")
        
        // Clear cookies
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        cookieManager.flush()
        
        // Clear cache (including disk cache)
        webView.clearCache(true)
        
        // Clear history
        webView.clearHistory()
        
        // Clear form data
        webView.clearFormData()
        
        // Clear WebStorage (localStorage, sessionStorage, etc.)
        WebStorage.getInstance().deleteAllData()
        
        Log.d("LoginFragment", "WebView data cleared")
        
        // Make sure WebView is visible
        webView.visibility = View.VISIBLE
        
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            javaScriptCanOpenWindowsAutomatically = true
        }

        // Add JavaScript interface to receive Authorization header
        webView.addJavascriptInterface(WebAppInterface(), "AndroidInterface")
        
        Log.d("LoginFragment", "WebView configured, loading URL")

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
                                    // Check if entities exist after login
                                    val entities = viewModel.getEntities()
                                    if (entities.isNotEmpty()) {
                                        navigateToControl()
                                    } else {
                                        navigateToSettings()
                                    }
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
        Log.d("LoginFragment", "WebView URL loaded: https://key.rt.ru/main/pwa/dashboard")
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
        if (!isAdded || view == null) {
            Log.w("LoginFragment", "Fragment not attached, cannot navigate")
            return
        }
        
        try {
            val navController = findNavController()
            // Don't use popUpTo with inclusive=true on start destination
            // Just navigate normally
            navController.navigate(com.ngerasimov.rtkar.R.id.nav_settings)
            Log.d("LoginFragment", "Successfully navigated to Settings")
            navigationRetryCount = 0 // Reset on success
        } catch (e: Exception) {
            Log.e("LoginFragment", "Failed to navigate to settings: ${e.message}", e)
            navigationRetryCount++
            if (navigationRetryCount < maxNavigationRetries) {
                // Retry after a delay
                view?.postDelayed({
                    if (isAdded) {
                        navigateToSettings()
                    }
                }, 300)
            } else {
                Log.e("LoginFragment", "Max retries reached, navigation failed")
                navigationRetryCount = 0
            }
        }
    }
    
    private fun navigateToControl() {
        if (!isAdded || view == null) {
            Log.w("LoginFragment", "Fragment not attached, cannot navigate")
            return
        }
        
        if (navigationRetryCount >= maxNavigationRetries) {
            Log.e("LoginFragment", "Max navigation retries reached, giving up")
            navigationRetryCount = 0
            return
        }
        
        try {
            val navController = findNavController()
            // Check if graph is ready and has the destination
            val graph = navController.graph
            val controlNode = graph.findNode(com.ngerasimov.rtkar.R.id.nav_control)
            
            if (controlNode == null) {
                Log.w("LoginFragment", "Control destination not found in navigation graph, retrying... (attempt ${navigationRetryCount + 1})")
                navigationRetryCount++
                // Retry after a short delay
                view?.postDelayed({
                    if (isAdded) {
                        navigateToControl()
                    }
                }, 200)
                return
            }
            
            // Navigate without popUpTo - just simple navigation
            navController.navigate(com.ngerasimov.rtkar.R.id.nav_control)
            Log.d("LoginFragment", "Successfully navigated to Control")
            navigationRetryCount = 0 // Reset on success
        } catch (e: Exception) {
            Log.e("LoginFragment", "Failed to navigate to control: ${e.message}", e)
            navigationRetryCount++
            if (navigationRetryCount < maxNavigationRetries) {
                // Retry after a longer delay
                view?.postDelayed({
                    navigateToControl()
                }, 300)
            } else {
                Log.e("LoginFragment", "Max retries reached, navigation failed")
                navigationRetryCount = 0
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
                        // Check if entities exist after login
                        val entities = viewModel.getEntities()
                        if (entities.isNotEmpty()) {
                            navigateToControl()
                        } else {
                            navigateToSettings()
                        }
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