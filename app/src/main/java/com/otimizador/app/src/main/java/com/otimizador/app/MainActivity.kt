package com.otimizador.app

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import rikka.shizuku.Shizuku

const val FF = "com.dts.freefireth"
const val MAX = "com.dts.freefiremax"
const val AUTO = "settings put global private_dns_mode opportunistic; echo ok"

/** '@' vira '$' e %G vira o pacote do jogo escolhido. */
fun sh(cmd: String, g: String): String = try {
    val c = cmd.replace('@', '$').replace("%G", g)
    val m = Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
    m.isAccessible = true
    val p = m.invoke(null, arrayOf("sh", "-c", "$c 2>&1"), null, null) as Process
    val o = p.inputStream.bufferedReader().readText(); p.waitFor()
    o.trim().ifEmpty { "Concluído." }.take(2500)
} catch (e: Throwable) { "Erro: ${e.message}" }

fun running() = try { Shizuku.pingBinder() } catch (e: Throwable) { false }
fun granted() = try { running() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED } catch (e: Throwable) { false }

fun info(c: Context): String {
    val mi = ActivityManager.MemoryInfo()
    (c.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
    val b = c.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val t = (b?.getIntExtra("temperature", 0) ?: 0) / 10f
    return "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} · RAM livre ${mi.availMem / 1048576} MB · ${t}° · ${b?.getIntExtra("level", 0) ?: 0}%"
}

class Act(val t: String, val d: String, val on: String, val off: String? = null)

fun anim(v: String) = "settings put global window_animation_scale $v; settings put global transition_animation_scale $v; settings put global animator_duration_scale $v"
fun res(p: Int) = "s=@(wm size | head -1 | grep -o '[0-9]*x[0-9]*'); w=@{s%x*}; h=@{s#*x}; wm size @((w*$p/100))x@((h*$p/100))"
fun rb(m: String) = "for p in @(pm list packages -3 | cut -d: -f2 | grep -vE 'shizuku|otimizador|%G|inputmethod|keyboard|launcher'); do cmd appops set @p RUN_IN_BACKGROUND $m; done; echo feito"
fun hz(v: String) = "settings put system min_refresh_rate $v; settings put system peak_refresh_rate $v"
const val HZOFF = "settings delete system min_refresh_rate; settings delete system peak_refresh_rate"
fun set(k: String, a: String, b: String, d: String, t: String) = Act(t, d, "settings put $k $a", "settings put $k $b")

val jogo = listOf(
    Act("Otimizar e jogar", "Fecha apps, limpa o cache do jogo, ativa o modo desempenho e abre o jogo.",
        "am kill-all; rm -rf /sdcard/Android/data/%G/cache/*; cmd game mode performance %G; cmd power set-fixed-performance-mode-enabled true; am set-standby-bucket %G active; ${anim("0.5")}; monkey -p %G -c android.intent.category.LAUNCHER 1",
        "cmd game mode standard %G; cmd power set-fixed-performance-mode-enabled false; ${anim("1")}"),
    Act("Game Mode: desempenho", "Pede ao Android 12+ o máximo de desempenho para o jogo.", "cmd game mode performance %G", "cmd game mode standard %G"),
    Act("Desempenho fixo", "Mantém CPU e GPU em desempenho onde o aparelho aceita. Esquenta mais.", "cmd power set-fixed-performance-mode-enabled true", "cmd power set-fixed-performance-mode-enabled false"),
    Act("Compilar só o jogo", "Otimiza o código do jogo para abrir mais rápido e travar menos. Leva alguns minutos.", "cmd package compile -m speed -f %G"),
    Act("Prioridade do jogo", "O sistema trata o jogo como app importante.", "am set-standby-bucket %G active"),
    Act("Fechar apps ao fundo", "Libera RAM na hora. Os apps podem reabrir sozinhos.", "am kill-all"),
    Act("Bloquear fundo dos outros apps", "Impede que reabram sozinhos durante a partida. Reversível.", rb("ignore"), rb("allow")),
    Act("Limpar cache do jogo", "Apaga só a pasta de cache. Não mexe nos arquivos do jogo nem na conta.", "rm -rf /sdcard/Android/data/%G/cache/*; echo ok"),
    Act("Não perturbe", "Bloqueia avisos durante a partida.", "cmd notification set_dnd on", "cmd notification set_dnd off"),
    Act("Bluetooth e NFC off", "Economiza bateria e processamento.", "svc bluetooth disable; svc nfc disable", "svc bluetooth enable; svc nfc enable"),
    Act("Pausar Play Store", "Evita downloads e atualizações durante a partida.", "cmd appops set com.android.vending RUN_IN_BACKGROUND ignore", "cmd appops set com.android.vending RUN_IN_BACKGROUND allow"))

val touch = listOf(
    set("system pointer_speed", "3", "0", "Aumenta a velocidade do toque no sistema.", "Ponteiro +3"),
    set("system pointer_speed", "6", "0", "Mais rápido ainda. Pode ficar sensível demais.", "Ponteiro +6"),
    set("secure long_press_timeout", "250", "400", "Reconhece o toque segurado mais cedo.", "Toque longo rápido (250 ms)"),
    set("system pointer_location", "1", "0", "Barra com as coordenadas X e Y do dedo. Serve para achar falhas na tela.", "Mostrar X e Y do toque"),
    set("system show_touches", "1", "0", "Bolinha onde você toca. Mostra atraso e falhas.", "Mostrar toques"))

val tela = listOf(
    Act("Hz máximo (120)", "Trava a tela em 120 Hz se ela aguentar. Mais fluidez e mais bateria.", hz("120.0"), HZOFF),
    Act("Hz 90", "Equilíbrio entre fluidez e temperatura.", hz("90.0"), HZOFF),
    Act("Vulkan", "Troca o renderizador gráfico. Reabra o jogo. Pode não funcionar em todo aparelho.", "setprop debug.hwui.renderer skiavk", "setprop debug.hwui.renderer ''"),
    Act("Animações desligadas", "Menos efeitos, sistema mais leve.", anim("0"), anim("1")),
    set("global disable_window_blurs", "1", "0", "Desliga o desfoque de janelas (Android 12+).", "Sem blur"),
    Act("Resolução 80%", "Menos pixels, mais FPS. Se a tela ficar estranha, toque em Desfazer.", res(80), "wm size reset"),
    Act("Resolução 70%", "Ganho maior em celular fraco, com imagem mais suave.", res(70), "wm size reset"))

val emu = listOf(
    Act("MODO EMULADOR", "Junta os ajustes mais fortes: resolução 70%, sem animações e blur, desempenho fixo e apps ao fundo bloqueados. Não muda o hardware, mas reduz travadas.",
        res(70) + "; " + anim("0") + "; settings put global disable_window_blurs 1; cmd power set-fixed-performance-mode-enabled true; am kill-all; " + rb("ignore"),
        "wm size reset; " + anim("1") + "; settings put global disable_window_blurs 0; cmd power set-fixed-performance-mode-enabled false; " + rb("allow")))

val dnsAll = listOf(
    Triple("Google", "dns.google", "8.8.8.8"), Triple("Cloudflare", "one.one.one.one", "1.1.1.1"),
    Triple("Quad9", "dns.quad9.net", "9.9.9.9"), Triple("AdGuard", "dns.adguard-dns.com", "94.140.14.14"),
    Triple("CleanBrowsing", "security-filter-dns.cleanbrowsing.org", "185.228.168.9"), Triple("Mullvad", "dns.mullvad.net", "194.242.2.2"),
    Triple("Control D", "p2.freedns.controld.com", "76.76.2.0"), Triple("DNS.SB", "dot.sb", "185.222.222.222"))

val rede = listOf(
    Act("Testar todos os DNS", "Mede a resposta de cada um. Menor tempo é melhor.", dnsAll.joinToString("; ") { "echo ${it.first}; ping -c 2 -W 1 ${it.third} | tail -1" }),
    Act("Ping do jogo", "Mostra a média e a perda de pacote.", "ping -c 5 -W 1 8.8.8.8 | tail -2"),
    Act("DNS automático", "Volta ao DNS do seu provedor.", AUTO)) +
    dnsAll.map { (n, h, _) -> Act("DNS $n", h, "settings put global private_dns_mode hostname; settings put global private_dns_specifier $h; echo ok", AUTO) }

val painel = listOf(
    Act("Status completo", "RAM, armazenamento, bateria e apps que mais usam CPU.", "grep -E 'MemTotal|MemAvailable' /proc/meminfo; df -h /data | tail -1; dumpsys battery | grep -E 'level|temperature'; top -b -n 1 -m 8 | tail -9"),
    Act("Saúde da bateria", "Temperatura, tensão e carga.", "dumpsys battery"),
    Act("Sinal do Wi-Fi", "Força e velocidade da conexão.", "dumpsys wifi | grep -m2 -E 'mWifiInfo|rssi'"),
    Act("Desfazer tudo", "Volta animações, ponteiro, Hz, resolução e restrições ao padrão.",
        "wm size reset; ${anim("1")}; settings put system pointer_speed 0; settings put secure long_press_timeout 400; $HZOFF; settings put global disable_window_blurs 0; cmd power set-fixed-performance-mode-enabled false; cmd game mode standard %G; cmd notification set_dnd off; " + rb("allow")))

val abas = listOf("Jogo", "Touch", "Tela", "Emulador", "Rede", "Painel", "Mais", "Apps", "Teste", "Sensi", "IA", "Ajuda")
val todas = listOf(jogo, touch, tela, emu, rede, painel)

class MainActivity : ComponentActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContent { MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFFF2A2A), onPrimary = Color.White)) { Surface { App() } } }
    }
}

