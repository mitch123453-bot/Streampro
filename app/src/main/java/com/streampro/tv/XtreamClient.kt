package com.streampro.tv

import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

data class XtreamAccount(val baseUrl: String, val username: String, val password: String)
data class TvChannel(val name: String, val streamId: String, val categoryId: String, val icon: String?)

class XtreamClient(private val account: XtreamAccount) {
    private val http = OkHttpClient()
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private fun api(action: String? = null): String {
        val root = account.baseUrl.trimEnd('/')
        val query = "username=${enc(account.username)}&password=${enc(account.password)}"
        return "$root/player_api.php?$query" + (action?.let { "&action=$it" } ?: "")
    }
    fun authenticate(done: (Boolean, String?) -> Unit) {
        val req = Request.Builder().url(api()).build()
        http.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = done(false, e.message)
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) { done(false, "Server returned HTTP ${it.code}"); return }
                    try {
                        val json = JSONObject(it.body?.string().orEmpty())
                        val info = json.optJSONObject("user_info")
                        val ok = info?.optString("auth") == "1" &&
                            info.optString("status").equals("Active", true)
                        done(ok, if (ok) null else "Login rejected or subscription inactive")
                    } catch (e: Exception) { done(false, "Invalid server response") }
                }
            }
        })
    }
    fun liveChannels(done: (List<TvChannel>?, String?) -> Unit) {
        val req = Request.Builder().url(api("get_live_streams")).build()
        http.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = done(null, e.message)
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) { done(null, "HTTP ${it.code}"); return }
                    try {
                        val arr = JSONArray(it.body?.string().orEmpty())
                        val items = (0 until arr.length()).map { i ->
                            val o = arr.getJSONObject(i)
                            TvChannel(o.optString("name", "Channel ${i+1}"),
                                o.optString("stream_id"), o.optString("category_id"),
                                o.optString("stream_icon").takeIf { s -> s.isNotBlank() })
                        }
                        done(items, null)
                    } catch (e: Exception) { done(null, "Could not read channel list") }
                }
            }
        })
    }
    fun liveUrl(channel: TvChannel): String =
        "${account.baseUrl.trimEnd('/')}/live/${enc(account.username)}/${enc(account.password)}/${enc(channel.streamId)}.ts"
}
