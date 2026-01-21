package com.example.registroaccidentes

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// --- COLORES AMT ---
val AmtBlue = Color(0xFF003366)
val AmtRed = Color(0xFFCC0000)
val AmtWhite = Color(0xFFFFFFFF)

// --- MODELO DE DATOS ---
data class ReporteAccidente(
    val tipo: String,
    val fecha: String,
    val matricula: String,
    val conductor: String,
    val cedula: String,
    val observaciones: String,
    val ubicacion: String,
    val foto: Bitmap?
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = AmtBlue, onPrimary = AmtWhite,
                    secondary = AmtRed, onSecondary = AmtWhite,
                    background = Color(0xFFF5F5F5)
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavegacion()
                }
            }
        }
    }
}

// --- NAVEGACIÓN (CONTROLADOR DE PANTALLAS) ---
@Composable
fun AppNavegacion() {
    val listaReportes = remember { mutableStateListOf<ReporteAccidente>() }
    // INICIA EN SPLASH
    var pantallaActual by remember { mutableStateOf("splash") }

    when (pantallaActual) {
        "splash" -> {
            PantallaSplash(onTimeout = { pantallaActual = "lista" })
        }
        "lista" -> {
            PantallaListaReportes(
                reportes = listaReportes,
                onNuevoReporte = { pantallaActual = "formulario" }
            )
        }
        "formulario" -> {
            PantallaFormulario(
                onGuardar = { nuevoReporte ->
                    listaReportes.add(0, nuevoReporte)
                    pantallaActual = "lista"
                },
                onCancelar = { pantallaActual = "lista" }
            )
        }
    }
}

// --- PANTALLA 0: SPLASH (ANIMACIÓN) ---
@Composable
fun PantallaSplash(onTimeout: () -> Unit) {
    val context = LocalContext.current

    // Configuración para cargar GIFs
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(coil.decode.GifDecoder.Factory())
                }
            }
            .build()
    }

    // Temporizador de 3 segundos
    LaunchedEffect(true) {
        delay(3000)
        onTimeout()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(AmtBlue),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // TU GIF AQUÍ (Asegúrate que se llame logo_animado en res/drawable)
            AsyncImage(
                model = R.drawable.logo_animado,
                imageLoader = imageLoader,
                contentDescription = "Cargando",
                modifier = Modifier.size(200.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text("Agencia Metropolitana de Tránsito", color = AmtWhite, fontWeight = FontWeight.Bold)
            Text("Cargando sistema...", color = Color.LightGray, fontSize = 12.sp)
        }
    }
}

// --- PANTALLA 1: LISTA ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaListaReportes(reportes: List<ReporteAccidente>, onNuevoReporte: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("AMT - Historial", color = AmtWhite) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = AmtBlue)) },
        floatingActionButton = { FloatingActionButton(onClick = onNuevoReporte, containerColor = AmtRed, contentColor = AmtWhite) { Icon(Icons.Default.Add, "Nuevo") } }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            if (reportes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No hay reportes registrados.", color = Color.Gray) }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(reportes) { ItemReporte(it) } }
            }
        }
    }
}

@Composable
fun ItemReporte(reporte: ReporteAccidente) {
    Card(elevation = CardDefaults.cardElevation(4.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(reporte.tipo.uppercase(), fontWeight = FontWeight.Bold, color = AmtBlue)
                Text("Placa: ${reporte.matricula}", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Text("Fecha: ${reporte.fecha} - ${reporte.conductor}", style = MaterialTheme.typography.bodySmall)
            }
            if (reporte.foto != null) Image(bitmap = reporte.foto.asImageBitmap(), contentDescription = null, modifier = Modifier.size(60.dp).background(Color.LightGray))
        }
    }
}