@Composable fun Chip(t: String, sel: Boolean, on: () -> Unit) = if (sel) Button(on) { Text(t) } else OutlinedButton(on) { Text(t) }

@Composable fun Sensi(ctx: Context) {
    val pr = ctx.getSharedPreferences("sensi", 0)
    var f by remember { mutableStateOf(3) }
    var sz by remember { mutableStateOf("6.5") }
    var prof by remember { mutableStateOf("Ranqueada") }
    var out by remember { mutableStateOf("") }
    Text("Sensi inicial sugerida", fontWeight = FontWeight.Bold)
    Text("Valores de partida pelo seu estilo e tela. Ajuste de 5 em 5 no treino.", fontSize = 12.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(2, 3, 4).forEach { Chip("$it dedos", f == it) { f = it } } }
    OutlinedTextField(sz, { sz = it }, label = { Text("Tela em polegadas") })
    Button({
        val s = sz.replace(',', '.').toFloatOrNull() ?: 6.5f
        val x = ((when (f) { 2 -> 85f; 3 -> 78f; else -> 72f }) + (s - 6.5f) * 3f).coerceIn(30f, 100f)
        val y = (x - 8f).coerceAtLeast(20f)
        out = "X ${x.toInt()} · Y ${y.toInt()}\nMira vermelha ${(x * 0.8f).toInt()} · 2x ${(x * 0.7f).toInt()} · 4x ${(x * 0.6f).toInt()} · AWM ${(x * 0.45f).toInt()}"
    }) { Text("Calcular") }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Ranqueada", "Casual", "Treino").forEach { Chip(it, prof == it) { prof = it } } }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton({ pr.edit().putString(prof, out).apply() }) { Text("Salvar perfil") }
        OutlinedButton({ out = pr.getString(prof, "Perfil vazio") ?: "" }) { Text("Abrir perfil") }
    }
    Text(out)
}

