package com.onionhost.app.hosting

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.onionhost.app.database.dao.WebsiteDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var websiteDao: WebsiteDao

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            CoroutineScope(Dispatchers.IO).launch {
                val activeWebsite = websiteDao.getActiveWebsite()
                if (activeWebsite != null) {
                    OnionHostingService.startService(context, activeWebsite.id)
                }
            }
        }
    }
}
