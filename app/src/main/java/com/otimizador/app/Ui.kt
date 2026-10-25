package com.otimizador.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import rikka.shizuku.Shizuku

val Rd = Color(0xFFFF2A2A)

val avancado = listOf(
    Act("Wi-Fi baixa latência", "Pede ao Wi-Fi para priorizar atraso baixo (Android 12 ou mais, se o aparelho aceitar). Ajuda no ping. Gasta um pouco mais de bateria.", "cmd wifi force-low-latency-mode enabled", "cmd wifi force-low-latency-mode disabled"),
    Act("Wi-Fi alto desempenho", "Mantém o Wi-Fi no modo de maior desempenho com a tela ligada.", "cmd wifi force-hi-perf-mode enabled", "cmd wifi force-hi-perf-mode disabled"),
    Act("Jogo fora da economia de bateria", "Tira o jogo da lista de economia (Doze), para o sistema não limitá-lo.", "cmd deviceidle whitelist +%G", "cmd deviceidle whitelist -%G"),
    Act("Bateria adaptável off", "Desliga o aprendizado que limita apps pouco usados. Os apps ficam menos restritos e a bateria dura menos.", "settings put global adaptive_battery_management_enabled 0", "settings put global adaptive_battery_management_enabled 1"),
    Act("Standby de apps off", "Desliga o modo de espera automático dos apps. Mais forte, gasta mais bateria.", "settings put global app_standby_enabled 0", "settings put global app_standby_enabled 1"),
    Act("Sem overlays de hardware", "Desenha tudo pela GPU. Pode corrigir falhas de imagem e gastar mais GPU. Alguns aparelhos ignoram.", "service call SurfaceFlinger 1008 i32 1", "service call SurfaceFlinger 1008 i32 0"),
    Act("Otimizar em segundo plano agora", "Roda agora a otimização que o Android faria de madrugada. Demora alguns minutos.", "cmd package bg-dexopt-job"),
    Act("Zerar estatísticas de bateria", "Recomeça a contagem de uso, para você medir de novo.", "dumpsys batterystats --reset; echo ok"),
    Act("Limpar logs do sistema", "Apaga os registros internos acumulados.", "logcat -b all -c; echo ok"),
    Act("Frequência da CPU agora", "Lê a velocidade de cada núcleo. Alguns aparelhos bloqueiam a leitura.", "for i in 0 1 2 3 4 5 6 7; do echo cpu@i @(cat /sys/devices/system/cpu/cpu@i/cpufreq/scaling_cur_freq 2>&1); done"),
    Act("Governador da CPU", "Mostra o modo de energia da CPU. Só leitura: sem root não dá para trocar.", "cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor 2>&1; cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors 2>&1"),
    Act("Informações da GPU", "Mostra o modelo e a versão gráfica.", "dumpsys SurfaceFlinger | grep -m3 -E 'GLES|Vulkan'"))

val ferr = listOf(
    Act("Informações do aparelho", "Modelo, chip, kernel, memória e GPU.", "getprop ro.product.model; getprop ro.board.platform; getprop ro.hardware; uname -r; grep -m1 MemTotal /proc/meminfo; dumpsys SurfaceFlinger | grep -m1 GLES"),
    Act("Captura de tela", "Salva um print na pasta NovaX do armazenamento.", "mkdir -p /sdcard/NovaX; screencap -p /sdcard/NovaX/print_@(date +%s).png; echo salvo em /sdcard/NovaX"),
    Act("Gravar tela (30 s)", "Grava 30 segundos de vídeo na pasta NovaX. O app fica ocupado até terminar.", "mkdir -p /sdcard/NovaX; screenrecord --time-limit 30 /sdcard/NovaX/video_@(date +%s).mp4; echo salvo em /sdcard/NovaX"),
    Act("DPI 90%", "Diminui o tamanho dos elementos da tela, e cabe mais coisa. Desfazer volta ao normal.", "d=@(wm density | grep -o '[0-9]*@' | tail -1); wm density @((d*90/100))", "wm density reset"),
    Act("Fonte menor", "Reduz o tamanho da letra do sistema.", "settings put system font_scale 0.9", "settings put system font_scale 1.0"),
    Act("Brilho máximo", "Brilho manual no máximo.", "settings put system screen_brightness_mode 0; settings put system screen_brightness 255", "settings put system screen_brightness_mode 1"),
    Act("Reiniciar a interface", "Reinicia a tela inicial e a barra do sistema. A tela pisca por alguns segundos.", "am crash com.android.systemui"),
    Act("Reiniciar o celular", "Reinicia na hora. Salve tudo antes. Depois, ligue o Shizuku de novo.", "svc power reboot"))

