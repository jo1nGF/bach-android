package com.example.bachmidi

import android.content.Context
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.media.midi.MidiOutputPort
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.random.Random

private val Panel = Color(0xFF111112)
private val Gold = Color(0xFFD7B66C)
private val Cream = Color(0xFFF3E6C8)
private val Blue = Color(0xFF2979FF)
private val Muted = Color(0xFF9A9386)

class MainActivity : ComponentActivity() {
    private lateinit var midiManager: MidiManager
    private var midiDevice: MidiDevice? = null
    private var output: MidiOutputPort? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        midiManager = getSystemService(Context.MIDI_SERVICE) as MidiManager
        setContent {
            BachApp(
                midiManager = midiManager,
                onConnect = { info -> connect(info) },
                onNote = { note, velocity -> sendNote(note, velocity) }
            )
        }
    }

    private fun connect(info: MidiDeviceInfo) {
        output?.close()
        midiDevice?.close()
        midiManager.openDevice(info, { device ->
            midiDevice = device
            output = device?.openOutputPort(0)
        }, Handler(Looper.getMainLooper()))
    }

    private fun sendNote(note: Int, velocity: Int) {
        val port = output ?: return
        val n = note.coerceIn(0, 127)
        val v = velocity.coerceIn(1, 127)
        port.send(byteArrayOf(0x90.toByte(), n.toByte(), v.toByte()))
        Handler(Looper.getMainLooper()).postDelayed({
            try { port.send(byteArrayOf(0x80.toByte(), n.toByte(), 0)) } catch (_: Exception) {}
        }, 130)
    }

    override fun onDestroy() {
        output?.close()
        midiDevice?.close()
        super.onDestroy()
    }
}

