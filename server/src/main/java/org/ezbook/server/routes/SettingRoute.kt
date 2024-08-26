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

package org.ezbook.server.routes

import com.google.gson.Gson
import fi.iki.elonen.NanoHTTPD
import org.ezbook.server.Server
import org.ezbook.server.db.Db
import org.ezbook.server.db.model.SettingModel

class SettingRoute(private val session: NanoHTTPD.IHTTPSession) {
    fun get(): NanoHTTPD.Response {
        val params = session.parameters
        val key = params["key"]?.firstOrNull()?.toString() ?: ""
        if (key === "") {
            return Server.json(400, "key is required")
        }
        val data = Db.get().settingDao().query(key)
        return Server.json(200, "OK", data)
    }

    fun set(): NanoHTTPD.Response {
        val model = Gson().fromJson(Server.reqData(session), SettingModel::class.java)
        val data = Db.get().settingDao().query(model.key)
        if (data != null) {
            Db.get().settingDao().update(model)
        } else {
            Db.get().settingDao().insert(model)
        }
        return Server.json(200, "OK")

    }

}