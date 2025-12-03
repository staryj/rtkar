package com.ngerasimov.rtkar

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.google.android.material.navigation.NavigationView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.appcompat.app.AppCompatActivity
import com.ngerasimov.rtkar.data.StorageManager
import com.ngerasimov.rtkar.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var storageManager: StorageManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)

        storageManager = StorageManager(this)

        val navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment?)!!
        val navController = navHostFragment.navController

        // Setup navigation based on auth status
        // Don't navigate on startup - let LoginFragment handle it if user is already authorized
        lifecycleScope.launch {
            val authToken = storageManager.authToken.first()
            val isAuthorized = !authToken.isNullOrEmpty()
            
            // Log auth_token on application startup
            if (authToken != null) {
                Log.d("MainActivity", "Auth token on startup: $authToken")
            } else {
                Log.d("MainActivity", "No auth token on startup")
            }
            
            setupNavigation(isAuthorized)
        }

        // Observe auth token changes to update UI
        lifecycleScope.launch {
            storageManager.authToken.collect { authToken ->
                val isAuthorized = !authToken.isNullOrEmpty()
                
                // Update navigation configuration
                setupNavigation(isAuthorized)
                
                // Navigate based on auth status changes
                val currentDestination = navController.currentDestination?.id
                if (!isAuthorized && currentDestination != null && currentDestination != R.id.nav_login) {
                    // User logged out or token cleared - redirect to login
                    try {
                        navController.navigate(R.id.nav_login) {
                            popUpTo(R.id.nav_control) { inclusive = true }
                        }
                    } catch (e: Exception) {
                        // Navigation might fail if graph is not ready, ignore
                    }
                }
                // Note: We don't auto-navigate when user logs in from LoginFragment
                // LoginFragment will handle navigation to Settings after successful login
            }
        }
    }

    private fun setupNavigation(isAuthorized: Boolean) {
        val navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment?)!!
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
                it.setupWithNavController(navController)
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
                val authToken = storageManager.authToken.first()
                val isAuthorized = !authToken.isNullOrEmpty()
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
}