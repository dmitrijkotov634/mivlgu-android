package com.wavecat.mivlgu

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.color.DynamicColors
import com.wavecat.mivlgu.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private val model by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(setOf(R.id.GroupFragment, R.id.InfoFragment))
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.included.navView.setupWithNavController(navController)

        binding.included.navView.setOnItemSelectedListener {
            val builder = NavOptions.Builder()
                .setEnterAnim(androidx.appcompat.R.anim.abc_fade_in)
                .setExitAnim(androidx.appcompat.R.anim.abc_fade_out)

            navController.popBackStack()
            navController.navigate(getMenu(it.itemId), null, builder.build())
            true
        }

        model.loadingException.observe(this) {
            binding.toolbar.subtitle = if (it == null) "" else it.message
        }
    }

    private fun getMenu(id: Int) =
        when (id) {
            R.id.timetable -> R.id.GroupFragment
            R.id.info -> R.id.InfoFragment
            R.id.chat -> R.id.ChatFragment
            else -> R.id.GroupFragment
        }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}