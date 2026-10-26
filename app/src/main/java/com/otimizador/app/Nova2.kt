package com.otimizador.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import kotlin.math.sqrt
import rikka.shizuku.Shizuku

val n2Raw = """
Sistema§Renderizador OpenGL§Alternativa ao Vulkan. Teste qual dá mais FPS. Reabra o jogo.§setprop debug.hwui.renderer skiagl§setprop debug.hwui.renderer ''
Sistema§Sem sons de toque§Tira o som ao tocar na tela.§settings put system sound_effects_enabled 0§settings put system sound_effects_enabled 1
Sistema§Sem vibração ao tocar§Desliga a vibração do toque. Poupa bateria.§settings put system haptic_feedback_enabled 0§settings put system haptic_feedback_enabled 1
Sistema§Sem sons de bloqueio§Tira o som de travar e destravar a tela.§settings put system lockscreen_sounds_enabled 0§settings put system lockscreen_sounds_enabled 1
Sistema§Sem pop-ups de notificação§Impede avisos que cobrem a tela durante a partida.§settings put global heads_up_notifications_enabled 0§settings put global heads_up_notifications_enabled 1
Sistema§Travar tela deitada§Trava a rotação em paisagem para o jogo.§settings put system accelerometer_rotation 0; settings put system user_rotation 1§settings put system accelerometer_rotation 1
Sistema§Tela ligada carregando§Mantém a tela ligada enquanto o celular carrega.§settings put global stay_on_while_plugged_in 3§settings put global stay_on_while_plugged_in 0
Sistema§Tela desliga em 30 min§Evita a tela apagar numa partida longa.§settings put system screen_off_timeout 1800000§settings put system screen_off_timeout 60000
Sistema§Modo escuro§Em tela OLED o escuro gasta menos bateria.§cmd uimode night yes§cmd uimode night no
Sistema§Brilho automático desligado§O brilho não muda sozinho durante o jogo.§settings put system screen_brightness_mode 0§settings put system screen_brightness_mode 1
Sistema§Sem hardware overlays§Desenha tudo pela GPU. Pode corrigir falhas de imagem. Alguns aparelhos ignoram.§service call SurfaceFlinger 1008 i32 1§service call SurfaceFlinger 1008 i32 0
Rede§Wi-Fi sem busca em segundo plano§Para de procurar redes sozinho. Menos oscilação.§settings put global wifi_scan_always_enabled 0§settings put global wifi_scan_always_enabled 1
Rede§Bluetooth sem busca em segundo plano§Para de procurar aparelhos Bluetooth sozinho.§settings put global ble_scan_always_enabled 0§settings put global ble_scan_always_enabled 1
Rede§Sem aviso de Wi-Fi aberto§Tira as notificações de redes disponíveis.§settings put global wifi_networks_available_notification_on 0§settings put global wifi_networks_available_notification_on 1
Rede§Jogar só no Wi-Fi§Desliga os dados móveis para o celular não trocar de rede na partida.§svc data disable§svc data enable
Rede§Portal cativo desligado§Para de testar a conexão a cada rede. Em Wi-Fi de hotel pode não abrir o login.§settings put global captive_portal_mode 0§settings put global captive_portal_mode 1
Rede§TCP com janela maior§Recebe mais dados de uma vez. O efeito varia por aparelho e operadora.§settings put global tcp_default_init_rwnd 60§settings delete global tcp_default_init_rwnd
Rede§Reiniciar o Wi-Fi§Desliga e liga o Wi-Fi. Ajuda com queda de sinal e ping alto.§svc wifi disable; sleep 2; svc wifi enable
Rede§Reiniciar a conexão toda§Liga o modo avião por 3 segundos e reconecta Wi-Fi e dados.§cmd connectivity airplane-mode enable; sleep 3; cmd connectivity airplane-mode disable
Rede§Wi-Fi com baixa latência§Prioriza atraso baixo (Android 12 ou mais, se o aparelho aceitar).§cmd wifi force-low-latency-mode enabled§cmd wifi force-low-latency-mode disabled
Rede§Economia de dados desligada§Libera os dados em segundo plano para o jogo.§cmd netpolicy set restrict-background false§cmd netpolicy set restrict-background true
Rede§Ping com 10 pacotes§Mede a média e a oscilação do ping.§ping -c 10 -i 0.3 -W 1 8.8.8.8 | tail -2
Energia§Economia de bateria desligada§Impede o modo de economia de limitar o desempenho.§settings put global low_power 0§settings put global low_power 1
Energia§Doze desligado§Tira o modo de espera profundo. Gasta mais bateria. Volta ao normal ao reiniciar.§dumpsys deviceidle disable all§dumpsys deviceidle enable all
Energia§Bateria adaptável desligada§O sistema para de limitar apps pouco usados.§settings put global adaptive_battery_management_enabled 0§settings put global adaptive_battery_management_enabled 1
Energia§Standby de apps desligado§Mais forte que o anterior. Gasta mais bateria.§settings put global app_standby_enabled 0§settings put global app_standby_enabled 1
Energia§Relógio na tela apagada desligado§Desliga o AOD. Poupa bateria.§settings put secure doze_always_on 0§settings put secure doze_always_on 1
Memória§Pedir memória aos apps§Pede a todos os apps que liberem memória sem uso. Mais gentil que fechar.§for p in @(pm list packages -3 | cut -d: -f2 | grep -vE 'shizuku|otimizador|%G'); do am send-trim-memory @p RUNNING_CRITICAL; done; echo feito
Memória§Ver RAM por app§Lista quem mais usa memória agora.§dumpsys meminfo | head -25
Memória§Processos mais pesados§Os 15 processos que mais ocupam RAM.§ps -A -o RSS,NAME | sort -nr | head -15
Memória§Manutenção do sistema agora§Roda a manutenção que o Android faria de madrugada.§am idle-maintenance
Memória§Otimizar armazenamento (TRIM)§Reorganiza o armazenamento. Pode ser ignorado em alguns aparelhos.§sm idle-maint run
Memória§Compilar o jogo (profile)§Compilação equilibrada do jogo, boa para o dia a dia.§cmd package compile -m speed-profile -f %G§cmd package compile --reset %G
Memória§Compilar o jogo (tudo)§Compilação máxima. Demora mais e usa mais espaço.§cmd package compile -m everything -f %G§cmd package compile --reset %G
Memória§Fechar o jogo§Força a parada do jogo selecionado.§am force-stop %G
Jogo§Game Mode: bateria§Economiza bateria no jogo, com desempenho menor.§cmd game mode battery %G§cmd game mode standard %G
Jogo§Jogo fora do Doze§Tira o jogo da economia do Doze.§cmd deviceidle whitelist +%G§cmd deviceidle whitelist -%G
Toque§Multi-toque mais rápido§Reconhece toques múltiplos mais cedo. Ajuda com 3 ou 4 dedos.§settings put secure multi_press_timeout 250§settings put secure multi_press_timeout 300
Toque§Toque longo 200 ms§Reconhece o toque segurado ainda mais cedo.§settings put secure long_press_timeout 200§settings put secure long_press_timeout 400
Toque§Ponteiro +2§Aumento leve na velocidade do toque. Bom para começar.§settings put system pointer_speed 2§settings put system pointer_speed 0
Toque§Esconder toques e X e Y§Desliga as marcas de toque e as coordenadas na tela.§settings put system show_touches 0; settings put system pointer_location 0
Diagnóstico§Temperatura dos sensores§Mostra a temperatura de CPU, bateria e do aparelho.§dumpsys thermalservice | head -30
Diagnóstico§Hz atual da tela§Mostra o modo de tela em uso.§dumpsys display | grep -m4 -iE 'activeMode|renderFrameRate|mRefreshRate'
Diagnóstico§Hz que a tela aceita§Lista as taxas de atualização suportadas.§dumpsys display | grep -o 'fps=[0-9.]*' | sort -u
Diagnóstico§Resolução e densidade§Mostra o tamanho e o DPI atuais.§wm size; wm density
Diagnóstico§Memória do aparelho§RAM total, livre e swap.§grep -E 'MemTotal|MemFree|MemAvailable|SwapTotal' /proc/meminfo
Diagnóstico§Quedas de quadro do jogo§Mostra quadros e quadros lentos. Funciona melhor em apps que usam a tela padrão.§dumpsys gfxinfo %G | grep -E 'Total frames|Janky'
Diagnóstico§Saúde da bateria§Nível, temperatura, tensão e saúde.§dumpsys battery | grep -E 'level|temperature|voltage|health'
Limpeza§Arquivos grandes§Lista até 10 arquivos acima de 100 MB, para você decidir o que apagar.§find /sdcard -type f -size +100M 2>/dev/null | head -10
Limpeza§Limpar cache de todos os apps§Apaga os arquivos temporários de todos os apps.§rm -rf /sdcard/Android/data/*/cache/*; pm trim-caches 999G; echo ok
"""

