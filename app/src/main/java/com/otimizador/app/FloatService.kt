package com.otimizador.app

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.view.*
import android.widget.*

class FloatService : Service() {
    private var root: View? = null
    private var revert: Runnable? = null
    private val ui = Handler(Looper.getMainLooper())
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun bg(c: String, r: Int = 12) = GradientDrawable().apply { setColor(Color.parseColor(c)); cornerRadius = dp(r).toFloat() }
    private fun tv(t: String, c: String, s: Float) = TextView(this).apply { text = t; setTextColor(Color.parseColor(c)); textSize = s }
    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("nx", "NovaX", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "nx").setContentTitle("NovaX: painel flutuante").setSmallIcon(android.R.drawable.ic_media_play).build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(2, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) else startForeground(2, n)
        if (root == null) show()
        return START_STICKY
    }

    private fun show() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val lp = WindowManager.LayoutParams(dp(300), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT)
        lp.gravity = Gravity.TOP or Gravity.START; lp.x = dp(16); lp.y = dp(100)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = bg("#EE101016", 18).also { it.setStroke(dp(1), Color.parseColor("#FF2A2A")) }
            setPadding(dp(10), dp(8), dp(10), dp(10))
        }
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val status = tv("Toque numa opção para aplicar.", "#9A9AA8", 11f)
        val confirm = Button(this).apply { text = "CONFIRMAR (volta sozinho em 12 s)"; visibility = View.GONE }
        confirm.setOnClickListener { revert?.let { r -> ui.removeCallbacks(r) }; confirm.visibility = View.GONE; status.text = "Resolução mantida." }

        fun exec(label: String, cmd: String, safe: Boolean = false) {
            status.text = "$label..."
            Thread { val r = sh(cmd, FF); ui.post { status.text = "$label: ${r.take(70)}" } }.start()
            if (safe) {
                confirm.visibility = View.VISIBLE
                revert?.let { r -> ui.removeCallbacks(r) }
                val r = Runnable { confirm.visibility = View.GONE; Thread { sh("wm size reset", FF) }.start(); status.text = "Voltou ao normal." }
                revert = r; ui.postDelayed(r, 12000)
            }
        }

        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { addView(content) }
        fun note(t: String) { content.addView(tv(t, "#9A9AA8", 11f).apply { setPadding(0, dp(8), 0, 0) }) }
        fun item(t: String, s: String, a: () -> Unit) {
            val c = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = bg("#1C1C26"); setPadding(dp(12), dp(8), dp(12), dp(8)); setOnClickListener { a() } }
            c.addView(tv(t, "#FFFFFF", 14f)); if (s.isNotEmpty()) c.addView(tv(s, "#9A9AA8", 11f))
            content.addView(c, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(4), 0, 0) })
        }
        fun tabRes() {
            content.removeAllViews()
            item("Nativo (resetar)", "Volta à resolução original do celular") { exec("Resolução nativa", "wm size reset") }
            listOf("RedMagic 10" to "1080x2340", "Realme C75" to "1080x2400", "Galaxy A54" to "1216x2688", "Galaxy S20 Ultra" to "1440x3088").forEach { (n, r) ->
                item(n, r.replace('x', '×')) { exec(n, "wm size $r", true) }
            }
            listOf(80, 70, 60, 50).forEach { p -> item("$p% do nativo", "Menos pixels, mais FPS") { exec("$p%", res(p), true) } }
            note("Resolução maior que a do seu celular deixa o jogo mais pesado e pode esticar a imagem.")
        }
        fun tabHz() {
            content.removeAllViews()
            listOf(144, 120, 90, 60).forEach { h -> item("$h FPS", "Limite de $h Hz da tela") { exec("Frame rate $h", hz("$h.0")) } }
            item("Padrão do aparelho", "Remove o limite") { exec("Frame rate padrão", HZOFF) }
            note("Define o limite de Hz da tela. O FPS dentro do jogo depende do jogo e do celular.")
        }
        fun tabGfx() {
            content.removeAllViews()
            item("Vulkan ligado", "Pode dar mais FPS. Reabra o jogo") { exec("Vulkan", "setprop debug.hwui.renderer skiavk") }
            item("Vulkan desligado", "Volta ao padrão") { exec("Vulkan off", "setprop debug.hwui.renderer ''") }
            item("Animações desligadas", "Sistema mais leve") { exec("Animações off", anim("0")) }
            item("Animações normais", "Volta ao padrão") { exec("Animações", anim("1")) }
            item("Sem blur", "Desliga o desfoque de janelas") { exec("Sem blur", "settings put global disable_window_blurs 1") }
            item("Desempenho fixo", "Mais FPS estável, esquenta mais") { exec("Desempenho fixo", "cmd power set-fixed-performance-mode-enabled true") }
            item("Fechar apps ao fundo", "Libera RAM agora") { exec("Fechar apps", "am kill-all") }
        }

        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf<Pair<String, () -> Unit>>("RESOLUÇÃO" to { tabRes() }, "FRAME RATE" to { tabHz() }, "GRÁFICO" to { tabGfx() }).forEach { (n, a) ->
            tabs.addView(tv(n, "#FFFFFF", 11f).apply { gravity = Gravity.CENTER; background = bg("#2A2A35"); setPadding(dp(4), dp(8), dp(4), dp(8)); setOnClickListener { a() } },
                LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(2), 0, dp(2), 0) })
        }
        body.addView(tabs); body.addView(scroll, LinearLayout.LayoutParams(-1, dp(280))); body.addView(status); body.addView(confirm)
        tabRes()

        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(tv("NOVAX", "#FF2A2A", 16f).apply { typeface = Typeface.DEFAULT_BOLD }, LinearLayout.LayoutParams(0, -2, 1f))
        head.addView(tv("—", "#FFFFFF", 18f).apply { setPadding(dp(10), 0, dp(10), 0); setOnClickListener { body.visibility = if (body.visibility == View.VISIBLE) View.GONE else View.VISIBLE } })
        head.addView(tv("✕", "#FFFFFF", 18f).apply { setPadding(dp(10), 0, dp(4), 0); setOnClickListener { stopSelf() } })
        var dx = 0f; var dy = 0f; var sx = 0; var sy = 0
        head.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = lp.x; sy = lp.y; dx = e.rawX; dy = e.rawY }
                MotionEvent.ACTION_MOVE -> { lp.x = sx + (e.rawX - dx).toInt(); lp.y = sy + (e.rawY - dy).toInt(); wm.updateViewLayout(box, lp) }
            }
            true
        }
        box.addView(head); box.addView(body)
        wm.addView(box, lp); root = box
    }

    override fun onDestroy() {
        revert?.let { ui.removeCallbacks(it) }
        root?.let { try { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it) } catch (e: Throwable) {} }
        super.onDestroy()
    }
}
