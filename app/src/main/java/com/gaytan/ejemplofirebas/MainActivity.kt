package com.gaytan.ejemplofirebas

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import com.gaytan.ejemplofirebas.ui.theme.EjemploFirebasTheme

data class User(val name: String = "")

class MainActivity : ComponentActivity() {

    private lateinit var dbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar Firebase si no está inicializado
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }

        // Habilitar persistencia offline
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        } catch (e: Exception) {
            Log.w("Firebase", "setPersistenceEnabled failed", e)
        }

        // Inicializar la referencia a la base de datos
        dbRef = FirebaseDatabase.getInstance().getReference("usuarios")

        setContent {
            EjemploFirebasTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FirebaseScreen(dbRef = dbRef)
                }
            }
        }
    }
}

@Composable
fun FirebaseScreen(dbRef: DatabaseReference) {
    val context = LocalContext.current
    var nombre by remember { mutableStateOf("") }
    var usuarios by remember { mutableStateOf(listOf<User>()) }
    var isLoading by remember { mutableStateOf(false) }
    var isConnected by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Verificar conexión y cargar datos
    LaunchedEffect(Unit) {
        Log.d("FirebaseScreen", "Iniciando conexión a Firebase...")

        // Listener para verificar conexión
        val connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected")
        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                isConnected = connected
                Log.d("FirebaseScreen", "Estado de conexión: $connected")
                if (!connected) {
                    errorMessage = "Sin conexión a Firebase"
                } else {
                    errorMessage = null
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseScreen", "Error en listener de conexión: ${error.message}")
            }
        })

        // Listener para los datos de usuarios
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("FirebaseScreen", "Datos recibidos de Firebase")
                val listaUsuarios = mutableListOf<User>()

                for (userSnapshot in snapshot.children) {
                    try {
                        val user = userSnapshot.getValue(User::class.java)
                        user?.let {
                            listaUsuarios.add(it)
                            Log.d("FirebaseScreen", "Usuario agregado: ${it.name}")
                        }
                    } catch (e: Exception) {
                        Log.e("FirebaseScreen", "Error al parsear usuario: ${e.message}")
                    }
                }

                usuarios = listaUsuarios
                Log.d("FirebaseScreen", "Total usuarios cargados: ${usuarios.size}")

                // Limpiar mensaje de error si los datos se cargan correctamente
                if (errorMessage == "Sin conexión a Firebase") {
                    errorMessage = null
                }
            }

            override fun onCancelled(error: DatabaseError) {
                val mensajeError = "Error al leer datos: ${error.message}"
                errorMessage = mensajeError
                Toast.makeText(context, mensajeError, Toast.LENGTH_LONG).show()
                Log.e("FirebaseScreen", "Error de lectura de Firebase: ${error.message}")
            }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Firebase Realtime Database",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Indicador de estado de conexión
        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .padding(end = 8.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isConnected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.size(8.dp)
                ) {}
            }
            Text(
                text = if (isConnected) "Conectado" else "Desconectado",
                fontSize = 12.sp,
                color = if (isConnected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
        }

        // Mostrar mensaje de error si existe
        errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 14.sp
                )
            }
        }

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Ingresa tu nombre") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true,
            enabled = isConnected
        )

        Button(
            onClick = {
                if (nombre.trim().isNotEmpty()) {
                    isLoading = true
                    errorMessage = null

                    val id = dbRef.push().key
                    if (id != null) {
                        val newUser = User(nombre.trim())
                        Log.d("FirebaseScreen", "Guardando usuario: ${newUser.name}")

                        dbRef.child(id).setValue(newUser)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Usuario guardado exitosamente", Toast.LENGTH_SHORT).show()
                                nombre = ""
                                isLoading = false
                                Log.d("FirebaseScreen", "Usuario guardado correctamente")
                            }
                            .addOnFailureListener { error ->
                                val mensajeError = "Error al guardar: ${error.message}"
                                errorMessage = mensajeError
                                Toast.makeText(context, mensajeError, Toast.LENGTH_LONG).show()
                                Log.e("FirebaseScreen", "Error al guardar en Firebase: ${error.message}")
                                isLoading = false
                            }
                    } else {
                        Toast.makeText(context, "No se pudo generar un ID único", Toast.LENGTH_SHORT).show()
                        isLoading = false
                    }
                } else {
                    Toast.makeText(context, "Por favor, ingresa un nombre", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            enabled = !isLoading && isConnected && nombre.trim().isNotEmpty()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Guardar Usuario")
        }

        Text(
            text = "Usuarios registrados:",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            if (usuarios.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!isConnected) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Conectando a Firebase...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "No hay usuarios registrados",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(usuarios.withIndex().toList()) { (index, user) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}. ",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = user.name,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    EjemploFirebasTheme {
        val mockDbRef = FirebaseDatabase.getInstance().getReference("usuarios_preview_mock")
        FirebaseScreen(mockDbRef)
    }
}