@Composable fun IA() {
    var q by remember { mutableStateOf("") }
    var a by remember { mutableStateOf("Pergunte sobre lag, esquentando, ping, touch, sensi ou bateria.") }
    fun ans(s: String): String { val t = s.lowercase(); return when {
        "esquent" in t || "quente" in t -> "Calor derruba o FPS. Use Resolução 80%, Hz 90 em vez de 120, tire a capa e não jogue carregando."
        "lag" in t || "trav" in t || "fps" in t -> "Nesta ordem: 1) Modo Emulador 2) Limpar cache do jogo 3) Compilar só o jogo 4) Resolução menor."
        "ping" in t || "net" in t || "rede" in t || "dns" in t -> "Aba Rede: Testar todos os DNS e use o de menor tempo. Prefira Wi-Fi 5 GHz perto do roteador."
        "touch" in t || "toque" in t -> "Aba Touch: Ponteiro +3 e Toque longo 250. Ligue Mostrar X e Y para ver se algum ponto da tela falha."
        "sensi" in t || "mira" in t -> "Aba Sensi: calcule por dedos e tela, salve num perfil e ajuste de 5 em 5 no treino. Mira boa vem de FPS estável e toque sem falhas."
        "bateria" in t -> "Aba Painel: Saúde da bateria. Evite jogar carregando, porque esquenta e derruba o desempenho."
        else -> "Não entendi. Tente: lag, esquentando, ping, touch, sensi ou bateria." } }
    Text("IA NovaX (offline)", fontWeight = FontWeight.Bold)
    OutlinedTextField(q, { q = it }, label = { Text("Sua pergunta") })
    Button({ a = ans(q) }) { Text("Perguntar") }
    Text(a)
}

