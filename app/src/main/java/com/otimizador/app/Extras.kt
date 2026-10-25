package com.otimizador.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.*
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*

val autoOn = "am kill-all; cmd game mode performance %G; cmd power set-fixed-performance-mode-enabled true; ${anim("0.5")}; cmd notification set_dnd on"
val autoOff = "cmd game mode standard %G; cmd power set-fixed-performance-mode-enabled false; ${anim("1")}; cmd notification set_dnd off"
fun tmp(c: Context) = (c.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.getIntExtra("temperature", 0) ?: 0) / 10f
fun bt(m: String) = "for p in @(pm list packages -3 | cut -d: -f2 | grep -vE 'shizuku|otimizador|inputmethod|keyboard'); do cmd appops set @p BOOT_COMPLETED $m; done; echo feito"

val mais = listOf(
    Act("Verificar o que está ativo", "Mostra Android, resolução, Hz e Game Mode do seu aparelho.",
        "echo Android @(getprop ro.build.version.release); wm size; wm density; settings get system peak_refresh_rate; settings get system min_refresh_rate; getprop debug.hwui.renderer; cmd game list-modes %G | head -3"),
    Act("Brilho e tela ligada", "Brilho manual e tela ligada por 10 minutos.", "settings put system screen_brightness_mode 0; settings put system screen_off_timeout 600000", "settings put system screen_brightness_mode 1; settings put system screen_off_timeout 60000"),
    Act("Limpeza de lixo", "Limpa cache de todos os apps, miniaturas e temporários.", "pm trim-caches 999G; rm -rf /sdcard/DCIM/.thumbnails /sdcard/Android/data/*/cache/*; echo ok"),
    Act("Compilar todos os apps", "Deixa todos os apps mais rápidos para abrir. Demora vários minutos.", "cmd package compile -m speed-profile -a"),
    Act("Localização off", "Desliga o GPS durante o jogo.", "settings put secure location_mode 0", "settings put secure location_mode 3"),
    Act("Ver apps que iniciam sozinhos", "Lista os apps que ligam junto com o celular.", "for p in @(pm list packages -3 | cut -d: -f2); do dumpsys package @p | grep -q RECEIVE_BOOT_COMPLETED && echo @p; done"),
    Act("Bloquear início automático", "Impede que os apps de terceiros iniciem sozinhos.", bt("ignore"), bt("allow")),
    Act("Apps que mais usam CPU", "Os 10 processos mais pesados agora.", "top -b -n 1 -m 10 | tail -11"))

val exp = mapOf(
    "Otimizar e jogar" to "Faz tudo de uma vez: fecha apps, limpa o cache do jogo, pede desempenho máximo ao Android, reduz animações e abre o jogo. Use antes de cada partida. O botão Desfazer volta o modo normal.",
    "Game Mode: desempenho" to "Recurso do Android 12 ou mais que libera mais CPU e GPU para o jogo. Pode esquentar mais. Em Android 11 ou menos o comando é ignorado.",
    "Desempenho fixo" to "Impede o sistema de baixar a velocidade do processador para economizar bateria. Dá FPS mais estável, mas aquece e gasta mais. Alguns aparelhos ignoram.",
    "Compilar só o jogo" to "Converte o código do jogo para o formato nativo do aparelho. Abre mais rápido e engasga menos. Leva alguns minutos e só precisa repetir depois de atualizar o jogo.",
    "Prioridade do jogo" to "Avisa ao sistema que o jogo é um app importante, para ele não ser limitado em segundo plano.",
    "Fechar apps ao fundo" to "Libera RAM na hora. Ajuda em celular com pouca memória, mas os apps voltam depois.",
    "Bloquear fundo dos outros apps" to "Impede que outros apps rodem sozinhos enquanto você joga. É mais forte que só fechar. Não mexe em teclado nem tela inicial. Desfazer devolve o normal.",
    "Limpar cache do jogo" to "Apaga só arquivos temporários do jogo. Não toca na conta, nos dados nem nos arquivos do jogo. A primeira abertura depois pode demorar um pouco mais.",
    "Não perturbe" to "Bloqueia avisos e ligações durante a partida, para nada tirar sua atenção.",
    "Pausar Play Store" to "Evita downloads e atualizações que gastam rede e processador durante a partida.",
    "Ponteiro +3" to "Aumenta a velocidade com que o sistema responde ao movimento do dedo. A sensi do jogo continua sendo a do Free Fire.",
    "Toque longo rápido (250 ms)" to "O sistema reconhece o toque segurado mais cedo. Deixa ações que dependem de segurar o dedo mais ágeis.",
    "Mostrar X e Y do toque" to "Mostra uma barra no topo com as coordenadas do dedo. Serve para achar partes da tela que falham.",
    "Hz máximo (120)" to "Trava a tela na taxa mais alta que ela suporta. Movimento e mira ficam mais fluidos. Gasta mais bateria.",
    "Vulkan" to "Troca o renderizador gráfico do sistema. Pode dar mais FPS em alguns aparelhos e não ter efeito em outros. Reabra o jogo depois.",
    "Resolução 70%" to "Diminui os pixels da tela. É o ajuste que mais aumenta o FPS em celular fraco, com imagem um pouco mais suave. Desfazer volta ao normal.",
    "MODO EMULADOR" to "Junta os ajustes mais fortes: resolução 70%, animações e blur desligados, desempenho fixo, apps ao fundo fechados e bloqueados. Não muda o hardware, mas reduz travadas. Desfazer reverte tudo.",
    "Testar todos os DNS" to "Mede o tempo até o servidor de cada DNS. Menor é melhor. Mostra a distância, não a velocidade real da internet.",
    "Ping do jogo" to "Mostra o tempo médio de resposta e a perda de pacotes. Perda acima de 2% já causa lag.",
    "DNS automático" to "Volta ao DNS do seu provedor de internet.",
    "Desfazer tudo" to "Devolve animações, ponteiro, Hz, resolução e restrições ao padrão. Use se algo ficar estranho.",
    "Limpeza de lixo" to "Apaga cache e arquivos temporários. Não apaga fotos, mensagens nem dados dos apps.")

