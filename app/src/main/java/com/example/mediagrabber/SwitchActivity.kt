package com.example.mediagrabber

import com.example.mediagrabber.fragments.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class SwitchActivity : AppCompatActivity() {

    private val homeFragment = MediaFragment()
    private val cookieFragment = CookieFragment()
    private val downloadFragment = DownloadFragment()

    private lateinit var currentFragment: Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bottom_navigation)

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    switchFragment(homeFragment)
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.navigation_more -> {
                    switchFragment(downloadFragment)
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.navigation_settings-> {
                    switchFragment(cookieFragment)
                    return@setOnNavigationItemSelectedListener true
                }
                else -> false
            }
        }

        // Ustawienie domyślnego fragmentu
        currentFragment = homeFragment
        supportFragmentManager.beginTransaction().replace(R.id.frame_container, homeFragment).commit()
    }


    private fun switchFragment(fragment: Fragment) {
        if (currentFragment != fragment) {
            supportFragmentManager.beginTransaction().replace(R.id.frame_container, fragment).commit()
            currentFragment = fragment
        }
    }
}