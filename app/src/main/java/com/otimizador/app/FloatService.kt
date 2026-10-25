package com.otimizador.app

import android.app.*
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.view.*
import android.widget.*

class FloatService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var pr: SharedPreferences
    private var bubbleV: View? = null
    private var panelV: View? = null
    private var revert: Runnable? = null
    private var px = 0
    private var py = 0
    private val ui = Handler(Looper.getMainLooper())
    private val red = "#FF2A2A"
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun bg(c: String, r: Int = 12) = GradientDrawable().apply { setColor(Color.parseColor(c)); cornerRadius = dp(r).toFloat() }
    private fun tv(t: String, c: String, s: Float) = TextView(this).apply { text = t; setTextColor(Color.parseColor(c)); textSize = s }
    private fun game() = pr.getString("g", FF) ?: FF
    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("nx", "NovaX", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "nx").setContentTitle("NovaX: bolinha ativa").setSmallIcon(android.R.drawable.ic_media_play).build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(2, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) else startForeground(2, n)
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        pr = getSharedPreferences("nx", 0)
        if (bubbleV == null && panelV == null) { px = dp(12); py = dp(160); showBubble() }
        return START_STICKY
    }

    private fun hideBubble() { bubbleV?.let { try { wm.removeView(it) } catch (e: Throwable) {} }; bubbleV = null }
    private fun hidePanel() { panelV?.let { try { wm.removeView(it) } catch (e: Throwable) {} }; panelV = null }

    private fun showBubble() {
        hidePanel()
        val sz = dp(pr.getInt("bsz", 64))
        val lp = WindowManager.LayoutParams(sz, sz, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT)
        lp.gravity = Gravity.TOP or Gravity.START; lp.x = px; lp.y = py
        val iv = ImageView(this).apply {
            setImageResource(R.drawable.ic_launcher)
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.BLACK); setStroke(dp(2), Color.parseColor(red)) }
            clipToOutline = true
            outlineProvider = ViewOutlineProvider.BACKGROUND
            alpha = pr.getInt("balpha", 100) / 100f
        }
        var dx = 0f; var dy = 0f; var sx = 0; var sy = 0; var moved = false
        iv.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = lp.x; sy = lp.y; dx = e.rawX; dy = e.rawY; moved = false }
                MotionEvent.ACTION_MOVE -> {
                    if (Math.abs(e.rawX - dx) > dp(6) || Math.abs(e.rawY - dy) > dp(6)) moved = true
                    if (moved) { lp.x = sx + (e.rawX - dx).toInt(); lp.y = sy + (e.rawY - dy).toInt(); wm.updateViewLayout(iv, lp) }
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) { px = lp.x; py = lp.y; showPanel() }
                    else {
                        val w = resources.displayMetrics.widthPixels
                        lp.x = if (lp.x + sz / 2 < w / 2) 0 else w - sz
                        px = lp.x; py = lp.y; wm.updateViewLayout(iv, lp)
                    }
                }
            }
            true
        }
        wm.addView(iv, lp); bubbleV = iv
    }

    private fun showPanel() {
        hideBubble()
        val lp = WindowManager.LayoutParams(dp(320), WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT)
        lp.gravity = Gravity.TOP or Gravity.START
        lp.x = px.coerceAtMost(resources.displayMetrics.widthPixels - dp(330)).coerceAtLeast(0); lp.y = py
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = bg("#F2101016", 20).also { it.setStroke(dp(1), Color.parseColor(red)) }
            setPadding(dp(10), dp(8), dp(10), dp(10))
            alpha = pr.getInt("palpha", 100) / 100f
        }
        val status = tv("Toque numa opção para aplicar.", "#9A9AA8", 11f)
        val confirm = Button(this).apply { text = "CONFIRMAR (volta sozinho em 12 s)"; visibility = View.GONE }
        confirm.setOnClickListener { revert?.let { r -> ui.removeCallbacks(r) }; confirm.visibility = View.GONE; status.text = "Resolução mantida." }

        fun exec(label: String, cmd: String, safe: Boolean = false) {
            status.text = "$label..."
            val gm = game()
            Thread { val r = sh(cmd, gm); ui.post { status.text = "$label: ${r.take(80)}" } }.start()
            if (safe) {
                confirm.visibility = View.VISIBLE
                revert?.let { r -> ui.removeCallbacks(r) }
                val r = Runnable { confirm.visibility = View.GONE; Thread { sh("wm size reset", gm) }.start(); status.text = "Voltou ao normal." }
                revert = r; ui.postDelayed(r, 12000)
            }
        }

        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { addView(content) }
        fun note(t: String) { content.addView(tv(t, "#9A9AA8", 11f).apply { setPadding(0, dp(8), 0, 0) }) }
        fun item(t: String, s: String, a: () -> Unit) {
            val c = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = bg("#1C1C26", 14); setPadding(dp(12), dp(8), dp(12), dp(8)); setOnClickListener { a() } }
            c.addView(tv(t, "#FFFFFF", 14f)); if (s.isNotEmpty()) c.addView(tv(s, "#9A9AA8", 11f))
            content.addView(c, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(4), 0, 0) })
        }
        fun bar(label: String, key: String, lo: Int, hi: Int, def: Int, fn: (Int) -> Unit) {
            content.addView(tv(label, "#FFFFFF", 12f).apply { setPadding(0, dp(10), 0, 0) })
            val sb = SeekBar(this)
            sb.max = hi - lo
            sb.progress = pr.getInt(key, def) - lo
            sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) {}
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) { val v = (s?.progress ?: 0) + lo; pr.edit().putInt(key, v).apply(); fn(v) }
            })
            content.addView(sb)
        }

        fun tabRes() {
            content.removeAllViews()
            item("📱 Nativo (resetar)", "Volta à resolução original") { exec("Resolução nativa", "wm size reset") }
            listOf("RedMagic 10" to "1080x2340", "Realme C75" to "1080x2400", "Galaxy A54" to "1216x2688", "Galaxy S20 Ultra" to "1440x3088").forEach { (n, r) ->
                item("📱 $n", r.replace('x', '×')) { exec(n, "wm size $r", true) }
            }
            listOf(80, 70, 60, 50).forEach { p -> item("⚡ $p% do nativo", "Menos pixels, mais FPS") { exec("$p%", res(p), true) } }
            note("Resolução maior que a do seu celular deixa o jogo mais pesado e pode esticar a imagem.")
        }
        fun tabFps() {
            content.removeAllViews()
            listOf(144, 120, 90, 60).forEach { h -> item("🎞 $h FPS", "Limite de $h Hz da tela") { exec("Frame rate $h", hz("$h.0")) } }
            item("↩ Padrão do aparelho", "Remove o limite") { exec("Frame rate padrão", HZOFF) }
            note("Define o limite de Hz da tela. O FPS dentro do jogo depende do jogo e do celular.")
        }
        fun tabGfx() {
            content.removeAllViews()
            item("🎨 Vulkan ligado", "Pode dar mais FPS. Reabra o jogo") { exec("Vulkan", "setprop debug.hwui.renderer skiavk") }
            item("↩ Vulkan desligado", "Volta ao padrão") { exec("Vulkan off", "setprop debug.hwui.renderer ''") }
            item("✨ Animações desligadas", "Sistema mais leve") { exec("Animações off", anim("0")) }
            item("↩ Animações normais", "Volta ao padrão") { exec("Animações", anim("1")) }
            item("🌫 Sem blur", "Desliga o desfoque de janelas") { exec("Sem blur", "settings put global disable_window_blurs 1") }
            item("🔥 Desempenho fixo", "FPS estável, esquenta mais") { exec("Desempenho fixo", "cmd power set-fixed-performance-mode-enabled true") }
            item("❄ Desempenho normal", "Volta ao padrão") { exec("Desempenho normal", "cmd power set-fixed-performance-mode-enabled false") }
            item("🧱 Sem overlays de hardware", "Pode corrigir falhas de imagem") { exec("Sem overlays", "service call SurfaceFlinger 1008 i32 1") }
        }
        fun tabNet() {
            content.removeAllViews()
            listOf("Cloudflare" to "one.one.one.one", "Google" to "dns.google", "AdGuard" to "dns.adguard-dns.com").forEach { (n, h) ->
                item("🌐 DNS $n", h) { exec("DNS $n", "settings put global private_dns_mode hostname; settings put global private_dns_specifier $h; echo ok") }
            }
            item("↩ DNS automático", "Volta ao do provedor") { exec("DNS automático", AUTO) }
            item("📡 Ping do jogo", "Média e perda de pacote") { exec("Ping", "ping -c 5 -W 1 8.8.8.8 | tail -2") }
            item("⚡ Wi-Fi baixa latência", "Android 12 ou mais") { exec("Wi-Fi baixa latência", "cmd wifi force-low-latency-mode enabled") }
        }
        fun tabTouch() {
            content.removeAllViews()
            item("👆 Ponteiro +3", "Mais rápido") { exec("Ponteiro +3", "settings put system pointer_speed 3") }
            item("👆 Ponteiro +6", "Bem rápido, pode ficar sensível") { exec("Ponteiro +6", "settings put system pointer_speed 6") }
            item("↩ Ponteiro normal", "Volta a 0") { exec("Ponteiro normal", "settings put system pointer_speed 0") }
            item("⏱ Toque longo 250 ms", "Reconhece o toque segurado mais cedo") { exec("Toque longo", "settings put secure long_press_timeout 250") }
            item("↩ Toque longo normal", "400 ms") { exec("Toque longo normal", "settings put secure long_press_timeout 400") }
            item("🔴 Mostrar toques", "Bolinha onde você toca") { exec("Mostrar toques", "settings put system show_touches 1") }
            item("📍 Mostrar X e Y", "Coordenadas do dedo no topo") { exec("X e Y", "settings put system pointer_location 1") }
            item("↩ Esconder toques e X e Y", "") { exec("Esconder", "settings put system show_touches 0; settings put system pointer_location 0") }
        }
        fun tabTools() {
            content.removeAllViews()
            item("🎮 Modo Jogo ligar", "Hz, desempenho, sem animações") { exec("Modo Jogo", autoOn) }
            item("↩ Modo Jogo desligar", "Volta ao normal") { exec("Modo Jogo off", autoOff) }
            item("🚀 Modo Emulador", "Resolução 70% e tudo leve") { exec("Modo Emulador", emu[0].on) }
            item("↩ Sair do Modo Emulador", "") { exec("Sair do Emulador", emu[0].off ?: "wm size reset") }
            item("🧹 Fechar apps ao fundo", "Libera RAM agora") { exec("Fechar apps", "am kill-all") }
            item("🗑 Limpar cache de tudo", "") { exec("Limpar cache", "pm trim-caches 999G") }
            item("📸 Captura de tela", "Salva em /sdcard/NovaX") { exec("Captura", "mkdir -p /sdcard/NovaX; screencap -p /sdcard/NovaX/print_$(date +%s).png; echo salvo") }
            item("🔕 Não perturbe ligar", "") { exec("Não perturbe", "cmd notification set_dnd on") }
            item("🔔 Não perturbe desligar", "") { exec("Não perturbe off", "cmd notification set_dnd off") }
            item("♻ Desfazer tudo", "Devolve o padrão") { exec("Desfazer tudo", painel[3].on) }
        }
        fun tabSet() {
            content.removeAllViews()
            bar("Tamanho da bolinha", "bsz", 40, 120, 64) { status.text = "Tamanho salvo. Minimize para ver." }
            bar("Transparência da bolinha", "balpha", 30, 100, 100) { status.text = "Salvo. Minimize para ver." }
            bar("Transparência do painel", "palpha", 40, 100, 100) { v -> box.alpha = v / 100f }
            item("📲 Abrir o NovaX", "") { packageManager.getLaunchIntentForPackage(packageName)?.let { startActivity(it) } }
            item("✕ Fechar a bolinha", "Desliga o painel flutuante") { stopSelf() }
        }

        val names = listOf("RESOLUÇÃO", "FPS", "GRÁFICO", "REDE", "TOQUE", "FERRAM.", "AJUSTES")
        val acts = listOf<() -> Unit>({ tabRes() }, { tabFps() }, { tabGfx() }, { tabNet() }, { tabTouch() }, { tabTools() }, { tabSet() })
        val chips = names.map { n -> tv(n, "#FFFFFF", 11f).apply { gravity = Gravity.CENTER; setPadding(dp(10), dp(7), dp(10), dp(7)); background = bg("#2A2A35", 16) } }
        fun pick(i: Int) { chips.forEachIndexed { j, c -> c.background = bg(if (j == i) red else "#2A2A35", 16) }; acts[i]() }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        chips.forEachIndexed { i, c -> c.setOnClickListener { pick(i) }; row.addView(c, LinearLayout.LayoutParams(-2, -2).apply { setMargins(dp(2), 0, dp(2), 0) }) }
        val tabs = HorizontalScrollView(this).apply { addView(row); isHorizontalScrollBarEnabled = false }

        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        body.addView(tabs); body.addView(scroll, LinearLayout.LayoutParams(-1, dp(300))); body.addView(status); body.addView(confirm)

        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 0, 0, dp(6)) }
        head.addView(tv("NOVAX", red, 16f).apply { typeface = Typeface.DEFAULT_BOLD }, LinearLayout.LayoutParams(0, -2, 1f))
        head.addView(tv("▾", "#FFFFFF", 22f).apply { setPadding(dp(14), 0, dp(14), 0); setOnClickListener { px = lp.x; py = lp.y; showBubble() } })
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
        wm.addView(box, lp); panelV = box
        pick(0)
    }

    override fun onDestroy() {
        revert?.let { ui.removeCallbacks(it) }
        hideBubble(); hidePanel()
        super.onDestroy()
    }
}