val dnsInfo = mapOf(
    "Google" to "Rápido e estável em quase todo lugar. Bom padrão. Não filtra nada.",
    "Cloudflare" to "Costuma ser o de menor tempo de resposta. Foco em privacidade. Ótima primeira escolha para jogar.",
    "Quad9" to "Bloqueia sites perigosos já conhecidos (golpes e vírus). Um pouco mais lento em alguns lugares.",
    "AdGuard" to "Bloqueia anúncios e rastreadores em todo o celular. Pode bloquear algo legítimo. No jogo não atrapalha.",
    "CleanBrowsing" to "Filtro de segurança contra sites de golpe e vírus. Bom para celular usado por mais pessoas.",
    "Mullvad" to "Foco em privacidade e sem registro de acesso. O tempo de resposta varia conforme o seu lugar.",
    "Control D" to "Filtra anúncios e rastreadores. O tempo de resposta varia conforme o seu lugar.",
    "DNS.SB" to "Foco em privacidade. No Brasil o tempo de resposta pode ser maior.")
val redeNova = rede.map { a -> dnsInfo[a.t.removePrefix("DNS ")]?.let { Act(a.t, it + " DNS não baixa o ping dentro da partida, só acelera a conexão inicial.", a.on, a.off) } ?: a }

@Composable fun Card3(a: Act, busy: Boolean, go: (String, String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val more = exp[a.t]
    Card(Modifier.fillMaxWidth().animateContentSize(), colors = CardDefaults.cardColors(containerColor = Color(0xFF16161D)), border = BorderStroke(1.dp, Color(0xFF4A1414))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(a.t, fontWeight = FontWeight.Bold, color = Color(0xFFFF5A5A))
            Text(a.d, fontSize = 12.sp)
            if (more != null) {
                Text(if (open) "▲ Fechar explicação" else "▼ Como funciona", fontSize = 11.sp, color = Color(0xFF9A9AA8), modifier = Modifier.clickable { open = !open })
                if (open) Text(more, fontSize = 12.sp, color = Color(0xFFD0D0DA))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({ go(a.t, a.on) }, enabled = !busy) { Text("Aplicar") }
                if (a.off != null) OutlinedButton({ go("Desfazer: ${a.t}", a.off) }, enabled = !busy) { Text("Desfazer") }
            }
        }
    }
}

@Composable fun Lista(l: List<Act>, busy: Boolean, go: (String, String) -> Unit) = l.forEach { Card3(it, busy, go) }

@Composable fun Banner() {
    val t = rememberInfiniteTransition(label = "b")
    val a by t.animateFloat(0.35f, 1f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "a")
    Box(Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(14.dp))) {
        Image(painterResource(R.drawable.banner), "NovaX", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xEE0B0B0F)))))
        Text("OTIMIZAÇÃO PARA FREE FIRE", Modifier.align(Alignment.BottomStart).padding(10.dp), color = Color(0xFFFF2A2A).copy(alpha = a), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
    val s = StatFs(Environment.getDataDirectory().path)
    val livre = s.availableBytes * 100 / s.totalBytes
    if (livre < 10) Text("⚠ Armazenamento quase cheio: $livre% livre", color = Color(0xFFFFB300), fontSize = 12.sp)
}