val n2Cat: Map<String, List<Act>> = n2Raw.lines().filter { it.contains("§") }.map { it.split("§") }
    .groupBy({ it[0] }, { Act(it[1], it[2], it[3], it.getOrNull(4)?.ifBlank { null }) })
fun n2(c: String): List<Act> = n2Cat[c] ?: listOf()
val n2All: Map<String, List<Act>> = linkedMapOf(
    "Jogo" to (jogo + n2("Jogo")), "Touch" to (touch + n2("Toque")), "Tela" to tela, "Emulador" to emu,
    "Rede" to (redeNova + n2("Rede")), "Sistema" to n2("Sistema"), "Energia" to n2("Energia"),
    "Memória" to n2("Memória"), "Painel" to (painel + mais + n2("Diagnóstico")), "Limpeza" to n2("Limpeza"))
val n2Themes = listOf(0xFFFF2A2A, 0xFF2A7BFF, 0xFF1ED760, 0xFFA855F7, 0xFFFF8A00).map { Color(it) }

class Nova2Activity : ComponentActivity() {
    override fun onCreate(b: Bundle?) { super.onCreate(b); setContent { N2Root() } }
}

@Composable fun N2Root() {
    val pr = LocalContext.current.getSharedPreferences("nx", 0)
    var ai by remember { mutableStateOf(pr.getInt("ac", 0)) }
    val ac = n2Themes[ai]
    MaterialTheme(colorScheme = darkColorScheme(primary = ac, onPrimary = Color.White, surface = Color(0xFF0B0B10), background = Color(0xFF0B0B10))) {
        N2App(ac, ai) { ai = it; pr.edit().putInt("ac", it).apply() }
    }
}

