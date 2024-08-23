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

package org.ezbook.server.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import org.ezbook.server.Server
import org.ezbook.server.constant.LogLevel
import org.ezbook.server.db.Db

@Entity
class LogModel {
    @PrimaryKey(autoGenerate = true)
    var id = 0

    var level = LogLevel.DEBUG

    var app = ""

    var location = ""

    var message = ""

    var time = System.currentTimeMillis()

    companion object{
       suspend fun add(level: LogLevel, app: String, location: String, message: String) {
            val log = LogModel()
            log.level = level
            log.app = app
            log.location = location
            log.message = message
            Server.request("log/add", Gson().toJson(log))
        }

       suspend fun list(page: Int = 1, limit: Int = 10): List<LogModel> {
            val response = Server.request("log/list?page=$page&limit=$limit")
            return Gson().fromJson(response, Array<LogModel>::class.java).toList()
        }

       suspend fun clear() {
            Server.request("log/clear")
        }
    }

}