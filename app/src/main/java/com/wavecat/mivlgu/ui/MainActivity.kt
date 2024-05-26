package com.wavecat.mivlgu.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
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
import com.wavecat.mivlgu.ui.chat.ChatFragment
import com.wavecat.mivlgu.ui.donate.BillingViewModel
import com.wavecat.mivlgu.ui.timetable.TimetableFragment
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private val model by viewModels<MainViewModel>()
    private val billingModel by viewModels<BillingViewModel>()

    private val repository by lazy { MainRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            billingModel.billingClient.onNewIntent(intent)
        }

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
                false
            } else {
                navController.popBackStack()
                navController.navigate(getMenu(it.itemId), null, navOptions.build())
                true
            }
        }

        model.loadingException.observe(this) {
            when (it) {
                null -> {}
                is IOException -> Snackbar.make(binding.root, R.string.no_internet, Snackbar.LENGTH_SHORT).show()
                else -> binding.toolbar.subtitle = it.message
            }
        }

        model.currentWeek.observe(this) {
            binding.toolbar.subtitle = if (it == null) "" else getString(R.string.current_week, it)
        }

        if (intent.getBooleanExtra(ChatFragment.OPEN_AI_CHAT_ARG, false)) {
            navController.popBackStack()
            navController.navigate(R.id.ChatFragment, intent.extras, navOptions.build())
        }

        intent.getStringExtra(TimetableFragment.CACHE_KEY_ARG)?.let {
            model.restoreTimetableFromCache(it)
            navController.popBackStack()
            navController.navigate(R.id.TimetableFragment, intent.extras, navOptions.build())
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        billingModel.billingClient.onNewIntent(intent)
    }

    fun invalidateNavMenu() {
        binding.included.navView.menu.clear()
        binding.included.navView.inflateMenu(R.menu.bottom_nav_menu)

        removeDisabledMenuItems()
    }

    fun enableNotifications() {
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

    private fun removeDisabledMenuItems() {
        binding.included.navView.visibility =
            if (repository.disableAI && repository.disableIEP) View.GONE else View.VISIBLE

        WindowCompat.setDecorFitsSystemWindows(window, repository.disableAI && repository.disableIEP)

        if (repository.disableIEP)
            binding.included.navView.menu.removeItem(R.id.iep)

        if (repository.disableAI)
            binding.included.navView.menu.removeItem(R.id.chat)
    }

    private fun getMenu(id: Int) = when (id) {
        R.id.timetable -> R.id.GroupFragment
        R.id.chat -> R.id.ChatFragment
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