package pe.edu.upeu.asistenciaupeujcn.ui.presentation.screens.qrscreen

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.mlkit.vision.barcode.common.Barcode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pe.edu.upeu.asistenciaupeujcn.modelo.Asistenciax
import pe.edu.upeu.asistenciaupeujcn.repository.AsistenciaxRepository
import pe.edu.upeu.asistenciaupeujcn.utils.TokenUtils
import javax.inject.Inject

@HiltViewModel
class QRViewModel @Inject constructor(
    private val asisRepo: AsistenciaxRepository
) : ViewModel() {

    // LiveData para manejar la detección de códigos de barras y las dimensiones de la imagen
    private val _detectedBarcodes = MutableLiveData<List<Barcode>>()
    val detectedBarcodes: LiveData<List<Barcode>> = _detectedBarcodes
    private val _imageWidth = MutableLiveData<Int>()
    val imageWidth: LiveData<Int> = _imageWidth
    private val _imageHeight = MutableLiveData<Int>()
    val imageHeight: LiveData<Int> = _imageHeight

    // Variables para manejar el procesamiento de códigos de barras
    private var isProcessing = false
    private var lastProcessedBarcode: String? = null

    // LiveData para el estado de inserción de la asistencia
    private val _insertStatus = MutableLiveData<Boolean>()
    val insertStatus: LiveData<Boolean> = _insertStatus

    // LiveData para almacenar las asistencias registradas
    private val _asistenciasRegistradas = MutableLiveData<List<Asistenciax>>(emptyList())
    val asistenciasRegistradas: LiveData<List<Asistenciax>> = _asistenciasRegistradas

    // Modelo de asistencia que se utilizará para registrar la asistencia escaneada
    var asisTO = Asistenciax.createEmpty()

    // Actualiza la lista de códigos de barras detectados y procesa el escaneo
    fun updateBarcodes(barcodes: List<Barcode>, width: Int, height: Int) {
        if (isProcessing) return
        _detectedBarcodes.value = barcodes
        _imageWidth.value = width
        _imageHeight.value = height

        // Obtiene la ubicación antes de registrar la asistencia
        posicionObtencion()

        // Procesa el código QR detectado
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (barcodes.size == 1) {
                    val dato = barcodes[0].displayValue
                    if (dato != null && dato != lastProcessedBarcode) {
                        isProcessing = true

                        // Rellenar los datos de asistencia
                        asisTO.fecha = "2024-09-14"
                        asisTO.horaReg = "14:30"
                        asisTO.tipo = "inspección"
                        asisTO.calificacion = 5
                        asisTO.cui = dato
                        asisTO.tipoCui = "C.Universitario"
                        asisTO.actividadId = TokenUtils.ID_ASIS_ACT
                        asisTO.subactasisId = 0
                        asisTO.offlinex = "NO"
                        asisTO.entsal = "E"

                        // Si el código QR es válido (por longitud), se registra la asistencia
                        if (dato.length == 8 || dato.length == 9) {
                            val success = asisRepo.insertarAsistenciax(asisTO)
                            if (success) {
                                // Actualiza la lista de asistencias registradas
                                _asistenciasRegistradas.postValue(_asistenciasRegistradas.value?.plus(asisTO))
                                lastProcessedBarcode = dato
                                _insertStatus.postValue(true)
                                resetLastProcessedBarcode()
                            } else {
                                _insertStatus.postValue(false)
                            }
                        } else {
                            _insertStatus.postValue(false)
                        }

                        // Finaliza el procesamiento
                        isProcessing = false
                        _detectedBarcodes.postValue(emptyList())
                    }
                }
            } catch (e: Exception) {
                Log.i("ERRRRR", "${e.message}")
            }
        }
    }

    // Reinicia el último código procesado después de 5 segundos para permitir procesar el mismo código más tarde
    private fun resetLastProcessedBarcode() {
        viewModelScope.launch {
            delay(5000)
            lastProcessedBarcode = null
        }
    }

    // Función para obtener la ubicación actual del dispositivo
    @SuppressLint("MissingPermission")
    fun posicionObtencion() {
        val context: Context = TokenUtils.CONTEXTO_APPX
        var locationCallback: LocationCallback? = null
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                if (p0.locations.isNotEmpty()) {
                    val location = p0.locations.last()
                    asisTO.latituda = location.latitude.toString()
                    asisTO.longituda = location.longitude.toString()

                    // Elimina las actualizaciones de ubicación para ahorrar recursos
                    fusedLocationClient.removeLocationUpdates(this)
                }
            }
        }

        // Inicia las actualizaciones de ubicación
        viewModelScope.launch {
            val locationRequest = LocationRequest.create().apply {
                interval = 10000
                fastestInterval = 5000
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }
}