@Composable fun Auto(ctx: Context) {
    var on by remember { mutableStateOf(false) }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF16161D)), border = BorderStroke(1.dp, Color(0xFFFF2A2A))) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Modo Jogo automático", fontWeight = FontWeight.Bold, color = Color(0xFFFF5A5A))
                Text("Detecta o Free Fire ou MAX e ativa os ajustes sozinho. Ao sair do jogo, desfaz. Deixa uma notificação fixa.", fontSize = 12.sp)
            }
            Switch(on, { v ->
                on = v
                val i = Intent(ctx, GameService::class.java)
                if (v) { if (Build.VERSION.SDK_INT >= 33) perm.launch("android.permission.POST_NOTIFICATIONS"); ctx.startForegroundService(i) } else ctx.stopService(i)
            })
        }
    }
}

@Composable fun Apps(g: String) {
    val scope = rememberCoroutineScope()
    var l by remember { mutableStateOf(listOf<String>()) }
    var msg by remember { mutableStateOf("") }
    Text("Seus apps. Parar fecha o app agora. Bloq. impede que rode sozinho.", fontSize = 12.sp)
    Button({ scope.launch { l = withContext(Dispatchers.IO) { sh("pm list packages -3 | cut -d: -f2 | sort", g).lines().filter { it.contains('.') } } } }) { Text("Carregar apps instalados") }
    Text(msg, fontSize = 11.sp)
    l.forEach { p ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(p, fontSize = 11.sp, modifier = Modifier.weight(1f))
            TextButton({ scope.launch { withContext(Dispatchers.IO) { sh("am force-stop $p", g) }; msg = "Parado: $p" } }) { Text("Parar") }
            TextButton({ scope.launch { withContext(Dispatchers.IO) { sh("cmd appops set $p RUN_IN_BACKGROUND ignore", g) }; msg = "Fundo bloqueado: $p" } }) { Text("Bloq.") }
        }
    }
}

@Composable fun Teste(ctx: Context) {
    val scope = rememberCoroutineScope()
    val pr = ctx.getSharedPreferences("nx", 0)
    val pts = remember { mutableStateListOf<Offset>() }
    val temps = remember { mutableStateListOf<Float>() }
    var xy by remember { mutableStateOf("Toque no quadro") }
    var n by remember { mutableStateOf(0) }
    var bench by remember { mutableStateOf("Não medido") }
    var wifi by remember { mutableStateOf("...") }
    var sess by remember { mutableStateOf(pr.getLong("ini", 0L)) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            temps.add(tmp(ctx)); if (temps.size > 40) temps.removeAt(0)
            wifi = try {
                val i = (ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).connectionInfo
                "${i.rssi} dBm · ${i.linkSpeed} Mbps · " + if (i.frequency > 4900) "5 GHz (melhor para jogar)" else "2,4 GHz (tente o 5 GHz)"
            } catch (e: Throwable) { "indisponível" }
            now = System.currentTimeMillis(); delay(3000)
        }
    }
    Text("Teste de toque, X e Y e mapa de calor", fontWeight = FontWeight.Bold)
    Text("Arraste os dedos no quadro. Pontos faltando ou linhas falhando indicam tela com problema.", fontSize = 12.sp)
    Canvas(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF101016)).pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val e = awaitPointerEvent()
                e.changes.forEach { c -> if (c.pressed) { pts.add(c.position); if (pts.size > 700) pts.removeAt(0); n++; xy = "X ${c.position.x.toInt()} · Y ${c.position.y.toInt()} · eventos $n" } }
            }
        }
    }) { pts.forEach { drawCircle(Color(0x44FF2A2A), 16f, it) } }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(xy, fontSize = 12.sp, modifier = Modifier.weight(1f))
        OutlinedButton({ pts.clear(); n = 0 }) { Text("Limpar") }
    }
    HorizontalDivider()
    Text("Desempenho antes e depois", fontWeight = FontWeight.Bold)
    Button({
        scope.launch {
            val ms = withContext(Dispatchers.Default) {
                val t0 = System.nanoTime(); val s = BooleanArray(3_000_000)
                for (i in 2 until s.size) if (!s[i]) { var j = i.toLong() * i; while (j < s.size) { s[j.toInt()] = true; j += i } }
                (System.nanoTime() - t0) / 1_000_000
            }
            val ant = pr.getLong("bench", 0L); pr.edit().putLong("bench", ms).apply()
            bench = "Agora: $ms ms · Anterior: " + (if (ant == 0L) "-" else "$ant ms") + " (menor é melhor)"
        }
    }) { Text("Rodar teste de desempenho") }
    Text(bench, fontSize = 12.sp)
    HorizontalDivider()
    Text("Temperatura: ${"%.1f".format(temps.lastOrNull() ?: 0f)}°", fontWeight = FontWeight.Bold)
    Canvas(Modifier.fillMaxWidth().height(70.dp).background(Color(0xFF101016))) {
        if (temps.size > 1) {
            val lo = temps.min() - 1; val hi = temps.max() + 1
            for (i in 1 until temps.size) drawLine(Color(0xFFFF2A2A),
                Offset(size.width * (i - 1) / (temps.size - 1), size.height * (1 - (temps[i - 1] - lo) / (hi - lo))),
                Offset(size.width * i / (temps.size - 1), size.height * (1 - (temps[i] - lo) / (hi - lo))), 4f)
        }
    }
    Text("Wi-Fi: $wifi", fontSize = 12.sp)
    HorizontalDivider()
    val mins = if (sess == 0L) 0L else (now - sess) / 60000
    Text(if (sess == 0L) "Sessão parada" else "Sessão: $mins min" + (if (mins >= 60) " · ⏰ Faça uma pausa" else ""))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button({ sess = System.currentTimeMillis(); pr.edit().putLong("ini", sess).apply() }) { Text("Iniciar sessão") }
        OutlinedButton({ sess = 0L; pr.edit().putLong("ini", 0L).apply() }) { Text("Parar") }
    }
}

