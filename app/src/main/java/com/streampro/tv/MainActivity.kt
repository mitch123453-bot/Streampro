package com.streampro.tv

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.graphics.Color
import android.text.InputType
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class MainActivity : Activity() {
    private val bg = Color.rgb(9, 13, 24)
    private val panel = Color.rgb(23, 31, 50)
    private val purple = Color.rgb(139, 92, 246)
    private lateinit var root: LinearLayout
    private var player: ExoPlayer? = null
    private var playerView: PlayerView? = null
    private var account: XtreamAccount? = null
    private var client: XtreamClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        showLogin()
    }
    private fun base(title: String) {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(36, 24, 36, 24)
            setBackgroundColor(bg)
        }
        val heading = TextView(this).apply {
            text = title; textSize = 30f; setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 22)
        }
        root.addView(heading)
        setContentView(root)
    }
    private fun field(hint: String, secret: Boolean = false): EditText =
        EditText(this).apply {
            this.hint = hint; textSize = 18f; setTextColor(Color.WHITE); setHintTextColor(0xFF9AA6BD.toInt())
            setSingleLine(true); setPadding(18, 12, 18, 12); setBackgroundColor(panel)
            if (secret) inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            root.addView(this, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 62).apply { bottomMargin = 12 })
        }
    private fun button(label: String, action: () -> Unit) {
        val b = Button(this).apply { text = label; textSize = 17f; isAllCaps = false; setTextColor(Color.WHITE); setBackgroundColor(purple); setOnClickListener { action() } }
        root.addView(b, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 60).apply { topMargin = 10; bottomMargin = 8 })
    }
    private fun message(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }
    private fun showLogin() {
        base("STREAMPRO")
        val sub = TextView(this).apply { text = "Connect your IPTV subscription"; textSize = 18f; setTextColor(0xFFB8C2D8.toInt()) }
        root.addView(sub)
        val url = field("Server URL (e.g. http://server:8080)")
        val user = field("Username")
        val pass = field("Password", true)
        button("CONNECT") {
            val u = url.text.toString().trim().trimEnd('/')
            val name = user.text.toString().trim()
            val pw = pass.text.toString()
            if (!(u.startsWith("http://") || u.startsWith("https://")) || name.isBlank() || pw.isBlank()) {
                message("Enter a valid server URL, username and password"); return@button
            }
            account = XtreamAccount(u, name, pw)
            client = XtreamClient(account!!)
            message("Connecting…")
            client!!.authenticate { ok, error -> runOnUiThread {
                if (ok) loadChannels() else message(error ?: "Login failed")
            } }
        }
        val note = TextView(this).apply {
            text = "Use details supplied by your authorised IPTV provider. StreamPro does not provide TV channels."
            textSize = 13f; setTextColor(0xFF8995AB.toInt()); setPadding(0, 20, 0, 0)
        }
        root.addView(note)
    }
    private fun loadChannels() {
        base("LIVE TV")
        val status = TextView(this).apply { text = "Loading your channels…"; textSize = 18f; setTextColor(Color.WHITE) }
        root.addView(status)
        client!!.liveChannels { channels, error -> runOnUiThread {
            if (channels == null) { status.text = error ?: "Could not load channels"; return@runOnUiThread }
            root.removeView(status)
            val list = ListView(this).apply {
                setBackgroundColor(bg); divider = null; dividerHeight = 4
                adapter = object : ArrayAdapter<TvChannel>(this@MainActivity, android.R.layout.simple_list_item_1, channels) {
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        return TextView(this@MainActivity).apply {
                            text = "▶   ${channels[position].name}"; textSize = 20f
                            setTextColor(Color.WHITE); setPadding(18, 18, 18, 18); setBackgroundColor(panel)
                        }
                    }
                }
                setOnItemClickListener { _, _, position, _ -> play(channels[position]) }
            }
            root.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
            button("LOG OUT") { player?.release(); player = null; account = null; showLogin() }
        } }
    }
    private fun play(channel: TvChannel) {
        val url = client!!.liveUrl(channel)
        root.removeAllViews()
        root.orientation = LinearLayout.VERTICAL
        val label = TextView(this).apply { text = channel.name; textSize = 20f; setTextColor(Color.WHITE) }
        root.addView(label)
        playerView = PlayerView(this).apply { useController = true; setBackgroundColor(Color.BLACK) }
        root.addView(playerView, LinearLayout.LayoutParams(-1, 0, 1f))
        player?.release()
        player = ExoPlayer.Builder(this).build().also { exo ->
            playerView?.player = exo
            exo.setMediaItem(MediaItem.fromUri(url))
            exo.prepare()
            exo.playWhenReady = true
        }
        val back = Button(this).apply { text = "BACK TO CHANNELS"; isAllCaps = false; setOnClickListener { player?.release(); player = null; loadChannels() } }
        root.addView(back)
    }
    override fun onStop() { super.onStop(); player?.pause() }
    override fun onDestroy() { player?.release(); player = null; super.onDestroy() }
}
