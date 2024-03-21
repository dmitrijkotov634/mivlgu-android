package com.wavecat.mivlgu.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.android.material.snackbar.Snackbar
import com.wavecat.mivlgu.MainRepository
import com.wavecat.mivlgu.R
import com.wavecat.mivlgu.databinding.ActivityMainBinding
import com.wavecat.mivlgu.ui.timetable.TimetableFragment
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private val model by viewModels<MainViewModel>()

    private val repository by lazy { MainRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(setOf(R.id.GroupFragment, R.id.ChatFragment))
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.included.navView.setupWithNavController(navController)

        removeDisabledMenuItems()

        val navOptions = NavOptions.Builder()
            .setEnterAnim(androidx.appcompat.R.anim.abc_fade_in)
            .setExitAnim(androidx.appcompat.R.anim.abc_fade_out)

        binding.included.navView.setOnItemSelectedListener {
            if (it.itemId == R.id.iep) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(IEP)))
            } else {
                navController.popBackStack()
                navController.navigate(getMenu(it.itemId), null, navOptions.build())
            }

            true
        }

        model.loadingException.observe(this) {
            if (it != null)
                when (it) {
                    is IOException -> Snackbar.make(binding.root, R.string.no_internet, Snackbar.LENGTH_SHORT).show()
                    else -> binding.toolbar.subtitle = it.message
                }
        }

        model.currentWeek.observe(this) {
            binding.toolbar.subtitle = if (it == null) "" else getString(R.string.current_week, it)
        }

        intent.getStringExtra(TimetableFragment.CACHE_KEY_ARG)?.let {
            model.restoreTimetableFromCache(it)
            navController.popBackStack()
            navController.navigate(R.id.TimetableFragment, intent.extras, navOptions.build())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && repository.useAnalyticsFunctions) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { result: Boolean? ->
                if (!result!!) {
                    Snackbar.make(
                        binding.getRoot(),
                        R.string.notification_permission,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun invalidateNavMenu() {
        binding.included.navView.menu.clear()
        binding.included.navView.inflateMenu(R.menu.bottom_nav_menu)

        removeDisabledMenuItems()
    }

    private fun removeDisabledMenuItems() {
        if (repository.disableIEP)
            binding.included.navView.menu.removeItem(R.id.iep)

        if (repository.disableAI)
            binding.included.navView.menu.removeItem(R.id.chat)

        if (!repository.disableAI || !repository.disableIEP)
            binding.included.navView.menu.removeItem(R.id.settings)
    }

    private fun getMenu(id: Int) = when (id) {
        R.id.timetable -> R.id.GroupFragment
        R.id.chat -> R.id.ChatFragment
        R.id.settings -> R.id.SettingsFragment
        else -> R.id.GroupFragment
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    companion object {
        const val IEP = "https://www.mivlgu.ru/iop/"
    }
}