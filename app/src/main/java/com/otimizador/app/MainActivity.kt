package com.otimizador.app

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import rikka.shizuku.Shizuku

/** Executa um comando shell com privilégio ADB via Shizuku. */
fun sh(cmd: String): String = try {
    val m = Shizuku::class.java.getDeclaredMethod(
        "newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
    m.isAccessible = true
    val p = m.invoke(null, arrayOf("sh", "-c", "$cmd 2>&1"), null, null) as Process
    val out = p.inputStream.bufferedReader().readText()
    p.waitFor()
    out.trim().ifEmpty { "Concluído." }
} catch (e: Throwable) { "Erro: ${e.message}" }

fun running() = try { Shizuku.pingBinder() } catch (e: Throwable) { false }
fun granted() = try {
    running() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
} catch (e: Throwable) { false }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("crash", MODE_PRIVATE)
        val last = prefs.getString("log", null)
        if (last != null) {
            prefs.edit().remove("log").commit()
            val tv = TextView(this).apply { text = "ERRO DA ÚLTIMA ABERTURA:\n\n$last"; setTextIsSelectable(true); setPadding(32, 96, 32, 32) }
            setContentView(ScrollView(this).apply { addView(tv) })
            return
        }
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            prefs.edit().putString("log", e.stackTraceToString().take(3000)).commit()
            android.os.Process.killProcess(android.os.Process.myPid())
        }
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { Surface { App() } } }
    }
}

@Composable
fun App() {
    val scope = rememberCoroutineScope()
    var log by remember { mutableStateOf("Toque em um botão.") }
    var busy by remember { mutableStateOf(false) }
    var pkg by remember { mutableStateOf("") }
    var ok by remember { mutableStateOf(granted()) }

    fun run(title: String, cmd: String) {
        if (!granted()) { log = "Shizuku não está ativo ou sem permissão."; return }
        scope.launch {
            busy = true; log = "$title...\n(pode demorar)"
            log = "$title\n\n" + withContext(Dispatchers.IO) { sh(cmd) }
            busy = false
        }
    }
    @Composable fun B(t: String, cmd: String) =
        Button({ run(t, cmd) }, Modifier.fillMaxWidth(), enabled = !busy) { Text(t) }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Otimizador (Shizuku)", style = MaterialTheme.typography.headlineSmall)
        Text(if (ok) "Shizuku: permissão concedida ✔" else if (running()) "Shizuku ativo, falta permissão" else "Shizuku não está rodando")
        if (!ok) Button({
            if (running()) { Shizuku.addRequestPermissionResultListener { _, _ -> ok = granted() }; Shizuku.requestPermission(1) }
            ok = granted()
        }, Modifier.fillMaxWidth()) { Text("Conceder permissão / atualizar") }

        HorizontalDivider()
        B("Painel de status", "grep -E 'MemTotal|MemAvailable' /proc/meminfo; df -h /data | tail -1; " +
            "dumpsys battery | grep -E 'level|temperature|status'; top -b -n 1 -m 8 | tail -9")
        B("Compilar todos os apps (speed-profile)", "cmd package compile -m speed-profile -a")
        B("Limpar cache de todos os apps", "pm trim-caches 999G")
        B("Encerrar apps em segundo plano", "am kill-all")
        B("Standby: apps de terceiros → rare",
            "for p in \$(pm list packages -3 | cut -d: -f2); do am set-standby-bucket \$p rare; done; echo feito")
        B("Standby: restaurar (active)",
            "for p in \$(pm list packages -3 | cut -d: -f2); do am set-standby-bucket \$p active; done; echo feito")

        HorizontalDivider()
        Text("Animações", style = MaterialTheme.typography.titleMedium)
        fun anim(v: String) = "settings put global window_animation_scale $v; " +
            "settings put global transition_animation_scale $v; settings put global animator_duration_scale $v; echo escala $v"
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ run("Animações 0.5x", anim("0.5")) }, enabled = !busy) { Text("0.5x") }
            OutlinedButton({ run("Animações off", anim("0")) }, enabled = !busy) { Text("Off") }
            OutlinedButton({ run("Animações padrão", anim("1")) }, enabled = !busy) { Text("1x") }
        }

        HorizontalDivider()
        Text("Bloatware (reversível)", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(pkg, { pkg = it.trim() }, Modifier.fillMaxWidth(), label = { Text("Pacote, ex: com.facebook.appmanager") })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({ run("Desativar $pkg", "pm uninstall -k --user 0 $pkg") }, enabled = !busy && pkg.isNotEmpty()) { Text("Desativar") }
            OutlinedButton({ run("Restaurar $pkg", "cmd package install-existing $pkg") }, enabled = !busy && pkg.isNotEmpty()) { Text("Restaurar") }
        }
        Button({ run("Pacotes instalados", "pm list packages --user 0 | cut -d: -f2 | sort") },
            Modifier.fillMaxWidth(), enabled = !busy) { Text("Listar pacotes") }

        HorizontalDivider()
        Text(log, style = MaterialTheme.typography.bodySmall)
    }
}