val cats = listOf(
    Triple("🎮", "Jogo", "Modo jogo e desempenho"), Triple("👆", "Touch", "Resposta da tela"),
    Triple("🖥", "Tela", "Resolução e Hz"), Triple("🚀", "Emulador", "Leveza máxima"),
    Triple("🌐", "Rede", "DNS e ping"), Triple("📊", "Painel", "Status e desfazer"),
    Triple("⚙", "Avançado", "Ajustes profundos"), Triple("🧰", "Ferramentas", "Print, DPI, reiniciar"),
    Triple("📱", "Apps", "Parar e bloquear"), Triple("🧹", "Bloatware", "Desativar apps"),
    Triple("🧪", "Teste", "Toque, FPS, temp"), Triple("🎯", "Sensi", "X, Y e perfis"),
    Triple("🫧", "Bolinha", "Painel flutuante"), Triple("🤖", "IA", "Dicas offline"),
    Triple("❓", "Ajuda", "Como usar"))

class NovaActivity : ComponentActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContent { MaterialTheme(colorScheme = darkColorScheme(primary = Rd, onPrimary = Color.White, surface = Color(0xFF0B0B10), background = Color(0xFF0B0B10))) { NovaApp() } }
    }
}

@Composable fun BigBtn(t: String, on: () -> Unit) {
    val tr = rememberInfiniteTransition(label = "s")
    val s by tr.animateFloat(1f, 1.03f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "s")
    Box(Modifier.fillMaxWidth().scale(s).clip(RoundedCornerShape(18.dp))
        .background(Brush.horizontalGradient(listOf(Rd, Color(0xFFB0001A)))).clickable { on() }.padding(18.dp), contentAlignment = Alignment.Center) {
        Text(t, fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.White)
    }
}

@Composable fun CatCard(e: String, n: String, sub: String, mod: Modifier, on: () -> Unit) {
    Column(mod.clip(RoundedCornerShape(18.dp)).background(Color(0xFF16161F)).border(1.dp, Color(0xFF3A1218), RoundedCornerShape(18.dp)).clickable { on() }.padding(14.dp)) {
        Text(e, fontSize = 28.sp)
        Text(n, fontWeight = FontWeight.Bold)
        Text(sub, fontSize = 11.sp, color = Color(0xFF9A9AA8))
    }
}

@Composable fun Bloat(busy: Boolean, go: (String, String) -> Unit) {
    var p by remember { mutableStateOf("") }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF16161D)), border = BorderStroke(1.dp, Color(0xFF4A1414))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Bloatware (reversível)", fontWeight = FontWeight.Bold, color = Color(0xFFFF5A5A))
            Text("Desativa um app só para o seu usuário, sem root. Restaurar traz de volta. Não desative telefone, interface, teclado nem serviços do Google que você usa.", fontSize = 12.sp)
            OutlinedTextField(p, { p = it.trim() }, Modifier.fillMaxWidth(), label = { Text("Nome do pacote") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({ go("Desativar $p", "pm uninstall -k --user 0 $p") }, enabled = !busy && p.isNotEmpty()) { Text("Desativar") }
                OutlinedButton({ go("Restaurar $p", "cmd package install-existing $p") }, enabled = !busy && p.isNotEmpty()) { Text("Restaurar") }
            }
            OutlinedButton({ go("Pacotes instalados", "pm list packages --user 0 | cut -d: -f2 | sort") }, enabled = !busy) { Text("Listar todos os pacotes") }
            Text("Atalhos: apps do Facebook instalados de fábrica costumam ser dispensáveis. Se algo ficar estranho, use Restaurar.", fontSize = 11.sp, color = Color(0xFF9A9AA8))
            listOf("com.facebook.appmanager", "com.facebook.services", "com.facebook.system").forEach { OutlinedButton({ p = it }) { Text(it, fontSize = 11.sp) } }
        }
    }
}

