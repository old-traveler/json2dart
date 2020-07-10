package com.hyc.feedback

import java.text.SimpleDateFormat
import java.util.*

/**
 *
 * Created by Seal.Wu on 2017/9/25.
 */

const val ACTION_FORMAT_JSON = "action_format_json"
const val ACTION_CLICK_PROJECT_URL = "action_click_project_url"



data class FormatJSONAction(
        val uuid: String = "",
        val pluginVersion: String = "1.X",
        val actionType: String = ACTION_FORMAT_JSON,
        val time: String = Date().time.toString(),
        val daytime: String = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())
)

data class ClickProjectURLAction(
        val uuid: String = "",
        val pluginVersion: String = "1.X",
        val actionType: String = ACTION_CLICK_PROJECT_URL,
        val time: String = Date().time.toString(),
        val daytime: String = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())
)