@Composable fun App() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(0) }
    var log by remember { mutableStateOf("Toque em uma função.") }
    var busy by remember { mutableStateOf(false) }
    var g by remember { mutableStateOf(FF) }
    var ok by remember { mutableStateOf(granted()) }
    var head by remember { mutableStateOf(info(ctx)) }
    fun go(t: String, c: String) {
        if (!granted()) { log = "Shizuku desligado ou sem permissão."; return }
        scope.launch { busy = true; log = "$t..."; val r = withContext(Dispatchers.IO) { sh(c, g) }; log = "$t\n\n$r"; head = info(ctx); busy = false }
    }
    Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Banner()
        Text(head, fontSize = 11.sp)
        Text(if (ok) "Shizuku: permissão concedida ✔" else "Shizuku sem permissão", fontSize = 12.sp)
        if (!ok) Button({ if (running() && !granted()) Shizuku.requestPermission(1); ok = granted() }) { Text("Conceder / atualizar permissão") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Chip("Free Fire", g == FF) { g = FF }; Chip("Free Fire MAX", g == MAX) { g = MAX } }
        ScrollableTabRow(tab, edgePadding = 0.dp) { abas.forEachIndexed { i, n -> Tab(tab == i, { tab = i }, text = { Text(n) }) } }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (tab) { 2 -> { Resol(busy) { t, c -> go(t, c) }; Lista(todas[2], busy) { t, c -> go(t, c) } }; 4 -> Lista(redeNova, busy) { t, c -> go(t, c) }; 6 -> { Auto(ctx); Flutuante(ctx); Lista(mais, busy) { t, c -> go(t, c) } }; 7 -> Apps(g); 8 -> Teste(ctx); 9 -> { SensiXY(ctx, busy) { t, c -> go(t, c) }; Sensi(ctx) }; 10 -> IA(); 11 -> Ajuda(ctx); else -> Lista(todas[tab], busy) { t, c -> go(t, c) } }
        }
        HorizontalDivider()
        Box(Modifier.heightIn(max = 130.dp).verticalScroll(rememberScrollState())) { Text(log, fontSize = 11.sp) }
    }
}