@Composable fun Ajuda(ctx: Context) {
    val pr = ctx.getSharedPreferences("sensi", 0)
    var code by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    Text("Como usar o NovaX", fontWeight = FontWeight.Bold)
    Text("1) Ligue o Shizuku. Depois de reiniciar o celular, ele precisa ser ligado de novo: Opções do desenvolvedor, Depuração sem fio, e no Shizuku toque em iniciar.\n2) Volte ao NovaX e conceda a permissão.\n3) Use Otimizar e jogar antes da partida, ou ligue o Modo Jogo automático na aba Mais.\n4) Se algo ficar estranho, aba Painel, Desfazer tudo.\n\nCada ajuste tem o botão Como funciona. Os resultados variam por aparelho, e a aba Teste mostra o antes e depois.", fontSize = 12.sp)
    Button({ ctx.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")?.let { ctx.startActivity(it) } }) { Text("Abrir o Shizuku") }
    HorizontalDivider()
    Text("Perfis de sensi: exportar e importar", fontWeight = FontWeight.Bold)
    OutlinedTextField(code, { code = it }, label = { Text("Código") })
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button({ code = Base64.encodeToString(pr.all.entries.joinToString("\n") { "${it.key}=${it.value.toString().replace("\n", "|")}" }.toByteArray(), Base64.NO_WRAP); msg = "Código gerado." }) { Text("Exportar") }
        OutlinedButton({
            msg = try {
                val e = pr.edit()
                String(Base64.decode(code.trim(), Base64.DEFAULT)).lines().filter { "=" in it }.forEach { e.putString(it.substringBefore("="), it.substringAfter("=").replace("|", "\n")) }
                e.apply(); "Perfis importados."
            } catch (x: Throwable) { "Código inválido." }
        }) { Text("Importar") }
    }
    Text(msg, fontSize = 12.sp)
}

class GameService : Service() {
    private var job: Job? = null
    override fun onBind(i: Intent?): IBinder? = null
    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("nx", "NovaX", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "nx").setContentTitle("NovaX: Modo Jogo automático").setContentText("Vigiando o Free Fire").setSmallIcon(android.R.drawable.ic_media_play).build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) else startForeground(1, n)
        if (job == null) job = CoroutineScope(Dispatchers.IO).launch {
            var ativo = false
            while (isActive) {
                val o = sh("dumpsys window | grep mCurrentFocus", FF)
                val g = if (MAX in o) MAX else if (FF in o) FF else null
                if (g != null && !ativo) { sh(autoOn, g); ativo = true }
                if (g == null && ativo) { sh(autoOff, FF); sh(autoOff, MAX); ativo = false }
                delay(4000)
            }
        }
        return START_STICKY
    }
    override fun onDestroy() { job?.cancel(); super.onDestroy() }
}

class NxTile : TileService() {
    override fun onClick() {
        val st = qsTile ?: return
        val ligar = st.state != Tile.STATE_ACTIVE
        st.state = if (ligar) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE; st.updateTile()
        Thread { sh(if (ligar) autoOn else autoOff, FF) }.start()
    }
}