@Composable fun N2Big(t: String, ac: Color, on: () -> Unit) {
    val tr = rememberInfiniteTransition(label = "n2")
    val s by tr.animateFloat(1f, 1.035f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "s")
    Box(Modifier.fillMaxWidth().scale(s).clip(RoundedCornerShape(20.dp))
        .background(Brush.horizontalGradient(listOf(ac, ac.copy(alpha = 0.55f)))).clickable { on() }.padding(20.dp), contentAlignment = Alignment.Center) {
        Text(t, fontWeight = FontWeight.Black, fontSize = 19.sp, color = Color.White)
    }
}

@Composable fun N2Card(a: Act, ac: Color, fav: Boolean, onFav: () -> Unit, busy: Boolean, go: (String, String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val more = exp[a.t]
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
        .background(Brush.linearGradient(listOf(Color(0xFF1A1A24), Color(0xFF12121A))))
        .border(1.dp, ac.copy(alpha = 0.35f), RoundedCornerShape(18.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(a.t, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text(if (fav) "★" else "☆", color = ac, fontSize = 24.sp, modifier = Modifier.clickable { onFav() })
        }
        Text(a.d, fontSize = 12.sp, color = Color(0xFFB0B0BC))
        if (more != null) {
            Text(if (open) "▲ Fechar explicação" else "▼ Como funciona", fontSize = 11.sp, color = ac, modifier = Modifier.clickable { open = !open })
            if (open) Text(more, fontSize = 12.sp, color = Color(0xFFD0D0DA))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({ go(a.t, a.on) }, enabled = !busy) { Text("Aplicar") }
            if (a.off != null) OutlinedButton({ go("Desfazer: ${a.t}", a.off) }, enabled = !busy) { Text("Desfazer") }
        }
    }
}

@Composable fun SensiPro(ctx: Context, ac: Color) {
    val dm = ctx.resources.displayMetrics
    val wi = dm.widthPixels / dm.xdpi
    val hi = dm.heightPixels / dm.ydpi
    val inch = sqrt(wi * wi + hi * hi)
    val pr = ctx.getSharedPreferences("sensi", 0)
    var f by remember { mutableStateOf(3) }
    var st by remember { mutableStateOf(1) }
    var sp by remember { mutableStateOf(1) }
    var adj by remember { mutableStateOf(0) }
    var prof by remember { mutableStateOf("Ranqueada") }
    var msg by remember { mutableStateOf("") }
    var speed by remember { mutableStateOf("Faça deslizes rápidos aqui para medir o seu estilo") }
    val x = ((when (f) { 2 -> 88f; 3 -> 80f; else -> 74f }) + (1 - st) * 6f + (1 - sp) * 6f + (inch - 6.5f) * 3f + adj).coerceIn(20f, 100f)
    val tbl = "X (horizontal) ${x.toInt()} · Y (vertical) ${(x * 0.92f).toInt()}\nMira vermelha ${(x * 0.85f).toInt()} · 2x ${(x * 0.75f).toInt()} · 4x ${(x * 0.62f).toInt()} · 8x ${(x * 0.52f).toInt()} · AWM ${(x * 0.45f).toInt()}"
    Text("🎯 Sensi Pro", fontWeight = FontWeight.Black, fontSize = 20.sp)
    Text("Tela detectada: ${"%.1f".format(inch)} pol · ${dm.widthPixels}×${dm.heightPixels} · ${dm.densityDpi} dpi", fontSize = 12.sp, color = Color(0xFF9A9AA8))
    Text("Quantos dedos você usa", fontSize = 12.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(2, 3, 4).forEach { Chip("$it dedos", f == it) { f = it } } }
    Text("Seu estilo de jogo", fontSize = 12.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("Rush", "Equilibrado", "Longa").forEachIndexed { i, n -> Chip(n, st == i) { st = i } } }
    Text("Velocidade do seu dedo (ou meça abaixo)", fontSize = 12.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("Lenta", "Média", "Rápida").forEachIndexed { i, n -> Chip(n, sp == i) { sp = i } } }
    Box(Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF14141C)).pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val d = awaitFirstDown()
                val t0 = d.uptimeMillis; val p0 = d.position
                var tl = t0; var pl = p0
                do {
                    val e = awaitPointerEvent()
                    e.changes.firstOrNull()?.let { tl = it.uptimeMillis; pl = it.position }
                } while (e.changes.any { it.pressed })
                val dist = (pl - p0).getDistance()
                val dt = (tl - t0).coerceAtLeast(1L)
                val v = dist / size.width / (dt / 1000f)
                if (dist > 40f) { sp = if (v < 1.6f) 0 else if (v < 3.2f) 1 else 2; speed = "Velocidade ${"%.1f".format(v)} telas/s → ${listOf("Lenta", "Média", "Rápida")[sp]} (estimativa)" }
            }
        }
    }, contentAlignment = Alignment.Center) { Text(speed, fontSize = 12.sp, color = Color(0xFF9A9AA8)) }
    Text(tbl, color = ac, fontWeight = FontWeight.Bold)
    Text("Ajuste fino: $adj. No treino, se a mira passa do alvo, toque em \"Passou\"; se fica curta, em \"Curto\".", fontSize = 11.sp, color = Color(0xFF9A9AA8))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton({ adj -= 4 }) { Text("Passou −4") }
        OutlinedButton({ adj += 4 }) { Text("Curto +4") }
        OutlinedButton({ adj = 0 }) { Text("Zerar") }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("Ranqueada", "Casual", "Treino").forEach { Chip(it, prof == it) { prof = it } } }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Button({ pr.edit().putString(prof, tbl + "\nAjuste $adj").apply(); msg = "Perfil $prof salvo." }) { Text("Salvar perfil") }
        OutlinedButton({ msg = pr.getString(prof, "Perfil vazio") ?: "" }) { Text("Abrir perfil") }
    }
    if (msg.isNotEmpty()) Text(msg, fontSize = 12.sp)
    Text("Esses são valores de partida. Digite no Free Fire em Configurações, Sensibilidade, e ajuste no treino.", fontSize = 11.sp, color = Color(0xFF9A9AA8))
}

