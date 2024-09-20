package pe.edu.upeu.asistenciaupeujcn.ui.presentation.screens.asistencia

import androidx.compose.foundation.layout.* // Importa las clases de layout
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // Importa la función items para LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.* // Importa Material3 para los componentes UI
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pe.edu.upeu.asistenciaupeujcn.ui.presentation.screens.qrscreen.QRViewModel

@Composable
fun ListaAsistencias(viewModel: QRViewModel = hiltViewModel()) {
    // Observa las asistencias registradas desde el ViewModel
    val asistencias by viewModel.asistenciasRegistradas.observeAsState(emptyList())

    // Verifica si hay asistencias registradas
    if (asistencias.isEmpty()) {
        // Si no hay asistencias, muestra un mensaje vacío
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No hay asistencias registradas", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        // Si hay asistencias, las muestra en forma de tarjetas
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp) // Añade padding para que no se peguen los elementos a los bordes
        ) {
            // Recorre cada asistencia y crea una tarjeta para mostrar sus datos
            items(asistencias) { asistencia ->
                Card(
                    shape = RoundedCornerShape(8.dp), // Redondea los bordes de la tarjeta
                    modifier = Modifier
                        .padding(8.dp) // Espacio entre las tarjetas
                        .fillMaxWidth(), // Hace que la tarjeta ocupe todo el ancho disponible
                    elevation = CardDefaults.cardElevation(4.dp) // Añade sombra a las tarjetas
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "CUI: ${asistencia.cui}") // Muestra el CUI de la asistencia
                        Text(text = "Fecha: ${asistencia.fecha}") // Muestra la fecha
                        Text(text = "Hora: ${asistencia.horaReg}") // Muestra la hora de registro
                        Text(text = "Tipo: ${asistencia.tipo}") // Muestra el tipo de asistencia
                    }
                }
            }
        }
    }
}