@Composable
private fun BachApp(
    midiManager: MidiManager,
    onConnect: (MidiDeviceInfo) -> Unit,
    onNote: (Int, Int) -> Unit
) {
    var bpm by remember { mutableFloatStateOf(120f) }
    var playing by remember { mutableStateOf(false) }
    var breakMode by remember { mutableStateOf(false) }
    var engine by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<MidiDeviceInfo?>(null) }
    var devices by remember { mutableStateOf(emptyList<MidiDeviceInfo>()) }
    val values = remember { mutableStateMapOf(
        "MATERIAL" to 0.55f, "MEMORY" to 0.48f, "GROWTH" to 0.62f, "MORPH" to 0.35f,
        "DENSITY" to 0.68f, "TENSION" to 0.42f, "INTERACTION" to 0.58f, "HUMANIZE" to 0.16f
    ) }

    LaunchedEffect(Unit) {
        devices = midiManager.devices.filter { it.outputPortCount > 0 }
    }
    LaunchedEffect(playing, bpm, engine, breakMode, selected) {
        if (!playing) return@LaunchedEffect
        val proba = ProbaEngine()
        val cellular = CellularEngine()
        while (playing) {
            val event = if (engine == 0) proba.generate(values["DENSITY"] ?: .5f, values["MATERIAL"] ?: .5f)
                       else cellular.generate(values["DENSITY"] ?: .5f)
            if (event.active && !breakMode) {
                val base = 60 + ((values["MORPH"] ?: .5f) * 12).roundToInt()
                onNote(base + event.degree, (event.velocity * 127).roundToInt())
            }
            delay((60000f / bpm / 4f).toLong().coerceAtLeast(10))
        }
    }

    Column(
        Modifier.fillMaxSize().background(Panel).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("B.A.C.H.", color = Cream, fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            listOf("MATERIAL", "MEMORY", "GROWTH", "MORPH", "DENSITY", "TENSION", "INTERACT", "HUMANIZE").forEach { name ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Box(Modifier.size(24.dp).border(2.dp, Gold, CircleShape).padding(4.dp).background(Color(0xFF29251E), CircleShape))
                    Text(name, color = Cream, fontSize = 6.sp, textAlign = TextAlign.Center, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            LedButton("RESET", Color(0xFFF5F0E5), false) { playing = false; breakMode = false; bpm = 120f }
            LedButton("PLAY/STOP", Color(0xFFFF6F7D), playing) { playing = !playing }
            LedButton("BREAK", Color(0xFFFF2364), breakMode) { breakMode = !breakMode }
            listOf(Color.Red, Color(0xFFFFC928), Color(0xFF4DEAFF), Color(0xFF287DFF)).forEach { Led(it, 18) }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1.05f), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                listOf("MATERIAL", "MEMORY", "GROWTH", "MORPH").forEach { name ->
                    KnobControl(name, values[name] ?: .5f, { values[name] = it })
                }
            }
            Column(Modifier.weight(1.05f), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                listOf("DENSITY", "TENSION", "INTERACTION", "HUMANIZE").forEach { name ->
                    KnobControl(name, values[name] ?: .5f, { values[name] = it })
                }
            }
            Column(Modifier.weight(1.25f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(1f).clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Color(0xFF356DFF), Color(0xFF111E78), Color(0xFF080D30))))
                        .border(4.dp, Gold.copy(alpha = .75f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${bpm.roundToInt()}", color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Black)
                        Text(if (engine == 0) "PROBA" else "CELLULAR", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("CH 1   CH 2", color = Color.White, fontSize = 8.sp)
                        Text("CH 3   CH 4", color = Color.White, fontSize = 8.sp)
                        Text(if (playing) "● RUNNING" else "○ STOPPED", color = Color(0xFFB7D5FF), fontSize = 8.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("BPM", color = Cream, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Slider(value = bpm, onValueChange = { bpm = it }, valueRange = 40f..240f,
                    colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold, inactiveTrackColor = Color.DarkGray))
                Text("${bpm.roundToInt()} BPM", color = Cream, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            LedButton("PRESETS", Color.Red, false) { engine = (engine + 1) % 2 }
            Spacer(Modifier.width(14.dp))
            LedButton("PREV", Cream, false) { engine = 0 }
            Spacer(Modifier.width(14.dp))
            LedButton("NEXT", Cream, false) { engine = 1 }
        }
        Spacer(Modifier.height(14.dp))
        Text("MIDI OUTPUT", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        if (devices.isEmpty()) {
            Text("Nenhuma saída MIDI detectada neste momento.", color = Muted, fontSize = 10.sp, textAlign = TextAlign.Center)
        } else {
            devices.forEach { device ->
                val name = device.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: "MIDI Device"
                Row(Modifier.fillMaxWidth().clickable { selected = device; onConnect(device) }.padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (selected == device) "●" else "○", color = if (selected == device) Color(0xFF5BDAFF) else Muted)
                    Spacer(Modifier.width(8.dp))
                    Text(name, color = Cream, fontSize = 11.sp)
                }
            }
        }
        listOf("GATE", "MOD", "PITCH").forEach { label ->
            Row(Modifier.fillMaxWidth().padding(top = 5.dp).border(1.dp, Cream.copy(alpha = .65f), RoundedCornerShape(18.dp)).padding(7.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = Cream, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(54.dp))
                repeat(4) {
                    Box(Modifier.weight(1f).padding(horizontal = 3.dp).height(30.dp).border(2.dp, Gold, CircleShape).background(Color(0xFF252321), CircleShape))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("KAONA • B.A.C.H. INSPIRED MIDI GENERATOR", color = Muted, fontSize = 8.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun KnobControl(label: String, value: Float, onChange: (Float) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.size(58.dp).clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0xFF414141), Color(0xFF0B0B0B), Color.Black)))
            .border(2.dp, Color(0xFF292722), CircleShape).clickable { onChange(if (value > .5f) .25f else .75f) },
            contentAlignment = Alignment.TopCenter) {
            Box(Modifier.padding(top = 7.dp).width(3.dp).height(13.dp).background(Gold, RoundedCornerShape(2.dp)))
        }
        Text(label, color = Cream, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Slider(value = value, onValueChange = onChange, modifier = Modifier.height(22.dp),
            colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold, inactiveTrackColor = Color(0xFF3C3831)))
    }
}

@Composable
private fun Led(color: Color, size: Int) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(color.copy(alpha = .22f)).border(2.dp, color, CircleShape).padding(4.dp).background(color, CircleShape))
}

@Composable
private fun LedButton(label: String, color: Color, active: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 2.dp)) {
        Box(Modifier.size(25.dp).clip(CircleShape)
            .background(if (active) color else color.copy(alpha = .35f))
            .border(2.dp, color.copy(alpha = .9f), CircleShape))
        Text(label, color = Cream, fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

private data class NoteEvent(val active: Boolean, val degree: Int, val velocity: Float)

private class ProbaEngine {
    private var state = 0
    private val random = Random.Default
    fun generate(density: Float, material: Float): NoteEvent {
        if (random.nextFloat() > density.coerceIn(0f, 1f)) return NoteEvent(false, 0, 0f)
        val weights = FloatArray(12)
        var sum = 0f
        for (j in 0 until 12) {
            val d0 = abs(state - j)
            val d = minOf(d0, 12 - d0)
            val w = exp((-d * (1.5f - material.coerceIn(0f, 1f))).toDouble()).toFloat()
            weights[j] = w
            sum += w
        }
        val r = random.nextFloat() * sum
        var cumulative = 0f
        var next = state
        for (j in 0 until 12) {
            cumulative += weights[j]
            if (r <= cumulative) { next = j; break }
        }
        state = next
        return NoteEvent(true, next, .5f + random.nextFloat() * .5f)
    }
}

private class CellularEngine {
    private val width = 16
    private var cells = BooleanArray(width) { Random.nextBoolean() }
    fun generate(density: Float): NoteEvent {
        val next = BooleanArray(width)
        for (i in 0 until width) {
            val left = cells[(i - 1 + width) % width]
            val center = cells[i]
            val right = cells[(i + 1) % width]
            val index = (if (left) 4 else 0) + (if (center) 2 else 0) + (if (right) 1 else 0)
            next[i] = (30 shr index) and 1 == 1
        }
        cells = next
        val active = cells.count { it }
        val first = cells.indexOfFirst { it }
        return if (active > 3 && first >= 0 && Random.nextFloat() <= density)
            NoteEvent(true, first - width / 2, .8f) else NoteEvent(false, 0, 0f)
    }
}