@Composable fun N2App(ac: Color, ai: Int, setAi: (Int) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val pr = ctx.getSharedPreferences("nx", 0)
    var tab by remember { mutableStateOf(0) }
    var cat by remember { mutableStateOf("Todas") }
    var sub by remember { mutableStateOf("Apps") }
    var q by remember { mutableStateOf("") }
    var log by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var g by remember { mutableStateOf(pr.getString("g", FF) ?: FF) }
    var ok by remember { mutableStateOf(granted()) }
    var favs by remember { mutableStateOf<Set<String>>(pr.getStringSet("fav", null)?.toSet() ?: emptySet()) }
    fun go(t: String, c: String) {
        if (!granted()) { log = "Shizuku desligado ou sem permissão."; return }
        scope.launch { busy = true; log = "$t..."; val r = withContext(Dispatchers.IO) { sh(c, g) }; log = "$t\n\n$r"; busy = false }
    }
    fun toggle(t: String) { favs = if (t in favs) favs - t else favs + t; pr.edit().putStringSet("fav", favs.toSet()).apply() }
    val all = n2All.values.flatten().distinctBy { it.t }
    val list = when {
        q.isNotBlank() -> all.filter { it.t.contains(q, true) || it.d.contains(q, true) }
        cat == "Favoritos" -> all.filter { it.t in favs }
        cat == "Todas" -> all
        else -> n2All[cat] ?: listOf()
    }
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0B0B10), ac.copy(alpha = 0.2f))))) {
        Column(Modifier.padding(12.dp, 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Banner()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (ok) "● Shizuku conectado" else "● Shizuku sem permissão", color = if (ok) Color(0xFF3DDC84) else Color(0xFFFF5A5A), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Chip("FF", g == FF) { g = FF; pr.edit().putString("g", FF).apply() }
                Chip("MAX", g == MAX) { g = MAX; pr.edit().putString("g", MAX).apply() }
            }
            if (!ok) Button({ if (running() && !granted()) Shizuku.requestPermission(1); ok = granted() }, Modifier.fillMaxWidth()) { Text("Conceder / atualizar permissão do Shizuku") }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (tab) {
                0 -> {
                    N2Big("🎮  ATIVAR MODO JOGO", ac) { go("Modo Jogo", jogo[0].on) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton({ go("Modo Emulador", emu[0].on) }, Modifier.weight(1f), enabled = !busy) { Text("🚀 Emulador") }
                        OutlinedButton({ go("Desfazer tudo", painel[3].on) }, Modifier.weight(1f), enabled = !busy) { Text("♻ Desfazer tudo") }
                    }
                    Text("⭐ Favoritos", fontWeight = FontWeight.Bold)
                    val fl = all.filter { it.t in favs }
                    if (fl.isEmpty()) Text("Toque na estrela ☆ de qualquer função para fixar aqui.", fontSize = 12.sp, color = Color(0xFF9A9AA8))
                    fl.forEach { a -> N2Card(a, ac, true, { toggle(a.t) }, busy) { t, c -> go(t, c) } }
                }
                1 -> {
                    OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), label = { Text("🔎 Buscar função") }, singleLine = true)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (listOf("Todas", "Favoritos") + n2All.keys).forEach { Chip(it, cat == it) { cat = it } }
                    }
                    Text("${list.size} funções", fontSize = 11.sp, color = Color(0xFF9A9AA8))
                    list.forEach { a -> N2Card(a, ac, a.t in favs, { toggle(a.t) }, busy) { t, c -> go(t, c) } }
                }
                2 -> { SensiPro(ctx, ac); SensiXY(ctx