// --- PANTALLA 2: FORMULARIO ---
@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaFormulario(onGuardar: (ReporteAccidente) -> Unit, onCancelar: () -> Unit) {
    val context = LocalContext.current
    val fechaActual = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }
    var fecha by remember { mutableStateOf(fechaActual) }
    var tipo by remember { mutableStateOf("Choque") }
    var expanded by remember { mutableStateOf(false) }
    var matricula by remember { mutableStateOf("") }
    var conductor by remember { mutableStateOf("") }
    var cedula by remember { mutableStateOf("") }
    var obs by remember { mutableStateOf("") }
    var ubicacion by remember { mutableStateOf("Coordenadas no capturadas") }
    var foto by remember { mutableStateOf<Bitmap?>(null) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Lanzadores
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) {
        if (it != null) foto = it
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        val gpsConcedido = permisos[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val camaraConcedida = permisos[Manifest.permission.CAMERA] ?: false

        if (gpsConcedido) {
            // Intentar obtener ubicación si el permiso se dio en ese instante
            Toast.makeText(context, "Permiso GPS concedido", Toast.LENGTH_SHORT).show()
        }
        if (camaraConcedida) {
            Toast.makeText(context, "Permiso Cámara concedido", Toast.LENGTH_SHORT).show()
        }
    }

    // Funciones de acción
    fun obtenerUbicacion() {
        val fineLoc = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        if (fineLoc == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                .addOnSuccessListener { loc ->
                    ubicacion = if (loc != null) "${loc.latitude}, ${loc.longitude}" else "Buscando señal GPS..."
                }
                .addOnFailureListener { ubicacion = "Error al conectar GPS" }
        } else {
            permLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Reporte", color = AmtWhite) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AmtBlue),
                navigationIcon = { IconButton(onClick = onCancelar) { Icon(Icons.Default.ArrowBack, "Volver", tint = AmtWhite) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Campos de texto
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(value = tipo, onValueChange = {}, readOnly = true, label = { Text("Tipo de Siniestro") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf("Choque", "Colisión", "Atropello", "Volcamiento").forEach {
                        DropdownMenuItem(text = { Text(it) }, onClick = { tipo = it; expanded = false })
                    }
                }
            }

            OutlinedTextField(value = fecha, onValueChange = { fecha = it }, label = { Text("Fecha") }, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(
                value = matricula,
                onValueChange = { if (it.length <= 7) matricula = it.uppercase() },
                label = { Text("Matrícula") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
            )

            OutlinedTextField(
                value = conductor,
                onValueChange = { if (it.all { c -> c.isLetter() || c.isWhitespace() }) conductor = it },
                label = { Text("Nombre del Conductor") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )

            OutlinedTextField(
                value = cedula,
                onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() }) cedula = it },
                label = { Text("Cédula de Identidad") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(value = obs, onValueChange = { obs = it }, label = { Text("Observaciones") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

            // --- FILA DE BOTONES GPS Y FOTO ---
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { obtenerUbicacion() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = AmtBlue)) {
                    Icon(Icons.Default.GpsFixed, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("GPS")
                }

                Button(onClick = {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraLauncher.launch()
                    } else {
                        permLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION))
                    }
                }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = AmtBlue)) {
                    Icon(Icons.Default.CameraAlt, null)
                    Spacer(Modifier.width(4.dp))
                    Text("FOTO")
                }
            }

            // Datos capturados (Ubicación y Foto)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEEEEEE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Ubicación: $ubicacion", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                    if (foto != null) {
                        Spacer(Modifier.height(8.dp))
                        Image(bitmap = foto!!.asImageBitmap(), contentDescription = null, modifier = Modifier.size(120.dp).background(Color.White))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botón GUARDAR
            Button(
                onClick = {
                    if (matricula.isNotEmpty() && conductor.isNotEmpty()) {
                        // Vibración corta (500ms es mejor que 5000ms)
                        val v = if (Build.VERSION.SDK_INT >= 31) (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator else context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                        if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)) else v.vibrate(500)

                        onGuardar(ReporteAccidente(tipo, fecha, matricula, conductor, cedula, obs, ubicacion, foto))
                        Toast.makeText(context, "Reporte Guardado exitosamente", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Por favor complete Matrícula y Conductor", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AmtRed),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("GUARDAR REPORTE", fontWeight = FontWeight.Bold)
            }
        }
    }
}
