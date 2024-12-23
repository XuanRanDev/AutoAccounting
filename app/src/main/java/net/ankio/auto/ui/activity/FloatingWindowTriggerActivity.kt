/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
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

package net.ankio.auto.ui.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import net.ankio.auto.R
import net.ankio.auto.service.FloatingWindowService
import net.ankio.auto.storage.Logger


class FloatingWindowTriggerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createOnePxWindow()
        setTheme(R.style.TransparentActivityTheme)
    }

    private fun createOnePxWindow() {
        val window = window
        window.setGravity(Gravity.START or Gravity.TOP)
        val layoutParams = window.attributes
        layoutParams.x = 0
        layoutParams.y = 0
        layoutParams.width = 1
        layoutParams.height = 1
        layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        window.attributes = layoutParams
    }

    override fun onStart() {
        super.onStart()
        startFloatService(intent)
    }

    private fun startFloatService(intent: Intent){
        // 启动服务,传递intent
        val serviceIntent = Intent(this, FloatingWindowService::class.java).apply {
            intent.extras?.let { putExtras(it) } // 直接传递所有 extras
        }
        try {
            startService(serviceIntent)
        }catch (e: Exception){
            e.printStackTrace()
            Logger.e("startFloatService error: ${e.message}",e)
        }
        // 关闭 Activity
        exitActivity()
    }

    private fun exitActivity() {
        finishAffinity()
        overridePendingTransition(0, 0)
        window.setWindowAnimations(0)
    }
}
