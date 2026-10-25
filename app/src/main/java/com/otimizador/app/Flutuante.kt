package com.otimizador.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable fun Flutuante(ctx: Context) {
    var on by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF16161D)), border = BorderStroke(1.dp, Color(0xFFFF2A2A))) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Painel flutuante", fontWeight = FontWeight.Bold, color = Color(0xFFFF5A5A))
                Text("Janela sobre o jogo com Resolução, Frame Rate e Gráfico. Dá para arrastar e minimizar. Na primeira vez o Android pede \"Exibir sobre outros apps\": ative, volte e ligue de novo.", fontSize = 12.sp)
            }
            Switch(on, { v ->
                if (v && !Settings.canDrawOverlays(ctx)) ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")))
                else { on = v; val i = Intent(ctx, FloatService::class.java); if (v) ctx.startForegroundService(i) else ctx.stopService(i) }
            })
        }
    }
}

@Composable fun Resol(busy: Boolean, go: (String, String) -> Unit) {
    var w by remember { mutableStateOf("") }
    var h by remember { mutableStateOf("") }
    val ok = (w.toIntOrNull() ?: 0) in 300..4000 && (h.toIntOrNull() ?: 0) in 300..4000
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF16161D)), border = BorderStroke(1.dp, Color(0xFF4A1414))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Resolução personalizada (X × Y)", fontWeight = FontWeight.Bold, color = Color(0xFFFF5A5A))
            Text("Digite a largura (X) e a altura (Y) em pixels, de 300 a 4000. Menor que a do celular dá mais FPS. Maior deixa mais pesado e pode esticar a imagem. Use Testar 15 s antes de aplicar de vez.", fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(w, { w = it.filter { c -> c.isDigit() } }, Modifier.weight(1f), label = { Text("X (largura)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(h, { h = it.filter { c -> c.isDigit() } }, Modifier.weight(1f), label = { Text("Y (altura)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({ go("Teste ${w}x$h por 15 s", "wm size ${w}x$h; sleep 15; wm size reset") }, enabled = !busy && ok) { Text("Testar 15 s") }
                Button({ go("Resolução ${w}x$h", "wm size ${w}x$h") }, enabled = !busy && ok) { Text("Aplicar") }
            }
            OutlinedButton({ go("Resolução nativa", "wm size reset") }, enabled = !busy) { Text("Voltar ao nativo") }
        }
    }
}

@Composable fun SensiXY(ctx: Context, busy: Boolean, go: (String, String) -> Unit) {
    val pr = ctx.getSharedPreferences("nx", 0)
    var x by remember { mutableStateOf(pr.getString("sx", "") ?: "") }
    var y by remember { mutableStateOf(pr.getString("sy", "") ?: "") }
    var p by remember { mutableStateOf(0f) }
    var msg by remember { mutableStateOf(if (x.isEmpty()) "Nenhum valor salvo." else "Salvo: X $x · Y $y") }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF16161D)), border = BorderStroke(1.dp, Color(0xFF4A1414))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Sensi X e Y (você escolhe)", fontWeight = FontWeight.Bold, color = Color(0xFFFF5A5A))
            Text("Escreva a sua sensi horizontal (X) e vertical (Y). O Android não tem X e Y separados no sistema, e a sensi do Free Fire só muda dentro do jogo. Aqui você guarda os seus valores para consulta. O que o app aplica de verdade é a velocidade do ponteiro, no controle abaixo.", fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(x, { x = it.filter { c -> c.isDigit() }.take(3) }, Modifier.weight(1f), label = { Text("X") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(y, { y = it.filter { c -> c.isDigit() }.take(3) }, Modifier.weight(1f), label = { Text("Y") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            OutlinedButton({ pr.edit().putString("sx", x).putString("sy", y).apply(); msg = "Salvo: X $x · Y $y" }) { Text("Salvar X e Y") }
            Text(msg, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("Velocidade do ponteiro: ${p.toInt()}", fontSize = 12.sp)
            Slider(p, { p = it }, valueRange = -7f..7f, steps = 13)
            Button({ go("Ponteiro ${p.toInt()}", "settings put system pointer_speed ${p.toInt()}") }, enabled = !busy) { Text("Aplicar ponteiro") }
        }
    }
}