@Composable fun Conteudo(s: String, ctx: Context, g: String, busy: Boolean, go: (String, String) -> Unit) {
    when (s) {
        "Jogo" -> { Auto(ctx); Lista(jogo, busy, go) }
        "Touch" -> Lista(touch, busy, go)
        "Tela" -> { Resol(busy, go); Lista(tela, busy, go) }
        "Emulador" -> Lista(emu, busy, go)
        "Rede" -> Lista(redeNova, busy, go)
        "Painel" -> { Lista(painel, busy, go); Lista(mais, busy, go) }
        "Avançado" -> Lista(avancado, busy, go)
        "Ferramentas" -> Lista(ferr, busy, go)
        "Apps" -> Apps(g)
        "Bloatware" -> Bloat(busy, go)
        "Teste" -> Teste(ctx)
        "Sensi" -> { SensiXY(ctx, busy, go); Sensi(ctx) }
        "Bolinha" -> Flutuante(ctx)
        "IA" -> IA()
        "Ajuda" -> Ajuda(ctx)
    }
}

@Composable fun NovaApp() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val pr = ctx.getSharedPreferences("nx", 0)
    var scr by remember { mutableStateOf("Início") }
    var log by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var g by remember { mutableStateOf(pr.getString("g", FF) ?: FF) }
    var ok by remember { mutableStateOf(granted()) }
    fun go(t: String, c: String) {
        if (!granted()) { log = "Shizuku desligado ou sem permissão."; return }
        scope.launch { busy = true; log = "$t..."; val r = withContext(Dispatchers.IO) { sh(c, g) }; log = "$t\n\n$r"; busy = false }
    }
    BackHandler(scr != "Início") { scr = "Início" }
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0B0B10), Color(0xFF1B0A10))))) {
        Column(Modifier.padding(12.dp, 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Banner()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (ok) "● Shizuku conectado" else "● Shizuku sem permissão", color = if (ok) Color(0xFF3DDC84) else Rd, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Chip("FF", g == FF) { g = FF; pr.edit().putString("g", FF).apply() }
                Chip("MAX", g == MAX) { g = MAX; pr.edit().putString("g", MAX).apply() }
            }
            if (!ok) Button({ if (running() && !granted()) Shizuku.requestPermission(1); ok = granted() }, Modifier.fillMaxWidth()) { Text("Conceder / atualizar permissão do Shizuku") }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (scr == "Início") {
                BigBtn("🎮  ATIVAR MODO JOGO") { go("Modo Jogo", jogo[0].on) }
                cats.chunked(2).forEach { r ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        r.forEach { (e, n, s) -> CatCard(e, n, s, Modifier.weight(1f).heightIn(min = 96.dp)) { scr = n } }
                        if (r.size < 2) Spacer(Modifier.weight(1f))
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton({ scr = "Início" }) { Text("← Voltar") }
                    Text(scr, fontWeight = FontWeight.Black, fontSize = 20.sp)
                }
                Conteudo(scr, ctx, g, busy) { t, c -> go(t, c) }
            }
            Spacer(Modifier.height(8.dp))
        }
        if (log.isNotEmpty()) Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF16161F))) {
            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).heightIn(max = 110.dp).verticalScroll(rememberScrollState())) { Text(log, fontSize = 11.sp) }
                TextButton({ log = "" }) { Text("✕") }
            }
        }
        NavigationBar(containerColor = Color(0xFF101016)) {
            listOf("🏠" to "Início", "🎮" to "Jogo", "⚙" to "Avançado", "🧰" to "Ferramentas").forEach { (e, n) ->
                NavigationBarItem(selected = scr == n, onClick = { scr = n }, icon = { Text(e, fontSize = 20.sp) }, label = { Text(n, fontSize = 10.sp) })
            }
        }
    }
}
