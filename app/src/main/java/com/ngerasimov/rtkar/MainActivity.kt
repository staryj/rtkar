package com.ngerasimov.rtkar

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.android.material.navigation.NavigationView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.appcompat.app.AppCompatActivity
import com.ngerasimov.rtkar.data.StorageManager
import com.ngerasimov.rtkar.data.api.RtkApiWrapper
import com.ngerasimov.rtkar.data.repository.DeviceRepository
import com.ngerasimov.rtkar.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: DeviceRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)

        repository = DeviceRepository(
            apiWrapper = RtkApiWrapper(this),
            storageManager = StorageManager(this)
        )

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as? NavHostFragment
            ?: run {
                Log.e("MainActivity", "NavHostFragment not found in onCreate")
                return
            }
        val navController = navHostFragment.navController

        // Setup navigation based on auth status
        // Check if entities exist and set start destination accordingly
        lifecycleScope.launch {
            val authToken = repository.authToken.first()
            val isAuthorized = !authToken.isNullOrBlank()
            
            // Log auth status on application startup (without exposing token)
            Log.d("MainActivity", "Auth token on startup: ${if (authToken != null) "present" else "null"}")
            
            setupNavigation(isAuthorized)
            
            // Note: Navigation to Control/Settings is handled by LoginFragment
            // after it's fully created and the graph is ready
        }

        // Observe auth token changes to update UI
        lifecycleScope.launch {
            repository.authToken.collect { authToken ->
                val isAuthorized = !authToken.isNullOrBlank()
                
                // Update navigation configuration
                setupNavigation(isAuthorized)
                
                // Navigate based on auth status changes
                val currentDestination = navController.currentDestination?.id
                if (!isAuthorized && currentDestination != null && currentDestination != R.id.nav_login) {
                    // User logged out or token cleared - redirect to login
                    // Use post to ensure navigation graph is ready after setupNavigation
                    binding.root.post {
                        try {
                            // Get fresh reference to navController after setupNavigation
                            val navHostFragment =
                                supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as? NavHostFragment
                            val currentNavController = navHostFragment?.navController ?: navController
                            
                            // Verify the destination exists in the graph
                            val loginNode = currentNavController.graph.findNode(R.id.nav_login)
                            if (loginNode != null) {
                                // Navigate to login and pop back stack to start destination
                                // Use the destination ID directly instead of graph ID
                                currentNavController.navigate(R.id.nav_login) {
                                    // Pop everything up to and including the start destination
                                    popUpTo(R.id.nav_login) {
                                        inclusive = true
                                    }
                                }
                            } else {
                                Log.w("MainActivity", "Login destination not found in graph, will retry")
                                // Retry after a short delay
                                binding.root.postDelayed({
                                    try {
                                        val retryNavController = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as? NavHostFragment)?.navController
                                        retryNavController?.navigate(R.id.nav_login)
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", "Error navigating to login after retry", e)
                                    }
                                }, 100)
                            }
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error navigating to login after logout", e)
                            // Final fallback: try simple navigation
                            try {
                                binding.root.postDelayed({
                                    val fallbackNavController = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as? NavHostFragment)?.navController
                                    fallbackNavController?.navigate(R.id.nav_login)
                                }, 200)
                            } catch (e2: Exception) {
                                Log.e("MainActivity", "Final navigation attempt failed", e2)
                            }
                        }
                    }
                }
                // Note: We don't auto-navigate when user logs in from LoginFragment
                // LoginFragment will handle navigation to Settings after successful login
            }
        }
    }

    private fun setupNavigation(isAuthorized: Boolean) {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as? NavHostFragment
            ?: run {
                Log.e("MainActivity", "NavHostFragment not found in setupNavigation")
                return
            }
        val navController = navHostFragment.navController

        if (isAuthorized) {
            // Authorized users: Control and Settings - show navigation menus
            binding.navView?.visibility = View.VISIBLE
            binding.drawerLayout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            
            binding.appBarMain.contentMain.bottomNavView?.visibility = View.VISIBLE

        binding.navView?.let {
            appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.nav_control, R.id.nav_settings
                ),
                binding.drawerLayout
            )
            setupActionBarWithNavController(navController, appBarConfiguration)
            
            // Handle navigation item selection (including logout)
            it.setNavigationItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.nav_logout -> {
                        showLogoutConfirmation()
                        binding.drawerLayout?.closeDrawers()
                        true
                    }
                    R.id.nav_control, R.id.nav_settings -> {
                        // Let setupWithNavController handle these
                        // But we need to manually navigate since we're overriding the listener
                        navController.navigate(menuItem.itemId)
                        binding.drawerLayout?.closeDrawers()
                        true
                    }
                    else -> {
                        navController.navigate(menuItem.itemId)
                        binding.drawerLayout?.closeDrawers()
                        true
                    }
                }
            }
        }

            binding.appBarMain.contentMain.bottomNavView?.let {
                appBarConfiguration = AppBarConfiguration(
                    setOf(
                        R.id.nav_control, R.id.nav_settings
                    )
                )
                setupActionBarWithNavController(navController, appBarConfiguration)
                it.setupWithNavController(navController)
            }
        } else {
            // Guest users: Login only - hide navigation menus
            binding.navView?.visibility = View.GONE
            binding.drawerLayout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            
            binding.appBarMain.contentMain.bottomNavView?.visibility = View.GONE

            // Setup navigation without drawer/bottom nav for guests
            appBarConfiguration = AppBarConfiguration(
                setOf(R.id.nav_login)
            )
            setupActionBarWithNavController(navController, appBarConfiguration)
        }

        updateMenuVisibility(isAuthorized)
    }

    private fun updateMenuVisibility(isAuthorized: Boolean) {
        binding.navView?.menu?.let { menu ->
            menu.findItem(R.id.nav_control)?.isVisible = isAuthorized
            menu.findItem(R.id.nav_settings)?.isVisible = isAuthorized
            menu.findItem(R.id.nav_logout)?.isVisible = isAuthorized
            menu.findItem(R.id.nav_login)?.isVisible = !isAuthorized
        }

        binding.appBarMain.contentMain.bottomNavView?.menu?.let { menu ->
            menu.findItem(R.id.nav_control)?.isVisible = isAuthorized
            menu.findItem(R.id.nav_settings)?.isVisible = isAuthorized
            menu.findItem(R.id.nav_login)?.isVisible = !isAuthorized
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val result = super.onCreateOptionsMenu(menu)
        // Using findViewById because NavigationView exists in different layout files
        // between w600dp and w1240dp
        val navView: NavigationView? = findViewById(R.id.nav_view)
        if (navView == null) {
            // The navigation drawer already has the items including the items in the overflow menu
            // We only inflate the overflow menu if the navigation drawer isn't visible
            menuInflater.inflate(R.menu.overflow, menu)
            
            // Update overflow menu visibility based on auth status
            lifecycleScope.launch {
                val authToken = repository.authToken.first()
                val isAuthorized = !authToken.isNullOrBlank()
                menu.findItem(R.id.nav_settings)?.isVisible = isAuthorized
            }
        }
        return result
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_settings -> {
                val navController = findNavController(R.id.nav_host_fragment_content_main)
                navController.navigate(R.id.nav_settings)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.logout_confirmation_title)
            .setMessage(R.string.logout_confirmation_message)
            .setPositiveButton(R.string.yes) { _, _ ->
                performLogout()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun performLogout() {
        lifecycleScope.launch {
            // Clear all stored data
            // This will trigger the auth token observer which will handle navigation
            repository.clearAllData()
            
            // The observer in onCreate will automatically navigate to login
            // when it detects the auth token is cleared. We don't need to navigate manually here.
        }
    }
}