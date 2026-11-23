package com.streampro.tv

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlin.math.min

class MainActivity : Activity() {
    private val bg = Color.rgb(7, 11, 20)
    private val panel = Color.rgb(18, 25, 42)
    private val fieldBg = Color.rgb(27, 36, 57)
    private val purple = Color.rgb(126, 87, 255)
    private val muted = Color.rgb(166, 178, 202)
    private lateinit var root: LinearLayout
    private var player: ExoPlayer? = null
    private var playerView: PlayerView? = null
    private var account: XtreamAccount? = null
    private var client: XtreamClient? = null

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        showLogin()
    }

    private fun rounded(color: Int, radius: Int = 14, stroke: Int = 0): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (stroke != 0) setStroke(dp(1), stroke)
        }

    private fun base(title: String) {
        val host = FrameLayout(this).apply { setBackgroundColor(bg) }
        val screenWidthDp = resources.displayMetrics.widthPixels / resources.displayMetrics.density
        val panelWidth = min(560f, screenWidthDp - 32f).toInt()

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP
            setPadding(dp(22), dp(28), dp(22), dp(22))
            background = rounded(panel, 24)
        }
        host.addView(root, FrameLayout.LayoutParams(dp(panelWidth), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER))
        setContentView(host)

        val heading = TextView(this).apply {
            text = title
            textSize = 27f
            letterSpacing = 0.06f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, dp(10))
        }
        root.addView(heading)
    }

    private fun field(hint: String, secret: Boolean = false): EditText =
        EditText(this).apply {
            this.hint = hint
            textSize = 16f
            setTextColor(Color.WHITE)
            setHintTextColor(muted)
            setSingleLine(true)
            setPadding(dp(16), 0, dp(16), 0)
            background = rounded(fieldBg, 12, Color.rgb(48, 60, 87))
            minHeight = dp(56)
            if (secret) inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            root.addView(this, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)).apply {
                bottomMargin = dp(12)
            })
        }

    private fun button(label: String, action: () -> Unit) {
        val b = TextView(this).apply {
            text = label
            textSize = 16f
            gravity = Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = rounded(purple, 14)
            isClickable = true
            isFocusable = true
            setPadding(dp(16), 0, dp(16), 0)
            setOnClickListener { action() }
        }
        root.addView(b, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)).apply {
            topMargin = dp(6)
            bottomMargin = dp(8)
        })
    }

    private fun message(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }

    private fun showLogin() {
        base("STREAMPRO")
        val sub = TextView(this).apply {
            text = "Your entertainment, in one place"
            textSize = 16f
            setTextColor(muted)
            setPadding(0, 0, 0, dp(24))
        }
        root.addView(sub)

        val url = field("Server URL")
        val user = field("Username")
        val pass = field("Password", true)

        button("CONNECT") {
            val u = url.text.toString().trim().trimEnd('/')
            val name = user.text.toString().trim()
            val pw = pass.text.toString()
            if (!(u.startsWith("http://") || u.startsWith("https://")) || name.isBlank() || pw.isBlank()) {
                message("Enter your server URL, username and password")
                return@button
            }
            account = XtreamAccount(u, name, pw)
            client = XtreamClient(account!!)
            message("Connecting…")
            client!!.authenticate { ok, error -> runOnUiThread {
                if (ok) loadChannels() else message(error ?: "Login failed")
            } }
        }

        val note = TextView(this).apply {
            text = "Use the login details from your authorised IPTV provider. StreamPro does not provide channels."
            textSize = 12f
            setTextColor(muted)
            setPadding(0, dp(16), 0, 0)
            gravity = Gravity.CENTER
        }
        root.addView(note, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun loadChannels() {
        base("LIVE TV")
        val status = TextView(this).apply {
            text = "Loading your channels…"
            textSize = 16f
            setTextColor(Color.WHITE)
        }
        root.addView(status)
        client!!.liveChannels { channels, error -> runOnUiThread {
            if (channels == null) {
                status.text = error ?: "Could not load channels"
                return@runOnUiThread
            }
            root.removeView(status)
            val list = ListView(this).apply {
                setBackgroundColor(bg)
                divider = null
                dividerHeight = dp(8)
                adapter = object : ArrayAdapter<TvChannel>(this@MainActivity, android.R.layout.simple_list_item_1, channels) {
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        return TextView(this@MainActivity).apply {
                            text = "▶   ${channels[position].name}"
                            textSize = 17f
                            setTextColor(Color.WHITE)
                            setPadding(dp(16), dp(16), dp(16), dp(16))
                            background = rounded(fieldBg, 12)
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
        val label = TextView(this).apply {
            text = channel.name
            textSize = 18f
            setTextColor(Color.WHITE)
        }
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
        button("BACK TO CHANNELS") { player?.release(); player = null; loadChannels() }
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }
}
