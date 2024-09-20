package pe.edu.upeu.asistenciaupeujcn.ui.presentation.screens.qrscreen

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import pe.edu.upeu.asistenciaupeujcn.utils.adjustPoint
import pe.edu.upeu.asistenciaupeujcn.utils.adjustSize
import pe.edu.upeu.asistenciaupeujcn.utils.drawBounds
import java.util.concurrent.Executors

@Composable
fun BarcodeScanningScreen(
    navController: NavController,
    viewModel: QRViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // Verifica si los permisos de cámara y ubicación están concedidos
    CheckPermissions(context = context) {
        // Si los permisos están concedidos, inicia el escaneo
        ScanSurface(navController = navController, viewModel = viewModel)
    }
}

@Composable
fun CheckPermissions(context: Context, onGranted: @Composable () -> Unit) {
    val cameraPermission = Manifest.permission.CAMERA
    val locationPermission = Manifest.permission.ACCESS_FINE_LOCATION

    // Estado para rastrear si los permisos están concedidos
    var permissionsGranted by remember { mutableStateOf(false) }

    // Utilizamos rememberLauncherForActivityResult para pedir los permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[cameraPermission] ?: false
        val locationGranted = permissions[locationPermission] ?: false

        if (cameraGranted && locationGranted) {
            permissionsGranted = true // Si los permisos están concedidos, actualiza el estado
        } else {
            // Muestra un mensaje al usuario indicando que los permisos son necesarios
            Toast.makeText(context, "Los permisos de cámara y ubicación son necesarios", Toast.LENGTH_LONG).show()
        }
    }

    // Solicita los permisos si aún no están concedidos
    LaunchedEffect(true) {
        permissionLauncher.launch(arrayOf(cameraPermission, locationPermission))
    }

    // Si los permisos fueron concedidos, ejecuta el bloque composable `onGranted`
    if (permissionsGranted) {
        onGranted()
    }
}


@Composable
fun ScanSurface(navController: NavController, viewModel: QRViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val detectedBarcodes by viewModel.detectedBarcodes.observeAsState(emptyList())
    val imageWidth by viewModel.imageWidth.observeAsState(0)
    val imageHeight by viewModel.imageHeight.observeAsState(0)
    val insertStatus by viewModel.insertStatus.observeAsState(null)

    val screenWidth = remember { mutableStateOf(context.resources.displayMetrics.widthPixels) }
    val screenHeight = remember { mutableStateOf(context.resources.displayMetrics.heightPixels) }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraView(
            context = context,
            lifecycleOwner = lifecycleOwner,
            analyzer = BarcodeScanningAnalyzer { barcodes, width, height ->
                viewModel.updateBarcodes(barcodes, width, height)
            }
        )
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(10.dp)
                .fillMaxHeight()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = "back",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Barcode scanning",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            }
            // Mostrar mensaje de estado basado en la inserción
            if (insertStatus != null) {
                val message = if (insertStatus == true) {
                    "Código registrado correctamente"
                } else {
                    "Error al registrar el código"
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (insertStatus == true) Color.Green else Color.Red,
                    modifier = Modifier.padding(10.dp)
                )

                // Si el registro es exitoso, navegar a la lista de asistencias
                LaunchedEffect(insertStatus) {
                    if (insertStatus == true) {
                        navController.navigate("listaAsistencia")
                    }
                }
            }
            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(vertical = 50.dp),
            ) {
                Text(
                    text = detectedBarcodes.joinToString(separator = "\n") { it.displayValue ?: "Código no válido" },
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
        }
        DrawBarcode(
            barcodes = detectedBarcodes,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            screenWidth = screenWidth.value,
            screenHeight = screenHeight.value
        )
    }
}

@Composable
fun DrawBarcode(
    barcodes: List<Barcode>,
    imageWidth: Int,
    imageHeight: Int,
    screenWidth: Int,
    screenHeight: Int
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        barcodes.forEach { barcode ->
            barcode.boundingBox?.toComposeRect()?.let {
                val topLeft =
                    adjustPoint(PointF(it.topLeft.x, it.topLeft.y), imageWidth,
                        imageHeight, screenWidth, screenHeight)
                val size = adjustSize(it.size, imageWidth,
                    imageHeight, screenWidth, screenHeight)
                drawBounds(topLeft, size, Color.Yellow, 10f)
            }
        }
    }
}

@Composable
fun CameraView(
    context: Context,
    analyzer: ImageAnalysis.Analyzer,
    lifecycleOwner: LifecycleOwner
) {
    val cameraProviderFuture = remember {
        ProcessCameraProvider.getInstance(context)
    }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .apply {
                        setAnalyzer(cameraExecutor, analyzer)
                    }
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageAnalysis,
                    preview
                )
            }, ContextCompat.getMainExecutor(context))
            previewView
        }
    )
}

@SuppressLint("UnsafeOptInUsageError")
class BarcodeScanningAnalyzer(
    private val onBarcodeDetected: (barcodes: List<Barcode>, width: Int, height: Int) -> Unit
) : ImageAnalysis.Analyzer {
    private val options = BarcodeScannerOptions.Builder().build()
    private val scanner = BarcodeScanning.getClient(options)
    override fun analyze(imageProxy: ImageProxy) {
        try {
            imageProxy.image?.let {
                val imageValue = InputImage.fromMediaImage(it, imageProxy.imageInfo.rotationDegrees)
                scanner.process(imageValue)
                    .addOnSuccessListener { barcodes ->
                        onBarcodeDetected(barcodes, imageValue.height, imageValue.width)
                    }
                    .addOnFailureListener { failure ->
                        failure.printStackTrace() // Log the error
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } ?: imageProxy.close()
        } catch (e: Exception) {
            e.printStackTrace()
            imageProxy.close()
        }
    }
}
