/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
 * Licensed under the Apache License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-3.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package net.ankio.auto.xposed.hooks.qianji.hooks

import android.app.Activity
import android.app.Application
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.xposed.common.ServerInfo
import net.ankio.auto.xposed.core.App
import net.ankio.auto.xposed.core.api.HookerManifest
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.core.ui.ColorUtils
import net.ankio.auto.xposed.core.ui.ViewUtils
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.databinding.MenuItemBinding
import net.ankio.auto.xposed.core.utils.MessageUtils
import net.ankio.auto.xposed.core.utils.ThreadUtils
import net.ankio.auto.xposed.hooks.qianji.sync.AssetsUtils
import net.ankio.auto.xposed.hooks.qianji.sync.BaoXiaoUtils
import net.ankio.auto.xposed.hooks.qianji.sync.BookUtils
import net.ankio.auto.xposed.hooks.qianji.sync.CategoryUtils
import net.ankio.auto.xposed.hooks.qianji.sync.SyncBillUtils


class SideBarHooker : PartHooker() {


    override fun hook(
        hookerManifest: HookerManifest,
        application: Application?,
        classLoader: ClassLoader
    ) {
        this.hookerManifest = hookerManifest
        val clazz = Hooker.loader("com.mutangtech.qianji.ui.main.MainActivity")

        Hooker.after(
            clazz,
            "onCreate",
            android.os.Bundle::class.java
        ){ param ->
            val activity = param.thisObject as Activity
            hookMenu(activity, classLoader)
        }

        Hooker.after(
            clazz,
            "onResume"
        ){ param ->
            val activity = param.thisObject as Activity
            runCatching {
                hookerManifest.attachResource(activity)
                ThreadUtils.launch {
                    checkServerStatus(activity)
                }
                syncData2Auto(activity)
            }.onFailure {
                hookerManifest.logE(it)
            }
        }

    }

    private fun hookMenu(
        activity: Activity,
        classLoader: ClassLoader?
    ) {
        if (!::hookerManifest.isInitialized) {
            return
        }
        val clazz = classLoader!!.loadClass("com.mutangtech.qianji.ui.maindrawer.MainDrawerLayout")
        Hooker.onceAfter(clazz, "refreshAccount") {
            // 只hook一次
            val obj = it.thisObject as FrameLayout
            // 调用 findViewById 并转换为 TextView
            val linearLayout =
                ViewUtils.getViewById(
                    "com.mutangtech.qianji.R\$id",
                    obj,
                    classLoader,
                    "main_drawer_content_layout"
                ) as LinearLayout
            runCatching {
                hookerManifest.attachResource(activity)
                // 找到了obj里面的name字段
                addSettingMenu(linearLayout, activity)
            }.onFailure {
                hookerManifest.logE(it)
            }
            true
        }
    }


    lateinit var hookerManifest: HookerManifest
    lateinit var itemMenuBinding: MenuItemBinding

    /**
     * 检查服务状态
     */
    private suspend fun checkServerStatus(activity: Activity) = withContext(Dispatchers.IO) {
        if (!::hookerManifest.isInitialized || !::itemMenuBinding.isInitialized) {
            return@withContext
        }
        val background =
            if (ServerInfo.isServerStart()) R.drawable.status_running else R.drawable.status_stopped

        withContext(Dispatchers.Main) {
            itemMenuBinding.serviceStatus.background =
                AppCompatResources.getDrawable(activity, background)
        }
    }

    private fun addSettingMenu(
        linearLayout: LinearLayout,
        context: Activity,
    ) {
        if (!::hookerManifest.isInitialized) {
            return
        }
        val mainColor = ColorUtils.getMainColor(context)
        val subColor = ColorUtils.getSubColor(context)
        val backgroundColor = ColorUtils.getBackgroundColor(context)

        itemMenuBinding = MenuItemBinding.inflate(LayoutInflater.from(context))
        itemMenuBinding.root.setBackgroundColor(backgroundColor)

        itemMenuBinding.title.text = context.getString(R.string.app_name)
        itemMenuBinding.title.setTextColor(mainColor)

        itemMenuBinding.version.text = BuildConfig.VERSION_NAME.replace(" - Xposed", "")
        itemMenuBinding.version.setTextColor(subColor)
        itemMenuBinding.root.setOnClickListener {
            MessageUtils.toast("强制同步数据中...")
            //强制同步
            last = 0L
            syncData2Auto(context)
        }

        linearLayout.addView(itemMenuBinding.root)

        ThreadUtils.launch {
            checkServerStatus(context)
        }
    }

    private var last = 0L

    /**
     * 同步数据到自动记账
     */
    private fun syncData2Auto(context: Activity) {
        // 最快3秒同步一次
        if (System.currentTimeMillis() - last < 1000 * 3) {
            hookerManifest.log("Sync too fast, ignore")
            return
        }
        last = System.currentTimeMillis()
        ThreadUtils.launch {
            AssetsUtils(hookerManifest, context.classLoader).syncAssets()
            val books = BookUtils(hookerManifest, context.classLoader, context).syncBooks()
            CategoryUtils(hookerManifest, context.classLoader, books).syncCategory()
            BaoXiaoUtils(hookerManifest, context.classLoader).syncBaoXiao()
            // LoanUtils(hookerManifest, context.classLoader).syncLoan()
            SyncBillUtils(hookerManifest, context.classLoader).sync(context)
        }
    }

}